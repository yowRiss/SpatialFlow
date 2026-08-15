# Flatpak staging

Build the Compose application image, stage it as a Flatpak Builder source, and
then build it with Flatpak Builder:

```sh
./gradlew :composeApp:createDistributable
./packaging/flatpak/prepare-flatpak-input.sh
flatpak-builder --user --install-deps-from=flathub --force-clean build/flatpak \
  packaging/flatpak/com.codetrio.SpatialFlow.yml
```

The manifest receives the staged directory as its module working directory and
installs the complete Compose app image under `/app/libexec/spatialflow`,
preserving the launcher/JVM layout. It grants the
window-system, PulseAudio, GPU, IPC, network, and read-only Music-directory
access required by Compose Desktop and the embedded YouTube Music JCEF login.

`libVLC` is deliberately not faked through host filesystem access. Before a
Flatpak release, vendor a pinned VLC module (and its decoder dependencies) into
the manifest or a checked-in shared module; the RPM release already declares
the equivalent Fedora `vlc-libs` dependency.
