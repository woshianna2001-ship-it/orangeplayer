import bencodepy

data = bencodepy.decode(open('A35F3E8E5A8092444B10EDB352CFBFB05E158E94.torrent', 'rb').read())
files = data[b'info'][b'files']

print(f"Total files: {len(files)}\n")
print("Largest 10 files:")
sorted_files = sorted(enumerate(files), key=lambda x: x[1][b'length'], reverse=True)

for i, (idx, f) in enumerate(sorted_files[:10]):
    path = b'/'.join(f[b'path']).decode('utf-8', errors='replace')
    size = f[b'length']
    print(f"{i}. Index {idx}: {path}")
    print(f"   Size: {size:,} bytes ({size / 1024 / 1024 / 1024:.2f} GB)")
    print()
