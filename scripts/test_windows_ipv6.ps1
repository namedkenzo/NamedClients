$ErrorActionPreference='Stop'
$errors=$null
$ast=[System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/../windows/namedP/vpn.ps1",[ref]$null,[ref]$errors)
if($errors.Count){throw ($errors|Out-String)}
$function=$ast.Find({param($node)$node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'NamedPIPv6Ranges'},$true)
if(!$function){throw 'IPv6 guard function missing'}
Invoke-Expression $function.Extent.Text
function ContainsIP($Prefix,[System.Net.IPAddress]$IP){
 $parts=$Prefix.Split('/');$network=[System.Net.IPAddress]::Parse($parts[0]).GetAddressBytes();$value=$IP.GetAddressBytes()
 for($bit=0;$bit -lt [int]$parts[1];$bit++){$index=[int][math]::Floor($bit/8);$mask=1 -shl(7-($bit%8));if(($network[$index] -band $mask)-ne($value[$index] -band $mask)){return $false}}
 return $true
}
foreach($literal in @('2001:db8::1','ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff','::1')){
 $endpoint=[System.Net.IPAddress]::Parse($literal);$ranges=@(NamedPIPv6Ranges $endpoint)
 if($ranges.Count -ne 128){throw 'Incorrect IPv6 complement size'}
 foreach($range in $ranges){if(ContainsIP $range $endpoint){throw 'Guard blocks its own ICMPv6 endpoint'}}
 foreach($other in @('2001:4860:4860::8888','2606:4700:4700::1111')){
  if(@($ranges|Where-Object{ContainsIP $_ ([System.Net.IPAddress]::Parse($other))}).Count -ne 1){throw 'IPv6 guard has a gap or overlap'}
 }
 $controlRanges=@(NamedPIPv6Ranges $endpoint $true)
 foreach($control in @('fe80::1','ff02::1:ff00:1','ff02::2')){if(@($controlRanges|Where-Object{ContainsIP $_ ([System.Net.IPAddress]::Parse($control))}).Count -ne 0){throw 'IPv6 guard blocks neighbor/router discovery'}}
 foreach($other in @('2001:4860:4860::8888','2606:4700:4700::1111')){if(@($controlRanges|Where-Object{ContainsIP $_ ([System.Net.IPAddress]::Parse($other))}).Count -ne 1){throw 'Control-traffic exclusions opened an Internet IPv6 gap'}}
}
Write-Output 'IPv6 endpoint bypass and exact non-endpoint guard ranges passed (no routes/firewall changed).'
