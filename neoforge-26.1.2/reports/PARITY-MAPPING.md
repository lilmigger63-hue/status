# Baseline and scope

The immutable reference is `../dist/medievalstatus-1.0.0.jar`, SHA-256 `777589dff5eb83bd9f57a9bdfbf21e0c31f0b4fec384826620872ac8023eb855`. The downloaded latest artifact has the same hash. Original source under `../src` is unchanged. Target: Minecraft 26.1.2, NeoForge 26.1.2.122, Java 25; mod ID `medievalstatus`, version `1.0.0`.

`original-jar-inventory.csv` inventories every original JAR entry with size and SHA-256. `original-classes.txt` is the exact compiled-class list. `original-source-inventory.csv` hashes every original source/resource file. Captured `original-config.java.txt`, `original-bonuses.java.txt`, and `original-events.java.txt` preserve the complete original expressions used for this mapping.

# Class mapping

All package and class identities below remain under `dev.medievalstatus`; this inventory covers every original compiled class, including nested records/classes.

| Original class | Responsibility preserved | Required platform adaptation |
|---|---|---|
| `MedievalStatus` | ID, config registration, networking setup | NeoForge `@Mod`, constructor-injected mod container/event bus |
| `Config` | All keys, defaults, limits and upgrade iteration order | `ForgeConfigSpec` to `ModConfigSpec` |
| `ConfigEvents` | Server config reload marks online players' data/bonuses dirty | NeoForge config event/subscription |
| `core.Progression` | Independent XP tracks, points, purchases, reset, formulas | No gameplay or formula change |
| `core.Progression$Level` | Level/XP/lifetime restoration and capped awards | No change |
| `Upgrade` | 18 ordinal IDs, titles, categories, effects | No change |
| `PlayerData` | Persistent schema, cooldowns, counters, online cache | Optional NBT getters replaced with legacy-compatible default readers |
| `PlayerData$PendingBreak` | Queued level/position/state/provenance | Current level API |
| `PlayerData$PriceChange` | Before/applied merchant special price | No semantic change |
| `Bonuses` | Cached capped effects and non-stacking transient modifiers | Holder attributes; identifier-based modifier IDs; renamed operations |
| `SkillService` | Server transaction validation, reset/refund/cooldown | Current player/merchant APIs |
| `PlacedBlocks` | Durable `medievalstatus_placed`, packed `positions` long array | Modern SavedData codec/storage API retaining disk keys |
| `Events` | Login, clone, respawn, dimension, action rewards, provenance, tick refresh | NeoForge event names/lifecycle, item components, current entity/potion APIs |
| `Network` | Request validation and authoritative snapshot | NeoForge payload registration/stream codecs, main-thread handling |
| `Network$Request` | Integer operation (`-2` snapshot, `-1` reset, `0..17` upgrade) | Current custom payload contract |
| `Network$Snapshot` | NBT player/config/bonus snapshot | Current custom payload contract |
| `client.ClientState` | Last snapshot, parsed state, logout clear | NBT default-reader compatibility |
| `client.ClientEvents` | P key, request/open, client mining prediction, logout | Current key mapping/tick/client events |
| `client.ClientEvents$Registration` | Client-only key mapping registration | NeoForge mod event subscriber |
| `client.StatusScreen` | Existing stone/gold layout, controls, scroll, help, confirmation | Current GUI extraction/render and mouse input APIs |
| `client.UiLayout` | Optional JSON layout, validation and safe defaults | NeoForge config path |
| `StatusGameTests` | Original 11 integration test bodies/assertions | Explicit test-function and test-instance registration; `ItemCost`; spawn reason |
| `ActivityBonusGameTests` | Original 4 activity integration test bodies/assertions | Same registration plus optional NBT reads |

New platform helper `Nbt` supplies old zero/empty default behavior for scalar, compound and array reads. It does not introduce new persistent fields. `LegacyPlayerData` imports the original Forge player envelope on first access without overwriting an existing modern payload. The network transport is loader/version specific; this is not a protocol-compatibility promise between Forge 1.20.1 and NeoForge 26.1.2 clients/servers.

# Resource mapping

| Original JAR resource | Target | Policy |
|---|---|---|
| `assets/medievalstatus/lang/en_us.json` | Same path | All original translations retained; required `key.category.medievalstatus.medievalstatus` alias added for modern registered key category |
| `assets/medievalstatus/textures/gui/stone.png` | Same path | Byte-for-byte hash equality |
| `data/medievalstatus/structures/empty.nbt` | `data/medievalstatus/structure/empty.nbt` | Required singular directory rename; NBT bytes unchanged |
| `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` | Required loader metadata/dependency changes |
| `pack.mcmeta` | Same path | Target pack format metadata |
| `META-INF/MANIFEST.MF` | Generated target manifest | Build/toolchain metadata |

