param(
 [Parameter(Mandatory=$true)][int]$ParentPid,
 [Parameter(Mandatory=$true)][int]$EnginePid,
 [Parameter(Mandatory=$true)][string]$ServerIP,
 [ValidateRange(1,65535)][int]$Port = 1080
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$parsed = [System.Net.IPAddress]::Parse($ServerIP)
$outerV6 = $parsed.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetworkV6
$outerFamily = if ($outerV6) { 'IPv6' } else { 'IPv4' }
$outerDefault = if ($outerV6) { '::/0' } else { '0.0.0.0/0' }
$outerZero = if ($outerV6) { '::' } else { '0.0.0.0' }
$outerPrefix = if ($outerV6) { "$ServerIP/128" } else { "$ServerIP/32" }
function NamedPIPv6Ranges([System.Net.IPAddress]$Endpoint, [bool]$PreserveControl = $false) {
 # Iterative CIDR subtraction: never block our endpoint or essential link-local
 # neighbor discovery. No recursion-limit dependence in Windows PowerShell 5.
 $exclusions = @(@{ Bytes=$Endpoint.GetAddressBytes(); Length=128 })
 if ($PreserveControl) {
  $exclusions += @{ Bytes=([System.Net.IPAddress]::Parse('fe80::')).GetAddressBytes(); Length=10 }
  $exclusions += @{ Bytes=([System.Net.IPAddress]::Parse('ff02::')).GetAddressBytes(); Length=16 }
 }
 $stack = [System.Collections.Generic.Stack[object]]::new()
 $stack.Push(@{ Bytes=(New-Object byte[] 16); Length=0 })
 while ($stack.Count -gt 0) {
  $prefix=$stack.Pop(); $covered=$false; $split=$false
  foreach ($exclude in $exclusions) {
   $matches=$true
   for ($bit=0; $bit -lt [math]::Min($prefix.Length,$exclude.Length); $bit++) {
    $index=[int][math]::Floor($bit/8); $mask=1 -shl (7-($bit%8))
    if (($prefix.Bytes[$index] -band $mask) -ne ($exclude.Bytes[$index] -band $mask)) { $matches=$false; break }
   }
   if ($matches) {
    if ($exclude.Length -le $prefix.Length) { $covered=$true; break }
    $split=$true
   }
  }
  if ($covered) { continue }
  if (!$split) { ([System.Net.IPAddress]::new([byte[]]$prefix.Bytes)).ToString()+'/'+$prefix.Length; continue }
  [byte[]]$left=$prefix.Bytes.Clone(); [byte[]]$right=$prefix.Bytes.Clone()
  $index=[int][math]::Floor($prefix.Length/8); $mask=1 -shl (7-($prefix.Length%8))
  $right[$index]=$right[$index] -bor $mask
  $stack.Push(@{ Bytes=$right; Length=($prefix.Length+1) })
  $stack.Push(@{ Bytes=$left; Length=($prefix.Length+1) })
 }
}
$sessionName = 'namedP-' + [guid]::NewGuid().ToString('N').Substring(0,12)
$routes = @()
$rules = @()
$tun = $null
$adapter = $null
$routeReady = $false
$stage = 'initialization'
try {
 $exe = Join-Path $PSScriptRoot 'tun2socks.exe'
 if (!(Test-Path $exe) -or !(Test-Path (Join-Path $PSScriptRoot 'wintun.dll'))) { throw 'The release is incomplete: tun2socks.exe or wintun.dll is missing.' }
 $gateway = Get-NetRoute -AddressFamily $outerFamily -DestinationPrefix $outerDefault | Where-Object { $_.NextHop -ne $outerZero } | Sort-Object @{Expression={ $_.RouteMetric + (Get-NetIPInterface -InterfaceIndex $_.InterfaceIndex -AddressFamily $outerFamily).InterfaceMetric }} | Select-Object -First 1
 if (!$gateway) { throw "No physical $outerFamily default gateway is available." }
 $stage = 'physical endpoint route'
 # Preserve the server route BEFORE adding /1 tunnel routes. Never delete
 # routes that predate this session; retain exact CIM objects for cleanup.
 $existing = @(Get-NetRoute -DestinationPrefix $outerPrefix -ErrorAction SilentlyContinue)
 if ($existing.Count -eq 0) {
  $routes += New-NetRoute -DestinationPrefix $outerPrefix -InterfaceIndex $gateway.InterfaceIndex -NextHop $gateway.NextHop -RouteMetric 1 -PolicyStore ActiveStore
 } elseif (!($existing | Where-Object { $_.InterfaceIndex -eq $gateway.InterfaceIndex -and $_.NextHop -eq $gateway.NextHop })) { throw 'An existing server route conflicts with the physical gateway.' }
 $tun = Start-Process -FilePath $exe -ArgumentList @('-device',"tun://$sessionName",'-proxy',"socks5://127.0.0.1:$Port",'-loglevel','silent') -WindowStyle Hidden -PassThru
 $stage = 'Wintun adapter'
 $until = [DateTime]::UtcNow.AddSeconds(15)
 do { Start-Sleep -Milliseconds 150; $tun.Refresh(); if ($tun.HasExited) { throw 'TUN engine stopped before creating an adapter.' }; $adapter = Get-NetAdapter -Name $sessionName -ErrorAction SilentlyContinue } until ($adapter -or [DateTime]::UtcNow -gt $until)
 if (!$adapter) { throw 'Wintun adapter was not created.' }
 $stage = 'adapter address and DNS'
 $index = $adapter.ifIndex
 Set-NetIPInterface -InterfaceIndex $index -AddressFamily IPv4 -Dhcp Disabled -InterfaceMetric 1 -NlMtuBytes 1280
 New-NetIPAddress -InterfaceIndex $index -IPAddress '198.18.0.1' -PrefixLength 24 -AddressFamily IPv4 -PolicyStore ActiveStore | Out-Null
 Set-DnsClientServerAddress -InterfaceIndex $index -ServerAddresses @('1.1.1.1','1.0.0.1')
 # Preserve the one physical IPv6 tunnel endpoint when selected; block all
 # other IPv6 destinations, not the outer ICMPv6 connection itself.
 $stage = 'IPv6 firewall guard'
 # NetSecurity rejects ::/0 (confirmed against the real Windows provider).
 $blockedV6 = if ($outerV6) { @(NamedPIPv6Ranges $parsed $true) } else { @('::/1','8000::/1') }
 $rules += New-NetFirewallRule -Name "$sessionName-v6" -DisplayName "$sessionName IPv6 guard" -Direction Outbound -Action Block -RemoteAddress $blockedV6 -Profile Any
 if ($outerV6) {
  foreach ($protocol in @('TCP','UDP')) {
   $rules += New-NetFirewallRule -Name "$sessionName-v6-local-$protocol" -DisplayName "$sessionName IPv6 link-local guard" -Direction Outbound -Action Block -RemoteAddress @('fe80::/10','ff02::/16') -Protocol $protocol -Profile Any
  }
 }
 $stage = 'physical DNS guards'
 foreach ($physical in @(Get-NetAdapter | Where-Object { $_.ifIndex -ne $index -and $_.Status -eq 'Up' })) {
  foreach ($protocol in @('UDP','TCP')) {
   $rules += New-NetFirewallRule -Name "$sessionName-dns-$($physical.ifIndex)-$protocol" -DisplayName "$sessionName DNS guard" -Direction Outbound -Action Block -Protocol $protocol -RemotePort 53 -InterfaceAlias $physical.Name -Profile Any
  }
 }
 foreach ($prefix in @('0.0.0.0/1','128.0.0.0/1')) { $routes += New-NetRoute -InterfaceIndex $index -DestinationPrefix $prefix -NextHop '0.0.0.0' -RouteMetric 1 -PolicyStore ActiveStore }
 $stage = 'route verification'
 Clear-DnsClientCache
 $routeReady = $true
 [Console]::Out.WriteLine('{"event":"ready"}')
 # Pipe EOF and either process exit both trigger restoration. The guardian
 # outlives a crashed GUI, unlike a cleanup callback in the window itself.
 $closed = [Console]::In.ReadLineAsync()
 while (!$closed.IsCompleted) {
  if (!(Get-Process -Id $ParentPid -ErrorAction SilentlyContinue) -or !(Get-Process -Id $EnginePid -ErrorAction SilentlyContinue)) { break }
  $tun.Refresh(); if ($tun.HasExited) { throw 'TUN engine stopped.' }
  if (!(Get-NetAdapter | Where-Object { $_.ifIndex -eq $gateway.InterfaceIndex -and $_.Status -eq 'Up' })) { throw 'Physical network changed. Reconnect namedP.' }
  Start-Sleep -Milliseconds 500
 }
} catch { [Console]::Error.WriteLine("VPN setup [$stage]: $($_.Exception.Message)"); exit 1 }
finally {
 foreach ($route in $routes) { $route | Remove-NetRoute -Confirm:$false -ErrorAction SilentlyContinue }
 foreach ($rule in $rules) { $rule | Remove-NetFirewallRule -ErrorAction SilentlyContinue }
 if ($adapter) { Set-DnsClientServerAddress -InterfaceIndex $adapter.ifIndex -ResetServerAddresses -ErrorAction SilentlyContinue }
 if ($tun) { if (!$tun.HasExited) { Stop-Process -Id $tun.Id -Force -ErrorAction SilentlyContinue }; $tun.Dispose() }
 Clear-DnsClientCache -ErrorAction SilentlyContinue
}

