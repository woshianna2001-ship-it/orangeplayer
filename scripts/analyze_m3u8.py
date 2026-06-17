#!/usr/bin/env python3
import re
import sys

with open('/tmp/test.m3u8', 'r') as f:
    lines = f.readlines()

time = 0
disc_count = 0
segment_count = 0

print("=== M3U8 时间轴分析 ===\n")

for i, line in enumerate(lines, 1):
    if 'DISCONTINUITY' in line:
        disc_count += 1
        print(f'\n🔴 Discontinuity #{disc_count} at line {i}')
        print(f'   累计时间: {time:.2f}秒 ({time/60:.2f}分钟)')
        print(f'   已播放片段: {segment_count}')
        print()
    elif line.startswith('#EXTINF'):
        duration = float(re.findall(r'[0-9.]+', line)[0])
        time += duration
        segment_count += 1
        
        # 显示关键位置的详细信息
        if i <= 15 or (150 <= i <= 165) or (170 <= i <= 185):
            next_line = lines[i] if i < len(lines) else ""
            print(f'Line {i:3d}: +{duration:6.2f}s → 累计: {time:7.2f}s | {next_line.strip()}')

print(f'\n总时长: {time:.2f}秒 ({time/60:.2f}分钟)')
print(f'总片段数: {segment_count}')
print(f'Discontinuity 数量: {disc_count}')
