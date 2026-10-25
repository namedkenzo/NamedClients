# Run real Wintun + IP/DNS setup and real disabled firewall validation; only
# global route mutations are mocked to preserve the CI runner's connectivity.
$ErrorActionPreference='Stop'
# Windows PowerShell module autoload can replace same-name test functions.
# Import first, then override only the host-route mutations below.
Import-Module NetTCPIP,NetSecurity,NetAdapter -ErrorAction Stop
$routeEvents=[System.Collections.Generic.List[string]]::new()
function Get-NetRoute {
 [CmdletBinding()]param([string]$AddressFamily,[string]$DestinationPrefix)
 if($DestinationPrefix -in @('0.0.0.0/0','::/0')){NetTCPIP\Get-NetRoute @PSBoundParameters}
}
function New-NetRoute {
 [CmdletBinding()]param($DestinationPrefix,$InterfaceIndex,$NextHop,$RouteMetric,$PolicyStore)
 $routeEvents.Add('add '+$DestinationPrefix)
 [pscustomobject]@{Fixture=$true;DestinationPrefix=$DestinationPrefix;InterfaceIndex=$InterfaceIndex;NextHop=$NextHop}
}
function Remove-NetRoute {
 [CmdletBinding(SupportsShouldProcess=$true)]param([Parameter(ValueFromPipeline=$true)]$InputObject)
 process{if(!$InputObject.Fixture){throw 'Refusing to remove a non-fixture route'};$routeEvents.Add('remove '+$InputObject.DestinationPrefix)}
}
function New-NetFirewallRule {
 [CmdletBinding()]param($Name,$DisplayName,$Direction,$Action,[string[]]$RemoteAddress,$Profile,$Protocol,$RemotePort,$InterfaceAlias)
 NetSecurity\New-NetFirewallRule @PSBoundParameters -Enabled False
}
$before=@(NetSecurity\Get-NetFirewallRule -Name 'namedP-*' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)
[Console]::SetIn([IO.StringReader]::new(''))
foreach($command in @('New-NetRoute','Remove-NetRoute','New-NetFirewallRule')){
 if((Get-Command $command).ModuleName){throw "Fixture override was replaced: $command"}
}
& "$PSScriptRoot/../dist/namedP/vpn.ps1" -ParentPid $PID -EnginePid $PID -ServerIP '192.0.2.25' -Port 1080
if($LASTEXITCODE){throw 'VPN guardian failed'}
Write-Output ('Fixture route events: '+($routeEvents -join ', '))
foreach($prefix in @('192.0.2.25/32','0.0.0.0/1','128.0.0.0/1')){
 if(!$routeEvents.Contains('add '+$prefix) -or !$routeEvents.Contains('remove '+$prefix)){throw "Missing setup/cleanup for $prefix"}
}
$after=@(NetSecurity\Get-NetFirewallRule -Name 'namedP-*' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)
if(@($after | Where-Object {$_ -notin $before}).Count){throw 'Firewall rules leaked after guardian exit'}
for($attempt=0;$attempt -lt 20;$attempt++){
 if(!@(Get-NetAdapter | Where-Object {$_.Name -like 'namedP-*'}).Count){break};Start-Sleep -Milliseconds 250
}
if(@(Get-NetAdapter | Where-Object {$_.Name -like 'namedP-*'}).Count){throw 'Wintun adapter leaked'}
Write-Output 'Real Wintun, IP, DNS, firewall validation and guardian EOF cleanup passed; host default routes were not modified.'
