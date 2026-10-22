# Configuration defaults

This is a transcription of the immutable original `Config.java`; the full captured source is `original-config.java.txt`. Paths, defaults and ranges are preserved in the target.

| Server key | Default | Allowed range |
|---|---:|---|
| `progression.attributeBase` | 100 | 1..100000 |
| `progression.attributeExponent` | 1.6 | 1..4 |
| `progression.overallBase` | 150 | 1..100000 |
| `progression.overallExponent` | 1.5 | 1..4 |
| `progression.attributeMultiplier` | 1 | 0..100 |
| `progression.overallMultiplier` | 1 | 0..100 |
| `progression.rewardMultiplier` | 1 | 0..100 |
| `progression.StrengthMultiplier` | 1 | 0..100 |
| `progression.DexterityMultiplier` | 1 | 0..100 |
| `progression.ConstitutionMultiplier` | 1 | 0..100 |
| `progression.IntelligenceMultiplier` | 1 | 0..100 |
| `progression.WisdomMultiplier` | 1 | 0..100 |
| `progression.CharismaMultiplier` | 1 | 0..100 |
| `limits.movementSampleTicks` | 40 | 20..200 |
| `limits.activityCooldownTicks` | 20 | 1..1200 |
| `limits.maxBonusHealth` | 20 | 0..100 |
| `limits.maxMovementFraction` | .25 | 0...5 |
| `limits.maxMiningFraction` | 1 | 0..5 |
| `limits.maxMeleeFraction` | .35 | 0..1 |
| `limits.maxTradeDiscount` | .25 | 0...5 |
| `limits.maxBonusArmor` | 5 | 0..10 |
| `limits.attributeEffectiveness` | 1 | 0..5 |
| `reset.enabled` | true | boolean |
| `reset.cooldownSeconds` | 300 | 0..86400 |
| `reset.vanillaLevelCost` | 0 | 0..100 |

Every `rewards.<name>` allows 0..1000:

| Name | Default | Attribute |
|---|---:|---|
| Mining | 2 | Strength |
| Woodcutting | 4 | Strength |
| Farming | 5 | Wisdom |
| Exploration | 2 | Dexterity |
| Archery | 6 | Dexterity |
| Endurance | 4 | Constitution |
| Enchanting | 18 | Intelligence |
| Brewing | 10 | Intelligence |
| Trading | 8 | Charisma |
| Melee | 5 | Strength |
| Breeding | 10 | Wisdom |
| Crafting | 1 | Intelligence |
| Recovery | 1 | Constitution |

Every `upgrades.<lowercase_enum>.cost` defaults to 1 (1..100); `maxRank` to 5 (1..100); `enabled` to true. Every `effectPerRank` permits 0..10. Ordinal IDs and default effects:

| ID | Enum/key | Effect per rank |
|---:|---|---:|
| 0 | EFFICIENT_MINER / efficient_miner | .04 |
| 1 | LUMBERJACK / lumberjack | .08 |
| 2 | HEAVY_LIFTER / heavy_lifter | .02 |
| 3 | SURE_GRIP / sure_grip | .12 |
| 4 | LIGHT_FOOTED / light_footed | .01 |
| 5 | ENDURING_RUNNER / enduring_runner | .02 |
| 6 | STEADY_AIM / steady_aim | .025 |
| 7 | IRON_BODY / iron_body | 1 heart |
| 8 | NATURAL_RECOVERY / natural_recovery | .2 |
| 9 | TOUGH_SKIN / tough_skin | .5 |
| 10 | EXPERIENCED_ENCHANTER / experienced_enchanter | .04 |
| 11 | EXPERIENCED_BREWER / experienced_brewer | .05 |
| 12 | CAREFUL_CRAFTER / careful_crafter | .10 |
| 13 | GREEN_THUMB / green_thumb | .025 |
| 14 | EXPERIENCED_FARMER / experienced_farmer | .10 |
| 15 | ANIMAL_CARETAKER / animal_caretaker | .05 |
| 16 | MERCHANT / merchant | .02 |
| 17 | NEGOTIATOR / negotiator | .04 |

| Client key | Default | Allowed range |
|---|---|---|
| `interfaceScale` | 1 | .5..2 |
| `animationIntensity` | .4 | 0..1 |
| `showActivities` | true | boolean |
| `goldColor` | `C6A665` | string |
| `stoneColor` | `171A1D` | string |

`medievalstatus-layout.json`: `leftWidth=224` (180..260), `activityWidth=280` (220..300), `gap=14` (8..20). Invalid/missing/null layout retains safe defaults. P opens the status screen. Titles, help text and controls remain in the source/UI port, not newly designed.

# Progression and rewards

`required(L,B,E) = max(1,min(1e100,floor(B*pow(max(1,L),E))))`.

`scale(L,C) = C*(1-exp(-max(0,L-1)/40.0))`.

`activityBonus(N) = floor(max(0,N)/50)*.025` using integer long division. Only 12 actions listed below affect bonuses; Recovery does not receive an activity-derived bonus.

Award ignores nonfinite/nonpositive input; per-action input caps at 1,000,000. Lifetime and current XP cap at 1e100. A call advances at most 4096 levels and never exceeds `Long.MAX_VALUE`. Each overall level grants exactly 5 points with saturating addition/multiplication; attribute levels grant no points. Purchase validates ID, positive cost/maximum, rank cap and available balance. Reset zeros purchased ranks and spent points only.

