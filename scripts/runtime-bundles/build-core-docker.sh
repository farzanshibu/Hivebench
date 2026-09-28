#!/usr/bin/env bash
set -euo pipefail

# Build the portable Core bundle from the official Ubuntu 24.04 ARM64 base
# inside an ARM64 Docker host (Apple silicon, ARM64 Linux). Docker is only the
# build host: the archive is made from the checksum-verified Ubuntu rootfs,
# prepared in a chroot the same way build-core-on-rooted-android.sh does.
VERSION="${POCKETDEV_CORE_VERSION:-2026.09.6}"
ROOTFS_VERSION="ubuntu-24.04.5-arm64"
ROOTFS_FILE="ubuntu-base-24.04.5-base-arm64.tar.gz"
ROOTFS_SHA256="a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2"
ROOTFS_URL="https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/${ROOTFS_FILE}"
NODE_VERSION="v24.19.0"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/dist/runtime-bundles"
ARCHIVE="pocketdev-core-arm64-${VERSION}.tar.zst"
SOURCES="$OUTPUT_DIR/sources"

docker info --format '{{.Architecture}}' | grep -Eqx 'aarch64|arm64' || {
  echo "An ARM64 Docker host is required." >&2
  exit 1
}
mkdir -p "$SOURCES"
if ! printf '%s  %s\n' "$ROOTFS_SHA256" "$SOURCES/$ROOTFS_FILE" | shasum -a 256 -c - >/dev/null 2>&1; then
  curl -fL --retry 3 -o "$SOURCES/$ROOTFS_FILE" "$ROOTFS_URL"
  printf '%s  %s\n' "$ROOTFS_SHA256" "$SOURCES/$ROOTFS_FILE" | shasum -a 256 -c -
fi

docker run --rm --privileged --platform linux/arm64 \
  -v "$SOURCES:/sources:ro" -v "$OUTPUT_DIR:/output" \
  -e VERSION="$VERSION" -e ROOTFS_VERSION="$ROOTFS_VERSION" -e ROOTFS_FILE="$ROOTFS_FILE" \
  -e NODE_VERSION="$NODE_VERSION" -e ARCHIVE="$ARCHIVE" \
  ubuntu:24.04 bash -euo pipefail -c '
    apt-get update >/dev/null && DEBIAN_FRONTEND=noninteractive apt-get install -y zstd >/dev/null
    R=/build/rootfs
    mkdir -p "$R"
    tar -xzf "/sources/$ROOTFS_FILE" -C "$R"
    mount --bind /dev "$R/dev"; mount -t proc proc "$R/proc"; mount -t sysfs sysfs "$R/sys"
    trap "umount $R/sys $R/proc $R/dev || true" EXIT
    guest() { chroot "$R" /usr/bin/env -i HOME=/root USER=root LANG=C.UTF-8 \
      PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /bin/bash -lc "$1"; }

    printf "nameserver 1.1.1.1\n" > "$R/etc/resolv.conf"
    guest "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get -y upgrade"
    guest "DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git ca-certificates curl wget unzip zip xz-utils zstd less openssh-client libssl3t64"
    guest "set -e; F=node-$NODE_VERSION-linux-arm64.tar.gz; cd /tmp
      curl -fsSLO https://nodejs.org/dist/$NODE_VERSION/\$F
      curl -fsSL https://nodejs.org/dist/$NODE_VERSION/SHASUMS256.txt | grep \"  \$F\$\" | sha256sum -c -
      mkdir -p /usr/local/lib/nodejs; tar -xzf \$F -C /usr/local/lib/nodejs --strip-components=1
      # Relative links, matching RuntimeInstaller: the app resolves them from the Android side too.
      for b in node npm npx corepack; do ln -sfn ../lib/nodejs/bin/\$b /usr/local/bin/\$b; done
      rm -f /tmp/\$F"
    # Coding agents and stacks install their own tools; pip needs no PEP 668 opt-out per call.
    guest "rm -f /usr/lib/python3*/EXTERNALLY-MANAGED"
    guest "apt-get clean && rm -rf /var/lib/apt/lists/* /var/cache/apt/* /tmp/* /var/tmp/*"
    guest "mkdir -p /workspace /opt/pocketdev /root/.gradle/init.d
      printf \"$ROOTFS_VERSION\n\" > /.pocket-rootfs-version
      printf \"core-bundle-$VERSION\n\" > /.pocket-core-tools-version
      printf \"core-bundle-$VERSION\n\" > /.pocket-runtime-ready
      printf \"ubuntu-maintenance-v1\n\" > /.pocket-system-upgrade-version
      printf \"{\\\"WEB\\\":true,\\\"PYTHON\\\":false,\\\"CPP\\\":false,\\\"PHP\\\":false,\\\"ANDROID\\\":false}\n\" > /.pocket-dev-stacks.json"
    guest "rm -rf /root/.cache /root/.npm /root/.ssh; find /var/log -type f -delete
      rm -f /etc/ssh/ssh_host_* /etc/machine-id /var/lib/dbus/machine-id /root/.bash_history /etc/resolv.conf"
    umount "$R/sys" "$R/proc" "$R/dev"; trap - EXIT

    tar --sort=name --mtime=2026-09-05T00:00:00Z --owner=0 --group=0 --numeric-owner \
      -C "$R" -cf - . | zstd -19 -T0 -q -o "/output/$ARCHIVE" -f
  '

shasum -a 256 "$OUTPUT_DIR/$ARCHIVE"
wc -c "$OUTPUT_DIR/$ARCHIVE"
echo "Core bundle created: dist/runtime-bundles/$ARCHIVE"
