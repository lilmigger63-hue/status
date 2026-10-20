# Medieval Status progress

Design: standalone Forge 1.20.1 / Java 17 mod. Three independent progression systems; server-authoritative awards, purchases and resets. Original stone/gold UI follows the supplied layout. No classes, spells or races.

## Completed
- Inspected reference specification, screenshot and initially empty repository.
- Downloaded official Forge 47.4.0 MDK and local Java 17.
- Added regression cases for progression and point accounting before implementation.
- Implemented progression, versioned player NBT, successful-action rewards, placement provenance, 18 passive upgrades, capped cached bonuses and validated networking.
- Added original tiled stone/gold status UI, upgrade categories, tooltips, activity details, settings and validated optional layout JSON.
- Independent code review completed; fixed crop eligibility, client mining speed, pre-break awards, save timing and trade restoration.
- `gradlew build`: successful, 17 progression checks passed (2026-10-20).
- Eleven Forge GameTests passed, including real brewing shift-click, reinsertion and merchant payment revalidation.
- Final `gradlew build` and `gradlew runGameTestServer -x downloadAssets --stacktrace` successful, 2026-10-20.
- Reobfuscated distributable in `dist/medievalstatus-1.0.0.jar`, SHA-256 in `dist/SHA256SUMS.txt`.
- Installation README, configuration examples, CI workflow and validation report included.

## Remaining
- Upload committed source and JAR to origin/main once GitHub authentication is available.
- Manual visual, survival and two-client multiplayer checks listed in TESTING.md.

Last successful build: `gradlew build`, 2026-10-20, 17 progression checks and 11 dedicated Forge GameTests passed.
Blocker: Git Credential Manager has no saved GitHub login; push dry-run could not authenticate. Two device-login attempts returned no authorization code and were canceled; user GitHub sign-in is required.
Next: authenticate GitHub, then `git push origin main`. No implementation work remains before upload; manual playtest coverage is documented separately.
