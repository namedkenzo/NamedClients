#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
android_native_root=$PWD
android_ndk_path="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to the installed NDK}"
android_clang_bin="$android_ndk_path/toolchains/llvm/prebuilt/linux-x86_64/bin"
android_tun_source=$(go mod download -json github.com/xjasonlyu/tun2socks/v2@v2.6.0 | jq -r .Dir)
[[ -d $android_tun_source ]] || { echo 'Missing pinned TUN source';exit 1; }
for android_abi in arm64-v8a x86_64; do
 case $android_abi in
  arm64-v8a)android_go_arch=arm64;android_compiler=aarch64-linux-android26-clang;;
  x86_64)android_go_arch=amd64;android_compiler=x86_64-linux-android26-clang;;
 esac
 android_output="$android_native_root/android/app/src/main/jniLibs/$android_abi"
 mkdir -p "$android_output"
 GOOS=android GOARCH="$android_go_arch" CGO_ENABLED=1 CC="$android_clang_bin/$android_compiler" \
  go build -trimpath -buildmode=pie -ldflags='-s -w -extldflags=-Wl,-z,max-page-size=16384' -o "$android_output/libnamedp.so" ./cmd/namedp-android-engine
 GOOS=android GOARCH="$android_go_arch" CGO_ENABLED=1 CC="$android_clang_bin/$android_compiler" \
  go -C "$android_tun_source" build -trimpath -buildmode=pie -ldflags='-s -w -extldflags=-Wl,-z,max-page-size=16384' -o "$android_output/libtun2socks.so" .
done
mkdir -p android/app/src/main/assets/licenses
cp "$android_tun_source/LICENSE" android/app/src/main/assets/licenses/TUN2SOCKS-LICENSE.txt
cp LICENSE THIRD_PARTY_NOTICES.md android/app/src/main/assets/licenses/
