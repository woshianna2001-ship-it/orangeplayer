# 批量替换文档中的 GSY 库 groupId
# 从 io.github.carguo 改为 io.github.706412584

$files = Get-ChildItem -Path "docs" -Filter "*.md" -Recurse

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    
    # 替换 groupId
    $newContent = $content -replace 'io\.github\.carguo:gsyvideoplayer', 'io.github.706412584:gsyVideoPlayer'
    $newContent = $newContent -replace 'io\.github\.carguo:gsyijkjava', 'io.github.706412584:gsyijkjava'
    
    # 替换版本号
    $newContent = $newContent -replace ':11\.3\.0', ':1.1.0'
    
    if ($content -ne $newContent) {
        Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8 -NoNewline
        Write-Host "Updated: $($file.FullName)"
    }
}

Write-Host "`nDone! All documentation files have been updated."
