# Medieval Status 1.0.0 — NeoForge 26.1.2

Preservation port of the downloaded latest Forge build. The mod ID remains `medievalstatus`; the displayed name and version remain **Medieval Status 1.0.0**. The new artifact filename includes `neoforge-26.1.2` to identify its loader and Minecraft version.

Requires Minecraft **26.1.2**, NeoForge **26.1.2.122** or a compatible later build for that Minecraft version, and Java **25**. Put the NeoForge JAR in the matching instance's `mods` directory. Install only one Medieval Status build in that instance.

Press **P** to open the original status screen. Original progression, activity milestones, purchased upgrades, caps, reset behavior, configuration keys/defaults, layout and artwork are preserved in the source. The preservation reports distinguish verified behavior from checks still requiring a real client or historical world.

## Building and testing

With Java 25 selected, run from this folder:

```
./gradlew.bat build runGameTestServer
```

The project uses the official NeoForge ModDevGradle binary development pipeline. On systems with restrictive intermediate-cache file handling, add `--no-configuration-cache -PneoForge.neoFormRuntime.enableCache=false`.

`build` runs 31 standalone progression checks, 70,014 exact comparisons against the included SHA-pinned reference JAR, and resource checks. `runGameTestServer` executes the original integration scenarios plus migration/platform regressions. Test evidence and limitations are recorded in `reports/VERIFICATION.md`.

`reports/run-standalone-parity.ps1` runs the core/resource comparison without Gradle, using `JAVA_HOME`. `reports/check-source-parity.ps1` additionally compares against the original source when this folder is inside the complete repository.

## Preservation documentation

- `reports/PARITY-MAPPING.md`: every original class/resource and its target mapping.
- `reports/DEFAULTS-AND-FORMULAS.md`: original configuration, effects, formulas and gates.
- `reports/SAVE-COMPATIBILITY.md`: save envelopes, paths, migration and limits.
- `reports/API-CHANGES.md`: required platform changes.
- `reports/VERIFICATION.md`: evidence and unresolved validation limits.
- `baseline/medievalstatus-1.0.0.jar`: the exact original reference, **not** the NeoForge installation artifact.
