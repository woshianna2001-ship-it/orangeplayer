import bencodepy

data = bencodepy.decode(open('A35F3E8E5A8092444B10EDB352CFBFB05E158E94.torrent', 'rb').read())
files = data[b'info'][b'files']

# 找到索引 1 的文件（00001.m2ts）
file_1 = files[1]
path_parts = file_1[b'path']

print("File index 1 (00001.m2ts):")
print(f"Number of path parts: {len(path_parts)}")
for i, part in enumerate(path_parts):
    print(f"Part {i}: {part}")
    print(f"  Raw bytes: {part.hex()}")
    print(f"  Decoded UTF-8: {part.decode('utf-8', errors='replace')}")
    print(f"  Length: {len(part)} bytes")
    print()
