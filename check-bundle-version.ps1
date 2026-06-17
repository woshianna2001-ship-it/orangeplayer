Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('maven-central\bundle-test.zip')
$poms = $zip.Entries | Where-Object { $_.FullName -like '*.pom' }
Write-Host "POM files in bundle:"
$poms | ForEach-Object { Write-Host $_.FullName }
$zip.Dispose()
