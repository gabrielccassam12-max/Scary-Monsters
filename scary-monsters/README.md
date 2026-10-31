# Scary Monsters - JoJo's Bizarre Adventure: Steel Ball Run (Fabric 1.21.1)

Diego Brando's dinosaur power. Hold the **Scary Monsters** item and you visibly turn into a dino-man
(tail, snout, claws, spikes, scales) in 4 Forms, with 16 attacks and raptor / T-Rex allies.
Everyone on the server sees your transformation.

## Build (GitHub, no installs)
1. Create a GitHub repo, upload the CONTENTS of this folder (not the folder itself).
2. Make sure `.github/workflows/build.yml` exists (create it in the GitHub UI if the upload skipped hidden folders).
3. Actions tab -> wait for the green check -> Artifacts -> download `scarymonsters` -> use `scarymonsters-1.0.0.jar`.
Or locally: JDK 21 + `gradle build`.

Install with Fabric Loader 1.21.1 + Fabric API 0.105.0+1.21.1 (it can live next to the Tusk mod).

## Controls
Right click = use attack | **V** = switch Form | **B** = switch attack (all rebindable)

## Forms (unlock by XP level: 5 / 15 / 30; creative has all)
**1 Raptor:** Claw Rake, Pounce (landing smash), Tail Whip, Bite (heals)
**2 Predator:** Raptor Rush, Dino Roar (monsters flee), **Dinosaurize** (turns the mob you look at into an allied raptor), Camouflage
**3 Tyrant:** Tyrant Bite, Stomp, Pack Call (3 raptors), Fossilize (target turns to stone)
**4 Primordial:** Meteor Storm, Extinction Aura, Rampage, Dino Army (4 raptors + Rex)

Passive while holding it: Speed/Jump/Strength/Resistance scaling with the Form, and a fear aura that weakens
nearby monsters (config flags in `ScaryMod.java`: `PASSIVE_BUFFS`, `UNLOCK_ALL`, `BREAK_BLOCKS`, `MINION_LIFETIME`).
Forms 3-4 need food level > 6. Meteors do not break blocks unless `BREAK_BLOCKS = true`; they can hurt YOU if you stand in them.
Dinosaurize permanently removes the original mob (the dinosaur lasts 45 s); it refuses bosses, animals, pets and villagers.

## Models
`tools/generate_assets.py` is the single source for the 3 models (gear, raptor, rex): it writes the textures AND the
generated Java geometry (`GearParts`, `RaptorParts`, `RexParts`). Edit it and run `python3 tools/generate_assets.py` (needs Pillow).
Animation lives in `DinoGear.java` (tail/jaw poses per attack) and `DinoMinionModel.java` (walk cycle, jaw snap).

## Not compiled by the author
Syntax-checked only. Yarn 1.21.1+build.3. If Gradle reports a name error it is a mapping detail on that line.
The gear is drawn in third person only (not on your own first-person hands).
