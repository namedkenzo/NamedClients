$ErrorActionPreference='Stop'
$prefix='namedP-validation-'+[guid]::NewGuid().ToString('N')
try {
 foreach($candidate in @('::/0','::/1','8000::/1','::1-ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff')) {
  $name=$prefix+'-'+[guid]::NewGuid().ToString('N')
  try {
   New-NetFirewallRule -Name $name -DisplayName $name -Direction Outbound -Action Block -RemoteAddress $candidate -Enabled False -Profile Any | Out-Null
   Write-Output "Provider accepted: $candidate"
  } catch { Write-Output "Provider rejected: $candidate : $($_.Exception.Message)" }
 }
 # Check every actual production guard prefix against NetSecurity, not only
 # mathematical CIDR coverage. Disabled rules cannot affect runner traffic.
 $ast=[System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/../windows/namedP/vpn.ps1",[ref]$null,[ref]$null)
 $fn=$ast.Find({param($n)$n -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq 'NamedPIPv6Ranges'},$true)
 Invoke-Expression $fn.Extent.Text
 $cases=@(@('::/1','8000::/1'),@(NamedPIPv6Ranges ([System.Net.IPAddress]::Parse('2001:db8::25')) $true))
 foreach($addresses in $cases){
  $name=$prefix+'-'+[guid]::NewGuid().ToString('N')
  New-NetFirewallRule -Name $name -DisplayName $name -Direction Outbound -Action Block -RemoteAddress $addresses -Enabled False -Profile Any | Out-Null
 }
 Write-Output 'Production IPv4/IPv6 firewall guards accepted by NetSecurity.'
} finally {Get-NetFirewallRule -Name "$prefix*" -ErrorAction SilentlyContinue | Remove-NetFirewallRule -ErrorAction SilentlyContinue}
