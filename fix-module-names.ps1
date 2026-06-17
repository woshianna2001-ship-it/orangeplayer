# 修正文档中的模块名，使其与实际发布的模块名一致

$files = Get-ChildItem -Path "." -Include "*.md" -Recurse | Where-Object { $_.FullName -notlike "*\GSYVideoPlayer-source\*" }

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # 修正模块名
    $content = $content -replace 'gsyVideoPlayer-exo2', 'gsyVideoPlayer-exo_player2'
    $content = $content -replace 'gsyVideoPlayer-androidvideocache', 'gsyVideoPlayer-proxy_cache'
    $content = $content -replace 'gsyVideoPlayer-ijkplayer', 'gsyVideoPlayer-java'
    
    # 修正单独的 gsyVideoPlayer（不带后缀的）改为 gsyVideoPlayer-java
    # 但要排除已经有后缀的情况
    $content = $content -replace "io\.github\.706412584:gsyVideoPlayer:([0-9])", 'io.github.706412584:gsyVideoPlayer-java:$1'
    $content = $content -replace "io\.github\.706412584:gsyVideoPlayer'", "io.github.706412584:gsyVideoPlayer-java'"
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Fixed: $($file.Name)"
    }
}

Write-Host "`nDone! All module names have been corrected."
