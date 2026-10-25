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
} finally {Get-NetFirewallRule -Name "$prefix*" -ErrorAction SilentlyContinue | Remove-NetFirewallRule -ErrorAction SilentlyContinue}
