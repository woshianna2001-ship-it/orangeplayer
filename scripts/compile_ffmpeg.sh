#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ $# -gt 0 ]; then
    export ORANGE_FFMPEG_ABIS="$1"
fi

echo "调用 build_ffmpeg.sh，目标 ABI: ${ORANGE_FFMPEG_ABIS:-armeabi-v7a,arm64-v8a,x86,x86_64}"
"${SCRIPT_DIR}/build_ffmpeg.sh"
