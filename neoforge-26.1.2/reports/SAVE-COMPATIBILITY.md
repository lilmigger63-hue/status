# Save compatibility

The definitive reference is the user-confirmed downloaded latest JAR, SHA-256 `777589dff5eb83bd9f57a9bdfbf21e0c31f0b4fec384826620872ac8023eb855`.

The mod's version-one player compound remains unchanged: same key `medievalstatus`, six attribute names, overall level/XP/lifetime doubles and longs, earned/spent points, 18 ordinal ranks, reset deadline and activity counts. Original defaulting and clamps are retained through the NBT adapter. Import does not award XP, refund points, rewrite counters or add a version field to an original payload.

Forge stored persistent entity data under `ForgeData`; current NeoForge uses `NeoForgeData`. A required entity-load adapter imports only the original mod-owned keys (`medievalstatus`, `medievalstatus_damage`, `medievalstatus_endured`). Every imported NBT value is copied, retaining its type and value. Existing modern keys take precedence. Other mods' data is not imported. This also preserves the reward-budget counters of surviving original mobs.

Player first-load fallback uses Minecraft's player lookup and can read the old `playerdata/<UUID>.dat` location or integrated owner's `level.dat` / `Data.Player`. Fallback reads never edit the old file. Unreadable fallback data stops import with an error instead of silently initializing empty progression. NeoForge subsequently saves the imported compound using its normal player-save mechanism.

Placed-block provenance keeps the exact packed `positions` long array and basename `medievalstatus_placed`. Modern SavedData requires the namespaced ID `medievalstatus:medievalstatus_placed`; its file is `data/medievalstatus/medievalstatus_placed.dat` in the current dimension storage folder. When absent, the adapter imports the original compressed `data/medievalstatus_placed.dat`, including historical overworld, Nether, End and custom-dimension paths. Original files remain untouched. Existing modern data wins; unreadable modern data or legacy import produces an error instead of resetting protection or resurrecting older consumed records.

The original server configuration filename and keys are unchanged. NeoForge can use a global `config/medievalstatus-server.toml` and honors a world's existing `serverconfig/medievalstatus-server.toml` as an override. Client configuration and optional layout JSON retain their filenames and contents.

Automated fixtures exercise exact payload preservation, missing/wrong types, numeric coercion, signed UUID arrays, clamps, cloning, packed-position codec round trips, legacy compressed files, source immutability, modern-data precedence, and actual entity loading through the adapter. These checks establish the mod-owned format migration. An actual user world upgraded from Minecraft 1.20.1 has not been opened here; vanilla terrain, registry/data-fixer changes and unrelated mods' saves are outside this evidence. Full-world upgrade compatibility is not claimed.