`AssetParityTest` reads resource bytes directly from the baseline JAR and checks SHA-256 against a target resource directory or built target JAR. It covers all original `assets/` and `data/` files, with the directory mapping above and the explicitly checked key-category localization addition (newline encoding normalized only for that language comparison). Texture and structure still require exact SHA-256 equality. A matching texture hash proves asset identity, not identical rendering in the newer game engine.

# Persistent and snapshot contract

Player persistent key: `medievalstatus`. Version remains integer `1`. `overall` and the six named attributes (`Strength`, `Dexterity`, `Constitution`, `Intelligence`, `Wisdom`, `Charisma`) each contain long `level`, double `xp`, double `lifetime`. Other fields: long `earned`, long `spent`, int array `ranks` (18 ordinal entries), long `resetAt`, compound `activities` of recognized action-name long counters.

Absent or mistyped data retains original zero/empty reader behavior; `restore` then clamps levels to at least 1, finite XP to `[0,1e100]`, lifetime to at least XP, earned to nonnegative, spent to `[0,earned]`, ranks to `[0,100]`, reset deadline/counters to nonnegative. Unknown activity names are ignored. Missing schema version does not discard known fields.

Snapshot adds `bonuses`, `attributeBase`, `attributePower`, `overallBase`, `overallPower`, `upgradeConfig` (per enum name: `cost`, `max`, `effect`, `enabled`), `miningCap`, `resetEnabled`, `resetCost`. Baseline channel protocol was `1`; transport registration changes are necessary for the new loader. World provenance retains the base identifier `medievalstatus_placed` and array field `positions`; the required modern namespaced SavedData identifier is `medievalstatus:medievalstatus_placed`.

# Legacy file migration

`PlacedBlocks` uses a `SavedDataType` with a compound codec that reads and writes the exact original `positions` long-array field, without converting coordinates or truncating packed longs. NeoForge's official `SavedDataType` patch supports a null data-fix type for mod-owned schemas; no vanilla data fixer transforms this payload.

The modern record lives in the current dimension data folder at `medievalstatus/medievalstatus_placed.dat`. On first access, an existing loaded record wins. If a modern file exists but fails decoding, loading stops with a readable error; it neither creates empty data nor resurrects older consumed entries. Only when the modern record/file is absent does the importer read the original compressed NBT basename `medievalstatus_placed.dat`, first in the current dimension data folder (if Minecraft relocated it), then the historical dimension location. Historical locations are root `data/` for the overworld, `DIM-1/data/` for Nether, `DIM1/data/` for End, and `dimensions/<namespace>/<dimension>/data/` for custom dimensions. The root `data` envelope is read, positions imported into a new dirty record, and the original file remains untouched. Read errors stop import instead of silently saving empty provenance.

Player migration reads `ForgeData.medievalstatus`, copies the compound independently, and preserves all original version-one fields. An existing destination key is authoritative, including a key of an unexpected type. The parent implementation uses Minecraft's player-data lookup first, with read-only legacy `playerdata/<UUID>.dat` and integrated-owner `level.dat` (`Data.Player`) fallbacks.

# Verification and limits

`OriginalJarParityTest` pins the JAR hash and creates `URLClassLoader(parent=null)` so reference classes cannot resolve from target source/classes. It verifies that reference code origin points to the JAR. The target is the port's compiled `core.Progression`.

The recorded standalone run passed 70,014 exact comparisons: curve matrix, bounded scale, action milestones (including 49/50/51), saturating addition, initial state, independent attributes, exactly five points per overall level, multiple-level awards, invalid/valid purchases and rank caps, refunds preserving progression, nonfinite/large XP, saved-value repairs, rank/point clamps, level ceiling, advancement work bound, and 1,500 deterministic mixed operations. Floating results are compared with boxed `Double.equals`, not an epsilon tolerance.

GameTests retain the original 15 scenarios and add seven migration cases: empty/wrong-type data, unversioned complete state, corrupt saved values, legacy numeric coercions/four-int UUID representation, original Forge player envelope import/no alias/no overwrite, provenance codec packed-long round trip, and compressed legacy provenance discovery/import/no-source-mutation/modern-file precedence. Additional platform-focused tests may be registered separately. Compilation alone does not establish that these integration assertions pass. GameTest execution and client/server launch results must be reported separately by the final build owner. No claim of perfect visual, vanilla world-upgrade, or untested multiplayer compatibility follows from core/asset parity.
