# Medieval Status

A standalone **Minecraft 1.20.1 / Forge 47.x / Java 17** RPG progression mod. Press **P** to open the original dark stone and antique gold character sheet. Install the same version on the client and server; no additional mods are required.

## Install

1. Install Minecraft Forge for **1.20.1** (developed against **47.4.0**).
2. Put `medievalstatus-1.0.0.jar` in the installation's `mods` folder, and in the server's `mods` folder for multiplayer.
3. Launch a world and press **P**. Rebind under Options → Controls → Medieval Status.

Prebuilt JARs are stored in this repository's `dist` directory after validation.

A separate [Minecraft 26.1.2 / NeoForge port](neoforge-26.1.2/README.md) preserves this build's progression and content. Its [JAR](dist/medievalstatus-1.0.0-neoforge-26.1.2.jar), [complete source project](dist/medievalstatus-1.0.0-neoforge-26.1.2-source.zip), and [verification report](neoforge-26.1.2/reports/VERIFICATION.md) are provided separately; the original Forge build remains available.

## Progression

Six independent attributes start at level 1. Actions give attribute XP and separate character XP. Attribute levels cannot be purchased.

| Attribute | Rewarded activities | Actual effects |
| --- | --- | --- |
| Strength | Mine natural stone/ore, chop unplaced logs, hit monsters in melee | Mining, woodcutting, melee damage |
| Dexterity | Meaningful grounded sprint movement, arrow hits on monsters | Movement, attack speed, arrow damage |
| Constitution | Survive monster attacks, recover combat injuries | Health, armor, food-fueled recovery |
| Intelligence | Enchant, collect freshly brewed potions, occasional crafting | Chance to refund one enchantment level, longer drunk potions |
| Wisdom | Break mature crops, breed animals | Bounded extra crop produce chance |
| Charisma | Complete villager trades | Bounded temporary trade discounts |

Attribute XP requirement: `floor(100 × level^1.6)`. Character requirement: `floor(150 × level^1.5)`. Character levels start at 1 with zero points and award **exactly 5 skill points per level crossed**, including multi-level awards. There is no gameplay level cap; signed 64-bit storage and saturated accounting prevent overflow. All bonuses have diminishing returns and hard effect caps.

Every complete **50 rewarded actions in an activity adds 2.5%** to its associated passive effect, before the existing cap. Numeric effects use the base value; chances and reductions gain percentage points. These bonuses are derived from existing counters, remain after Reset Skills, and introduce no changes to the interface, configuration, save format or version number. See [the focused verification report](test-results/activity-update.txt) for the exact mapping and validation.

## Passive upgrades

Spend points in **Skill Upgrades**, using the category filters, mouse wheel, or page buttons. All 18 upgrades have a configurable cost, maximum rank, effect and enabled flag. Purchases cannot increase attributes.

| Category | Upgrades |
| --- | --- |
| Physical | Efficient Miner, Lumberjack, Heavy Lifter, Sure Grip |
| Mobility | Light Footed, Enduring Runner, Steady Aim |
| Survival | Iron Body, Natural Recovery, Tough Skin |
| Knowledge | Experienced Enchanter, Experienced Brewer, Careful Crafter |
| Nature | Green Thumb, Experienced Farmer, Animal Caretaker |
| Social | Merchant, Negotiator |

Sure Grip reduces effective underwater/airborne mining penalties. Enduring Runner recovers a small amount of sprint exhaustion. Steady Aim increases arrow damage. Careful Crafter may repair one point of held tool wear after an eligible craft. Animal Caretaker shortens newborn growth time. Negotiator may grant one bonus vanilla XP after a trade. Upgrades do not create active abilities, spells or classes.

**Reset Skills** asks for confirmation, refunds all spent points, removes upgrade effects, and preserves all XP and levels. Its vanilla-level cost and real-time cooldown are configurable. Defaults: free, once every five minutes.

## Configuration

- `<world>/serverconfig/medievalstatus-server.toml`: progression curves, multipliers, per-activity rewards, cooldowns, effect caps, upgrade costs/ranks/effects/availability, reset rules. Copy desired defaults into `defaultconfigs` for new worlds. Five points per character level is deliberately fixed to preserve the required accounting rule.
- `config/medievalstatus-client.toml`: colors, scale, activity visibility and animation intensity. The Settings tab changes the common display options.
- `config/medievalstatus-layout.json`: validated panel widths and spacing; created on first opening, with safe defaults for invalid data. Logical canvas 960 × 600, automatically fitted to the window. UI scale controls can shrink this fitted layout.

Server gameplay configuration is authoritative. Client preferences cannot change XP, balances or upgrade effects. Runtime server config changes invalidate derived bonus caches.

## Persistence and exploit controls

Versioned NBT is stored in Forge player persistent data and copied on death. Character data is updated when awards/purchases occur, saved with the player, and synchronized on login, respawn, dimension changes and at most once per second for ordinary activity. Purchase requests contain only an operation ID and are rate limited and validated on the server.

Placed and piston-moved block positions are stored per dimension in `medievalstatus_placed.dat`, so relogging/restarting does not bypass provenance. Fluid-generated stone is excluded. Successful removal is confirmed before rewards; planted mature crops remain eligible. Old pre-install player buildings cannot be distinguished from natural terrain. Trees grown from saplings are eligible; directly placed logs are not. Persistent placement storage grows with the number of still-tracked positions; temporary action queues and cooldown caches are bounded.

Combat XP requires successful damage to monsters and uses cooldowns plus per-monster lifetime budgets. Constitution uses surviving monster damage, its own per-monster budget and injury-limited recovery credit. PvP, self-harm, trivial damage, creative/spectator activity, stationary movement, riding and flight do not award XP. Crafting is throttled to one rewarded craft per minute. Brewing reward markers are consumed on manual extraction; automatic extraction does not award XP. No XP is granted for automated harvesting.

## Build and tests

Use a Java 17 JDK:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

On Linux/macOS: `bash ./gradlew build`. Output: `build/libs/medievalstatus-1.0.0.jar`.

`build` runs a dependency-free Java progression regression suite through `progressionTest`. Forge GameTests exercise NBT round-trips, death clone, real attribute modifiers, reset refunds, transaction rejection, six reward tracks, effect caps, planted crops and failed breaks. See [TESTING.md](TESTING.md) for recorded results and remaining manual checks. GameTests are only auto-registered in enabled development test runs.

## Scope and compatibility

Enchanting uses Minecraft's successful-enchantment statistic; Forge has no general post-enchantment player event in this target. It refunds at most one paid level per sampled success and never changes enchantment limits. Brewing extends consumed, non-instant potion effects, not brewing-stand internals. Trade adjustments are scoped to the active offer session and preserve vanilla price resets; costs remain at least one item.

The proficiency number on an attribute card is informational. The Active Bonuses panel reports actual capped effects. Activity counters are rewarded-action totals, not independent XP skill trees. No external font, image pack or mandatory dependency is bundled.

Original mod code and art: All Rights Reserved. The Gradle wrapper and Forge development-kit notices retain their original licenses in `LICENSE.txt` and `CREDITS.txt`.
