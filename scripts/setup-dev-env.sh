#!/bin/bash
# 自动安装 Rust 工具链和交叉编译目标
set -e

echo "🔧 Setting up MTR Optimizer Development Environment..."

# 检查 Rust
if ! command -v cargo &> /dev/null; then
    echo "❌ Rust not found. Installing via rustup..."
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source $HOME/.cargo/env
fi

echo "📦 Installing Rust targets for cross-compilation..."
rustup target add x86_64-pc-windows-msvc
rustup target add x86_64-unknown-linux-gnu
rustup target add aarch64-unknown-linux-gnu
rustup target add x86_64-apple-darwin
rustup target add aarch64-apple-darwin

echo "🛠️ Installing Rust components (fmt, clippy)..."
rustup component add rustfmt clippy

# 检查 Java
if ! command -v javac &> /dev/null; then
    echo "⚠️ Java JDK not found in PATH. Please install JDK 21."
else
    echo "☕ Java version: $(javac -version)"
fi

echo "✅ Development environment setup complete!"
echo "Run './gradlew buildLocalNative build' to test your first build."