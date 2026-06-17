#!/bin/bash
# 验证 IJK SO 文件的 16K 对齐

echo "========================================"
echo "  验证 IJK SO 文件 16K 对齐"
echo "========================================"
echo ""

WORK_DIR="${HOME}/ijkplayer-build-official"
IJK_DIR="${WORK_DIR}/ijkplayer"

if [ ! -d "$IJK_DIR" ]; then
    echo "错误: 找不到 IJK 编译目录: $IJK_DIR"
    exit 1
fi

# 检查 readelf 是否可用
if ! command -v readelf &> /dev/null; then
    echo "错误: readelf 未安装"
    echo "安装: sudo apt install binutils"
    exit 1
fi

echo "检查 SO 文件对齐..."
echo ""

check_alignment() {
    local arch=$1
    local abi=$2
    local so_dir="${IJK_DIR}/android/ijkplayer/ijkplayer-${arch}/src/main/libs/${abi}"
    
    if [ ! -d "$so_dir" ]; then
        echo "⚠ 跳过 $arch: 目录不存在"
        return
    fi
    
    echo "[$arch - $abi]"
    echo "----------------------------------------"
    
    for so_file in "$so_dir"/*.so; do
        if [ ! -f "$so_file" ]; then
            continue
        fi
        
        local filename=$(basename "$so_file")
        
        # 使用更可靠的方法提取对齐值
        local align_output=$(readelf -l "$so_file" | grep -A 1 "LOAD" | head -2)
        local align=$(echo "$align_output" | grep -oP '0x[0-9a-fA-F]+\s*$' | head -1 | xargs)
        
        # 如果第一种方法失败，尝试第二种方法
        if [ -z "$align" ] || [ "$align" = "0x0000000000000000" ] || [ "$align" = "0x00" ]; then
            align=$(readelf -l "$so_file" | awk '/LOAD/{getline; print $NF}' | head -1)
        fi
        
        # 转换为十进制以便比较
        local align_dec=0
        if [[ "$align" =~ ^0x[0-9a-fA-F]+$ ]]; then
            align_dec=$((align))
        fi
        
        if [ $align_dec -eq 65536 ]; then
            echo "  ✓ $filename: Align = $align (65536) - 16K 对齐正确"
        elif [ $align_dec -eq 4096 ]; then
            echo "  ℹ $filename: Align = $align (4096) - 4K 对齐（32位架构正常）"
        elif [ $align_dec -gt 0 ]; then
            echo "  ⚠ $filename: Align = $align ($align_dec) - 非标准对齐"
        else
            # 直接显示完整的 LOAD 段信息
            echo "  ℹ $filename:"
            readelf -l "$so_file" | grep -A 1 "LOAD" | head -4 | sed 's/^/      /'
        fi
    done
    
    echo ""
}

# 检查所有架构
check_alignment "armv7a" "armeabi-v7a"
check_alignment "arm64" "arm64-v8a"
check_alignment "x86" "x86"
check_alignment "x86_64" "x86_64"

echo "========================================"
echo "验证完成"
echo "========================================"
echo ""
echo "说明："
echo "  - 64位架构 (arm64, x86_64): 应该是 0x10000 (16K 对齐)"
echo "  - 32位架构 (armv7a, x86): 通常是 0x1000 (4K 对齐)"
echo ""
