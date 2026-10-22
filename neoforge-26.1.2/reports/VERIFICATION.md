# Verification — 2026-10-22

Baseline: user-confirmed downloaded latest `medievalstatus-1.0.0.jar`, SHA-256 `777589dff5eb83bd9f57a9bdfbf21e0c31f0b4fec384826620872ac8023eb855`. Root Forge source and original distribution JAR remain unchanged.

Target: Minecraft 26.1.2, NeoForge 26.1.2.122, Java 25, ModDevGradle 2.0.148, Gradle 9.2.1. Artifact: `medievalstatus-1.0.0-neoforge-26.1.2.jar`; displayed mod name/version remain Medieval Status 1.0.0.

## Results

| Check | Result | Evidence |
|---|---|---|
| Compilation and JAR build | Passed | `build-results.txt` |
| Original-JAR progression comparison | 70,014 exact comparisons passed | `original-jar-progression-parity.txt` |
| Existing standalone progression suite | 31 checks passed | `build-results.txt` |
| Configuration/default/range/upgrade/core preservation | Passed source comparisons | `source-parity.txt` |
| Passive formulas/caps/precision/snapshot preservation | Passed source comparisons | `source-parity.txt` |
| UI layout settings and every screen string literal/order | Passed source comparisons | `source-parity.txt` |
| Original texture and structure | Exact SHA-256 matches in built JAR | `built-jar-resource-parity.txt` |
| Language resource | All original text retained; required key-category alias only | `built-jar-resource-parity.txt` |
| NeoForge dedicated GameTest server | **30 mod tests passed**, plus 1 vanilla test; zero failures | `game-test-results.txt` |
| Client startup and resource loading | Passed; client/config/key/payload subscribers loaded, rendering/audio initialized, clean normal exit | `client-startup-results.txt` |
| Packaged classes | All 32 main compiled classes match the JAR byte for byte | `artifact-validation.txt` |
| Independent source review | Arrow timing issue identified and fixed; no remaining reviewed blocker | `REVIEW.md` |

The 30 mod GameTests include all 15 original scenarios. Fifteen added cases cover legacy missing/wrong-type/unversioned/corrupt/numeric data, signed UUID representation, exact Forge player-envelope import, actual entity-load migration, packed-block codec/import paths and precedence, post-absorption killing blows/overkill, fully absorbed hits, surviving versus fatal endurance, potion duration/flags/cap, food-state preservation, raw arrow/melee hurt-resistance sequencing on both mobs and players, and zero-count brewing token redemption. Individual registered IDs are listed in the runtime evidence.

The build uses executable Java main suites rather than JUnit; Gradle's empty JUnit discovery is therefore permitted, while all three explicit regression tasks are required by `check`. Tests were run with Windows certificate-store trust and the official binary development pipeline; TLS verification was not disabled.

## Corrections discovered during verification

The initial server run exposed a bare potion test fixture that no longer triggers NeoForge's brewed-potion event. The fixture now contains an actual Awkward potion, matching a real brewed item; the original reward/reinsertion assertions are unchanged. A separate zero-count test preserves the previous shift-click edge case.

Review identified that NeoForge's incoming-damage event runs earlier than the original Forge hurt event. Applying the arrow multiplier there changed raw hurt-resistance bookkeeping. Small compatibility hooks now apply the same multiplier after those checks and before armor. The real two-hit test verifies this correction for both target pipelines.

Windows denied some intermediate cache timestamp/move operations and one Java subprocess start. The supported no-recompilation development pipeline and cache-disabled invocation resolved the cache limitation; the same server launch then completed successfully. These environment failures were not reported as passing test runs. Upstream Gradle deprecation notices and two NeoForge shader sampler warnings appeared during otherwise successful builds/client startup; no mod exception or runtime failure remained in the final runs.

## Unresolved validation limits

- No screenshot comparison or in-world P-screen interaction was performed. Original bitmap, strings, coordinates, colors, layout, animation expressions and transparent background implementation are preserved, but pixel-identical appearance under the new engine/font/item renderers is not proven.
- No real user world was upgraded from 1.20.1. Mod-owned data migration is tested with original-schema fixtures and actual target entity loading; full vanilla/other-mod world upgrade compatibility is not proven.
- A real remote multiplayer client/server session, configuration reload during such a session, and interactions with unrelated mods' nested damage callbacks were not exercised.
- The comparison against original executable bytecode directly covers the pure progression core. Platform-dependent behavior is covered by preserved expressions, code review and target integration tests; it is not a full simultaneous old-engine/new-engine simulation.

No known unresolved formula, content, cap, configuration-default or tested-gameplay discrepancy remains. These limits prevent a claim of perfect 1:1 compatibility.
