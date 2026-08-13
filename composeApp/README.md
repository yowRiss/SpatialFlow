# SpatialFlow Compose Multiplatform module

`:composeApp` is the migration target. It contains the shared Compose UI,
presentation logic, Navigation KMP dependency, and Ktor client setup. The
existing `:app` module remains the production Android application while Android
services, widgets, Media3 playback, Room, and file-system integration are moved
behind platform interfaces.

## Run

```bash
./gradlew :composeApp:run
./gradlew :composeApp:installDebug
```

## Fedora RPM

Install a JDK 17+ and Fedora's `rpm-build` package, then run:

```bash
./gradlew :composeApp:packageRpm
```

The RPM and the JVM-bundled application image are written under
`composeApp/build/compose/binaries/main/rpm/`. For a local smoke test, use
`./gradlew :composeApp:runDistributable`.

`packageDistributionForCurrentOS` builds the configured native package for the
host OS. Linux builds produce RPM; Windows builds produce MSI. Build each on its
target OS in CI rather than attempting cross-packaging.

## Flatpak

Compose Desktop does not emit Flatpaks directly. First create the JVM-bundled
app image with `./gradlew :composeApp:createDistributable`, then package it with
Flatpak Builder (or Conveyor). `../packaging/flatpak/com.codetrio.SpatialFlow.yml`
is a deliberately small manifest template: replace the source command with the
CI-produced application image and add your app icon, desktop file, permissions,
and release source.