Action reward = configured reward * min(4,difficulty) * global reward multiplier. Attribute XP additionally multiplies attribute/global-track and named-attribute multipliers. Overall XP independently multiplies overall multiplier. Successful awards increment the named action counter with saturating addition; every 50th action invalidates the bonus cache even without leveling. Creative/spectator or invalid difficulty earns nothing.

# Bonus formulas

Let `S(index,cap)=min(cap,scale(attributeLevel,cap)*attributeEffectiveness)`; indexes 0..5 correspond to the six attributes in order. Let `U(enum)=enabled ? min(rank,configuredMaxRank)*configuredEffect : 0`; `A(name)=activityBonus(counter)`; `mine=S(0,.65)+U(EFFICIENT_MINER)`. All summands add before the final cap.

| Snapshot effect | Expression |
|---|---|
| Mining | min(miningCap, mine+A(Mining)) |
| Woodcutting | min(miningCap, mine+U(LUMBERJACK)+A(Woodcutting)) |
| Melee damage | min(damageCap, S(0,.25)+U(HEAVY_LIFTER)+A(Melee)) |
| Movement | min(moveCap, S(1,.15)+U(LIGHT_FOOTED)+A(Exploration)) |
| Attack speed | S(1,.12) |
| Bonus health | min(healthCap, S(2,12)+2*U(IRON_BODY)+actualBaseHealth*A(Endurance)) |
| Bonus armor | min(armorCap, S(2,2)+U(TOUGH_SKIN)) |
| Arrow damage | min(.35,S(1,.2)+U(STEADY_AIM)+A(Archery)) |
| Crop yield chance | min(.25,S(4,.15)+U(GREEN_THUMB)+A(Farming)) |
| Enchant refund chance | min(.35,S(3,.2)+U(EXPERIENCED_ENCHANTER)+A(Enchanting)) |
| Potion duration | min(.5,S(3,.2)+U(EXPERIENCED_BREWER)+A(Brewing)) |
| Trade discount | min(tradeCap,S(5,.15)+U(MERCHANT)+A(Trading)) |
| Recovery pulse | min(2,S(2,1)+U(NATURAL_RECOVERY)) |
| Other block mining | min(miningCap,mine) |
| Growth reduction | min(.4,U(ANIMAL_CARETAKER)+A(Breeding)) |
| Tool repair chance | min(.5,U(CAREFUL_CRAFTER)+A(Crafting)) |

Health/armor apply additive modifiers; movement/melee/attack speed multiply base. Refresh replaces the same modifier; it cannot stack or heal. Excess current health clamps down to maximum. Mining speed is originalSpeed*(1+min(miningCap,classifiedBonus)); logs use wood, pickaxe blocks use mining, other blocks use otherMining. Crops add min(.5,farmer skill); underwater/airborne adds min(.6,sure-grip skill).

# Action gates and secondary effects

* Break attempts queue at most 128 entries. Confirmed removal awards; unsuccessful breaks retain provenance. Mature crops earn Farming even when player planted. Natural logs earn Woodcutting; natural pickaxe blocks with hardness >=1 earn Mining, difficulty min(3,hardness/1.5). Placed/piston/fluid-generated blocks carry persistent protection.
* Hostile combat damage >=1 uses activity cooldown, difficulty min(3,damage/4), max credit twice target max health. Surviving hostile damage earns Endurance, min(3,damage/3), 200-tick cooldown, twice hostile max-health allowance. Recoverable damage caps at 10. Recovery rewards only within 1200 ticks of combat, at most min(1,restored amount), 100-tick cooldown.
* Brewing tags first three nonempty outputs with a unique token; taking a fresh potion clears matching inventory/carried tokens and rewards once, subject to action cooldown. Potion duration multiplier is 1+bonus, capped at 72000 ticks; instant effects excluded.
* Breeding uses action cooldown and queues at most 32 babies; negative newborn age multiplies by 1-growthReduction. Crafting uses 1200-tick cooldown, reward once, and may repair one held-tool damage point.
* Trading difficulty min(3,max(1,offerXP/5)); negotiator chance min(.25,skill) grants 1 vanilla XP. Discount floor(base count*tradeBonus) changes special price; prior changes restore only when still applied. Refresh revalidates result/payment; close respects a vanilla reset already performed.
* Sprint sampling requires same dimension, ground, no vehicle/flying/fall-flying, distance >=4 and <=sampleTicks*.7; difficulty min(2,distance/8). Every 20 ticks: enchant-stat deltas reward min(3,delta), possible one paid-level refund; sprint exhaustion recovers min(.1,runner skill); dirty bonuses/data refresh. Every 200 ticks: alive, food>=18, >=200 ticks since combat, below max health: heal recoveryPulse, exhaust recoveryPulse*.5.
* Reset requires alive noncreative/nonspectator player, enabled, elapsed deadline and sufficient vanilla levels; refund purchased points, consume configured vanilla levels, set deadline now+cooldownSeconds*1000. Packet rate gate is 5 game ticks. General cooldown map clears at 128 names.

Complete control-flow reference is `original-events.java.txt`; formulas above describe the original, not additional gameplay features.
