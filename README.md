# GeoTectonic

**Vanilla cave spam, gone. Seven hand-designed cave systems take its place — each one rare enough that finding it feels like a discovery, not a chore.**

GeoTectonic strips out Minecraft's entire cave generation system and replaces it with seven geologically distinct underground environments. No more endless spaghetti tunnels under every block of stone. No more cheese voids swallowing your strip mines. The underground is mostly solid rock, the way it used to be — punctuated by the kind of caves you actually remember finding.

---

## Why GeoTectonic

- **Strip mining works again.** The deep underground is dominated by solid stone, with rare cave intrusions instead of constant interruption.
- **Caves you'll remember.** Each of the seven cave types has its own geometry, depth range, and decoration palette. When you find one, you know which one.
- **Geologically grounded.** Every cave maps to a real underground feature — tectonic fractures, karst dissolution, hydrothermal vents, mineralized faults. The world feels coherent rather than randomly generated.
- **Tunable.** Every cave's spawn probability and on/off state is exposed in a config file. Live-reload on Fabric, hot-reload via `/reload` on Forge.
- **Strip-mining friendly by default**, but configurable in either direction — bump probabilities for a richer underground, or push them down for a near-empty world dominated by a handful of dramatic finds.

---

## The Seven Caves

### Tectonic Fracture

The mod's defining feature. A fracture begins at the surface as a narrow crack — a slot canyon entry on flat ground, or a gash in a cliff face on steep slopes — and descends continuously through the earth to bedrock-adjacent depth. As depth increases, the passage widens dramatically. At mid-depth the trunk splits into branches that diverge outward and downward.

Decoration shifts with depth: sparse basalt near the surface, graduating to magma blocks and dripstone in the warm zone, then lava pools and cascading lava falls in the deep volcanic vault.

**Depth:** Surface to bedrock-adjacent · **Biomes:** All overworld · **Rarity:** Rare

---

### Karst Chamber

Warm-biome cave networks formed around a main vaulted chamber with satellite rooms connected by narrow passages. A diagonal entry crack descends from the surface to the hub chamber at mid-depth. From the hub, 2 to 4 satellite chambers branch outward, each connected by tight tunnels. A shallow water pool collects at the lowest point of the hub floor.

Stalactites hang from the ceilings, stalagmites rise from dry floors, and calcite veins streak the walls.

**Depth:** Shallow to mid · **Biomes:** Warm and hot only · **Rarity:** Uncommon

---

### Cenote

Water-filled sinkholes found in warm climates. From the surface, a cenote appears as a shaft descending into the earth — the mouth is narrow, with walls that undulate slightly as they widen downward. Below the waterline, the shaft opens into a multi-lobe chamber: a cluster of overlapping ovoid caverns forming an irregular flooded vault.

Glow berries cling to dry ceilings above the waterline, casting light down toward the surface. A calcite rim marks where the shaft meets the water. Seagrass carpets the submerged floor.

**Depth:** Shallow chamber, shaft to surface · **Biomes:** Warm and hot only · **Rarity:** Rare

---

### Erosion Cave

Shallow, winding passages carved by flowing water near sea level. These are the only caves that always contain an active water channel — a stream runs continuously through the floor of every erosion passage. The path meanders with smooth curves, following an unpredictable but naturally flowing route. At one point along each cave, the passage widens into a pool chamber where the water broadens and deepens.

Moss carpets the ceilings, hanging roots drape in clusters, clay lines the dry portions of the floor, and gravel beds form beneath the water channel.

**Depth:** Near sea level · **Biomes:** All overworld · **Rarity:** Uncommon

---

### Sea Cave

Flooded passages forming just above and below sea level along ocean-adjacent terrain. They begin at the seafloor or a coastal cliff face and carve inward through solid ground. The passage volume is almost entirely submerged — air pockets are rare.

Kelp and seagrass blanket the floor. Sea lanterns and prismarine blocks appear on walls exposed to open water, casting a cold glow through the flooded interior.

**Depth:** Sea level zone · **Biomes:** All overworld · **Rarity:** Uncommon

---

### Crystal Vein

Mineralized faults at deep-slate depth. Narrow, roughly straight galleries follow a single compass bearing through the rock, suggesting an ancient fault line where mineral-rich fluid once flowed. The walls expose a higher-than-ambient concentration of ores: iron, gold, copper, and redstone throughout the passage. An alcove at the far end reliably contains one or two diamond exposures and amethyst clusters growing from the walls.

Calcite seams streak the passage walls. The floor is strewn with gravel and cobbled deepslate. Cobwebs gather near the cave-in wall that seals the opposite end.

**Depth:** Deep · **Biomes:** All overworld · **Rarity:** Uncommon

---

### Volcanic Vent

Deep hydrothermal structures anchored well below the deepslate layer. A magma chamber — a wide, low vault — sits at the base, its floor paved with magma blocks and its ceiling lined with basalt. A narrow chimney rises from the chamber through the rock above, terminating in a small dead-end pocket.

A single lava source pools at the centre of the chamber floor. Soul fire clings to basalt outcrops on the walls. The chimney and top pocket are faced in blackstone and basalt.

**Depth:** Deep · **Biomes:** All overworld · **Rarity:** Rare

---

## What Is Removed

GeoTectonic overrides all vanilla overworld cave density functions, disabling:

- Spaghetti tunnels
- Noodle caves
- Cheese caves (underground terrain voids)
- Cave entrances
- Pillars
- Canyons and ravines (underground portion)

Surface terrain, biomes, structures, and ores are unaffected. Vanilla mineshafts still generate normally. The underground is simply empty except for GeoTectonic's seven cave systems.

---

## Configuration

GeoTectonic generates a config file at `config/geotectonic.toml` on first launch. The file is split into a `[probabilities]` section (per-cave spawn rates) and an `[enabled]` section (per-cave on/off toggles), plus a `[ui]` section controlling discovery subtitles, advancements, and ambient sounds.

Probabilities are per-chunk floats — lower values are rarer caves, higher values are more frequent. The file includes inline rarity guidance for tuning.

On **Forge**, the config is managed by ForgeConfigSpec and reloads via `/reload`. On **Fabric**, the file is watched for changes every two seconds and reloads automatically. Changes apply to newly generated chunks; already-explored areas keep the caves they have.

---

## Compatibility

GeoTectonic ships with override layers for several popular world-gen mods, including Tectonic, Terralith, and Overworldify. Caves spawn correctly alongside these terrain mods.

Vanilla mineshafts are left intact. GeoTectonic does not modify structures, ore distribution, or biome generation — only caves.

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft | 1.20.1 |
| Architectury API | 9.2.14+ |
| Forge *(if using Forge)* | 47.4.0+ |
| Fabric Loader *(if using Fabric)* | 0.14.23+ |
| Fabric API *(if using Fabric)* | 0.90.4+1.20.1+ |

---

## Commands

Diagnostic commands for world inspection and development. All require OP level 4.

**`/geotectonic find <type> [radius]`** — Teleport to the nearest cave of the given type. Default radius 200 chunks, maximum 500. Valid types: `fracture`, `karst`, `cenote`, `erosion`, `sea_cave`, `crystal_vein`, `volcanic_vent`.

**`/geotectonic list`** — Print all cave types with their current spawn probability, depth range, and biome restrictions. Disabled types show a `[DISABLED]` suffix.

**`/geotectonic stats [radius]`** — Count how many of each cave type exist within the given chunk radius. Default 50, maximum 200.

**`/geotectonic help`** — Show all commands with descriptions.

---

## License

[Alkeari License Agreement](https://github.com/Alkeari/GeoTectonic/blob/main/LICENSE.md)