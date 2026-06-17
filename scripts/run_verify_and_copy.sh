#!/bin/bash
# 一键验证和复制脚本

cd /mnt/d/android/projecet_iade/orangeplayer

echo "步骤 1: 验证 16K 对齐"
bash scripts/verify_ijk_alignment.sh

echo ""
echo "按 Enter 继续复制 SO 文件，或 Ctrl+C 取消..."
read

echo ""
echo "步骤 2: 复制 SO 文件到项目"
bash scripts/copy_ijk_so.sh

echo ""
echo "全部完成！"
