# Validation

Environment: Windows, Java 17.0.20.1, Minecraft 1.20.1, Forge 47.4.0, Gradle 8.8. Validation date: 2026-10-20.

Final results after the activity update: **BUILD SUCCESSFUL**, **31 progression checks passed**, **all 15 required Forge GameTests passed**. The distributable JAR was reobfuscated by ForgeGradle; its SHA-256 is recorded in `dist/SHA256SUMS.txt`. A concise execution record is in `test-results/verification.txt`.

Commands used after the final code change:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runGameTestServer --console=plain -x downloadAssets --stacktrace
```

## Automated coverage

The `progressionTest` task runs 31 checks: the original 17 progression/accounting checks, 12 activity milestone boundaries, negative counters and finite results at the largest signed 64-bit counter.

Forge GameTests run in an actual dedicated GameTest server:

1. Versioned NBT round-trip preserves every persistent field.
2. Death clone preserves XP, levels and point balances.
3. Purchase/reset changes real maximum health, prevents duplicate modifiers and enforces the reset cooldown.
4. Invalid purchase IDs and insufficient points cannot alter balances.
5. All six reward tracks grant independent attribute XP and overall XP.
6. Extreme attribute levels respect finite effect caps.
7. Player-planted mature crops reward Wisdom only after removal.
8. Failed breaks do not grant XP or erase placement provenance.
9. The actual brewing menu's shift-click path rewards a new potion exactly once, including after reinsertion with the activity cooldown cleared.
10. Closing a merchant menu does not resurrect prices already cleared by vanilla.
11. Resetting a merchant discount revalidates the existing trade result against the new required payment.
12. All 12 activity counters affect only their own passive effect.
13. Activity contributions add to existing bonuses before unchanged caps; unrelated effects retain their original values.
14. Existing NBT, death clone, purchased skills and resets preserve activity bonuses. Endurance scales from actual base health without healing or duplicate modifiers.
15. Crossing 49 → 50 invalidates bonuses, 50 → 51 reuses the cache, and Mining cannot boost woodcutting or unrelated digging.

All four new integration tests failed against the old calculation pipeline before implementation, then passed with the update. An entry-by-entry comparison against the installed baseline JAR confirmed every resource, UI class, configuration class, networking class and unrelated class is unchanged. Only the intended gameplay/helper classes, their compiler-generated nested classes, the build timestamp and the added GameTest class differ. The installed JAR itself was not overwritten.

The brewing regression first failed with zero XP on shift-click; the implementation now reads raw source tags and clears matching destination tokens. The trading regression first demonstrated a stale trade result after reset; recomputing the merchant result and broadcasting the new slots fixed it. Both pass in the final run.

## Manual checks still required

Automated tests cover progression, data and selected real Forge/vanilla integration paths. They do not establish complete playtest coverage.

- Inspect status, upgrade, activity and settings screens at multiple window sizes and GUI scales; check skin rendering, icons, tooltips, scrolling and reset confirmation.
- Playtest all reward sources and upgrades in survival, including enchanting refunds, consumed potion durations, breeding growth acceleration, natural recovery and hunger effects.
- Test two real clients on a dedicated server, including sync after reconnect, death, dimension travel, and simultaneous purchases.
- Reopen a normal survival world/server after progression, verify restored data and confirm placement protections survive restart.
- Verify compatibility with any additional mods used in the intended modpack, especially custom tools, crops, merchants and brewing recipes.
- Assess long-running-world performance and balancing under real gameplay.

## Environment notes

The downloaded Java runtime initially did not trust the environment's HTTPS certificate chain. Local commands used the Windows trusted root store; TLS verification remained enabled. Windows intermittently returned `CreateProcess error=5` when launching Java immediately after compilation; retrying the unchanged test launch succeeded. No runtime workaround is required by the shipped mod.

Forge 47.4.0 emits deprecation warnings for older 1.20.1 registration/resource-location APIs, and ForgeGradle emits a Gradle 9 compatibility notice. The project intentionally uses Gradle 8.8.
