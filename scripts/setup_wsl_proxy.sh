#!/bin/bash

echo "========================================"
echo "  配置 WSL Clash 代理"
echo "========================================"

# 获取 Windows 主机 IP（WSL 网关）
HOST_IP=$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf 2>/dev/null)

if [ -z "$HOST_IP" ]; then
    echo "错误: 无法获取 Windows 主机 IP"
    exit 1
fi

echo "Windows 主机 IP: $HOST_IP"

# Clash 默认端口
CLASH_PORT="${CLASH_PROXY_PORT:-7890}"

echo "测试 Clash 代理连接..."
echo "  尝试端口: 7890, 7891, 7892, 7893"

# 尝试常见的 Clash 端口
for port in 7890 7891 7892 7893; do
    echo -n "  测试 ${HOST_IP}:${port} ... "
    if timeout 2 bash -c "echo > /dev/tcp/${HOST_IP}/${port}" 2>/dev/null; then
        CLASH_PORT=$port
        echo "✓ 可用"
        break
    else
        echo "✗ 不可用"
    fi
done

# 再次验证
if ! timeout 2 bash -c "echo > /dev/tcp/${HOST_IP}/${CLASH_PORT}" 2>/dev/null; then
    echo ""
    echo "❌ Clash 代理不可用！"
    echo ""
    echo "请检查："
    echo "  1. Clash 是否正在运行"
    echo "  2. Clash 设置 -> 允许局域网连接 (Allow LAN)"
    echo "  3. Clash 端口是否是 ${CLASH_PORT}"
    echo "  4. Windows 防火墙是否阻止了 WSL 访问"
    echo ""
    echo "临时解决方案："
    echo "  export http_proxy=http://${HOST_IP}:7890"
    echo "  export https_proxy=http://${HOST_IP}:7890"
    exit 1
fi

PROXY_URL="http://${HOST_IP}:${CLASH_PORT}"

echo ""
echo "✓ Clash 代理可用: $PROXY_URL"
echo ""
echo "设置环境变量..."

# 导出代理环境变量
export http_proxy="$PROXY_URL"
export https_proxy="$PROXY_URL"
export HTTP_PROXY="$PROXY_URL"
export HTTPS_PROXY="$PROXY_URL"
export ALL_PROXY="$PROXY_URL"
export all_proxy="$PROXY_URL"
export no_proxy="127.0.0.1,localhost"
export NO_PROXY="$no_proxy"

# 写入当前 shell 配置
cat >> ~/.bashrc << EOF

# Clash 代理配置 (自动生成)
export http_proxy="$PROXY_URL"
export https_proxy="$PROXY_URL"
export HTTP_PROXY="$PROXY_URL"
export HTTPS_PROXY="$PROXY_URL"
export ALL_PROXY="$PROXY_URL"
export all_proxy="$PROXY_URL"
export no_proxy="127.0.0.1,localhost"
export NO_PROXY="\$no_proxy"
EOF

echo "✓ 代理已配置并写入 ~/.bashrc"
echo ""
echo "测试代理..."
if curl -s --connect-timeout 5 -I https://www.google.com > /dev/null 2>&1; then
    echo "✓ 代理工作正常！可以访问 Google"
else
    echo "⚠️  无法访问 Google，但代理已配置"
fi

echo ""
echo "========================================"
echo "  配置完成！"
echo "========================================"
echo ""
echo "当前会话已生效，新终端需要运行:"
echo "  source ~/.bashrc"
echo ""
echo "或者直接运行:"
echo "  export http_proxy=$PROXY_URL"
echo "  export https_proxy=$PROXY_URL"
echo ""
