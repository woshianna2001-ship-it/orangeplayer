Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('maven-central\bundle-test.zip')

Write-Host "`n签名文件 (.asc) 示例:"
$ascFiles = $zip.Entries | Where-Object {$_.Name -like '*.asc'}
$ascFiles | Select-Object -First 3 FullName | Format-Table -AutoSize

Write-Host "`nPOM 文件:"
$pomFiles = $zip.Entries | Where-Object {$_.Name -like '*.pom'}
$pomFiles | Select-Object FullName | Format-Table -AutoSize

Write-Host "`n统计:"
Write-Host "  AAR 文件: $(($zip.Entries | Where-Object {$_.Name -like '*.aar'}).Count)"
Write-Host "  POM 文件: $($pomFiles.Count)"
Write-Host "  签名文件: $($ascFiles.Count)"

$zip.Dispose()
