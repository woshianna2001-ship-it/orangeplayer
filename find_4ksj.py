import bencodepy

data = bencodepy.decode(open('A35F3E8E5A8092444B10EDB352CFBFB05E158E94.torrent', 'rb').read())
files = data[b'info'][b'files']

print(f"Total files: {len(files)}\n")
print("Searching for files containing '4ksj' or '00001':")
for i, f in enumerate(files):
    path = b'/'.join(f[b'path']).decode('utf-8', errors='replace')
    size = f[b'length']
    if '4ksj' in path.lower() or '00001' in path:
        print(f"Index {i}: {path}")
        print(f"  Size: {size:,} bytes")
        print()
