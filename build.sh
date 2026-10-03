#!/usr/bin/env bash
# ============================================================
#  GTS GiantAI 构建脚本
#  用法:
#    ./build.sh          # 构建 java17 (默认)
#    ./build.sh all      # 构建 java17 + java21 + java25
#    ./build.sh 21       # 仅 java21
#  前置: JDK 17/21/25 之一在 PATH 中；Maven 3.8+（或使用 -javac 直接编译）
# ============================================================
set -e
cd "$(dirname "$0")"

TARGET_JDK="${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}"

build_java17() {
  if command -v mvn >/dev/null 2>&1; then
    mvn -q clean package -Pjava17
  else
    echo "[!] 未找到 mvn，尝试 javac 直接编译（需要 paper-api 依赖在 lib/ 下）"
    javac_direct
  fi
}

javac_direct() {
  # 直接编译路径：需要 lib/paper-api.jar 与 lib/paper-server.jar（完整服务端jar）
  if [ ! -f lib/paper-api.jar ]; then
    echo "[x] 缺少 lib/paper-api.jar，请放入 Paper 1.20.1 的 paper-api jar"
    exit 1
  fi
  mkdir -p target/classes
  find src/main/java -name "*.java" > target/sources.txt
  CP="lib/paper-api.jar"
  for j in lib/paper-server.jar lib/libs/*.jar; do
    [ -f "$j" ] && CP="$CP:$j"
  done
  javac -encoding UTF-8 --release 17 -cp "$CP" -d target/classes @target/sources.txt
  cp -r src/main/resources/* target/classes/
  jar cf target/GTSGiantAI-java17.jar -C target/classes .
  echo "[✓] 构建完成: target/GTSGiantAI-java17.jar"
}

case "${1:-17}" in
  all)
    build_java17
    mvn -q clean package -Pjava21 2>/dev/null || echo "[!] java21 构建需要 JDK21"
    mvn -q clean package -Pjava25 2>/dev/null || echo "[!] java25 构建需要 JDK25"
    ;;
  21) mvn -q clean package -Pjava21 ;;
  25) mvn -q clean package -Pjava25 ;;
  *) build_java17 ;;
esac

ls -la target/*.jar 2>/dev/null || true
