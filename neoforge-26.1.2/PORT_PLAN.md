# Medieval Status — preservation port

Goal: Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25. Keep original mod ID, version, resources, UI coordinates/text, config keys/defaults, formulas, rewards, caps, saved identifiers and transactions.

User-confirmed baseline: downloaded latest `medievalstatus-1.0.0.jar`, SHA-256 `777589dff5eb83bd9f57a9bdfbf21e0c31f0b4fec384826620872ac8023eb855`, matching repository commit `0cefacf`. The exact reference JAR is included under `baseline/` for reproducible checks. The older installed JAR remains untouched.

- [x] Inventory original source/JAR and identify target toolchain in official NeoForge MDK.
- [x] Record all class/resource/config inventories and original-to-target mapping before API edits.
- [x] Copy core/upgrade definitions/assets unchanged; run original-vs-port pure Java parity tests.
- [x] Adapt loader registration, events, NBT/data components, SavedData and payload networking.
- [x] Adapt GUI submission/input APIs while retaining all layout constants, strings and original bitmap.
- [x] Adapt/run gameplay tests, legacy-NBT fixture tests and config/resource comparisons.
- [x] Build standalone artifact, record unavoidable API changes and unresolved engine/visual/manual checks.

Source-to-target mapping: each original class remains under `dev.medievalstatus` with the same responsibility. Only platform imports and required API adapters may differ. `core/Progression` and `Upgrade` are copied verbatim. Original Forge project remains untouched at the repository root; this standalone sibling project contains the NeoForge implementation.

Review focus: persisted ForgeData lookup on loading old player files; post-damage timing/health validation; merchant price cleanup and stale results; potion component copies during shift-click; deferred GUI pose/scissor/input equivalence.
