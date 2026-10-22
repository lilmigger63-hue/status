# Independent source review

A separate reviewer inspected the port against the original source and matching Minecraft/NeoForge source/bytecode. Review covered common event handling, player/entity/provenance migration, configuration, networking, attribute modifiers, client source adaptation and metadata.

One important issue was found: applying arrow damage during `LivingIncomingDamageEvent` changed hurt-resistance ordering. The original Forge hook ran within `actuallyHurt`, after raw incoming comparisons and shield processing. The fix removes the incoming-event multiplier and uses precise LivingEntity/Player hooks before the first damage-container read in that stage, avoiding duplicate invulnerability checks or stale armor operands.

The reviewer rechecked that fix, its registration and the two-hit regression on both target types. Final assessment: no remaining blocker found in the reviewed code. The final runtime log independently records all 31 required tests passing (30 mod, one vanilla).

Visual pixel identity, an actual historical-world upgrade, remote multiplayer play and unrelated mods' nested damage behavior were not established by that review. See `VERIFICATION.md` for the corresponding limits.
