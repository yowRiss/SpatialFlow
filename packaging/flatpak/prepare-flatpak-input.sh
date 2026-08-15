#!/usr/bin/env sh
set -eu

# Stage, rather than symlink, the generated Compose application image: Flatpak
# Builder only accepts sources inside the manifest directory.
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
app_image="$repo_root/composeApp/build/compose/binaries/main/app/spatialflow"
stage_dir="$repo_root/packaging/flatpak/spatialflow"

if [ ! -x "$app_image/bin/spatialflow" ]; then
  echo "Missing Compose app image. Run ./gradlew :composeApp:createDistributable first." >&2
  exit 1
fi

rm -rf "$stage_dir"
mkdir -p "$stage_dir"
cp -a "$app_image/." "$stage_dir/"
