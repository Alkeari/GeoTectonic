# GeoTectonic

**GeoTectonic** is a world generation overhaul for Minecraft 1.20.1 that completely replaces the vanilla cave system with four geologically distinct underground environments. Every cave type is rare, purposefully shaped, and decorated to feel like a real place rather than a procedurally punched void.

Vanilla caves — spaghetti tunnels, noodle caves, cheese voids, canyon ravines, and mineshaft-depth networks — are fully removed. What replaces them is rarer, larger, and more deliberate.

---

## Cave Types

### Fracture Caves

Tectonic fractures are the mod's defining feature. A fracture begins at the surface as a narrow crack — a slot canyon entry on flat ground, or a gash in a cliff face on steep slopes — and descends continuously through the earth down to Y −60. As depth increases, the passage widens. At mid-depth the trunk splits into 2 to 4 branches that diverge outward and downward, each continuing to widen independently.

**Size:** Entry passages begin at roughly 3×7 blocks (width × height). By the deep zone they reach up to 18×17 blocks.

**Depth:** Surface to Y −60.

**Decoration:** The deeper the fracture, the more volcanic it becomes. Basalt appears on walls with increasing frequency toward the bottom. Magma blocks line the floors of the deep zone. Dripstone forms on ceilings in the hot zone. Lava pools collect at the lowest points, and occasional lava falls cascade down the walls.

**Biomes:** All overworld biomes.

**Rarity:** Very rare. Roughly 0.07% per chunk — expect openings spaced hundreds of blocks apart.

---

### Karst Chambers

Karst systems are warm-biome cave networks formed around a main chamber with satellite rooms connected by narrow passages. They are found only where the climate is warm to hot — deserts, savannas, jungles, badlands, and similar biomes. Cold and temperate biomes do not generate karst.

The layout is structured: a diagonal entry crack descends from around Y 40 down to the hub chamber, which sits between Y 10 and Y 25. From the hub, 2 to 4 satellite chambers branch outward, each 15 to 30 blocks away, connected by tight tunnels. A shallow water pool forms at the lowest point of the hub floor.

**Size:** The hub chamber is between 13 and 30 blocks wide. Satellite rooms are 6 to 12 blocks wide.

**Depth:** Hub at Y 10–25. Entry crack begins around Y 40.

**Decoration:** Stalactites hang from ceilings throughout. Cave vines drape downward in patches. Stalagmites rise from dry floors. Calcite veins streak the walls. The central water pool reflects it all.

**Biomes:** Warm and hot biomes only (biome temperature 0.2–1.5).

**Rarity:** Uncommon. Roughly 0.15% per chunk — more common than fractures but still meaningful to find.

---

### Cenotes

Cenotes are water-filled sinkholes found in warm climates. From the surface they appear as a shaft descending into the earth — the mouth is narrow, with walls that undulate slightly as they widen downward. Below the waterline, the shaft opens into a multi-lobe chamber: a cluster of overlapping ovoid caverns that together form an irregular flooded vault.

The entire lower chamber is submerged. The shaft above water level is open air, carved with subtle variation so the walls are not perfectly smooth.

**Size:** The chamber complex spans 16 to 30 blocks across. Individual lobes are 8 to 18 blocks wide. The surface shaft is 3 to 8 blocks wide at the mouth.

**Depth:** Chamber sits at Y 20–40. The shaft rises from there to the surface.

**Decoration:** Stalactites hang from dry ceiling sections above the waterline. Calcite patches mark the chamber walls. Clay covers the submerged floors.

**Biomes:** Warm and hot biomes only (biome temperature 0.5–1.5).

**Rarity:** Rare. Roughly 0.05% per chunk.

---

### Erosion Caves

Erosion caves are shallow, winding passages carved by flowing water near sea level. They are the only cave type that always contains an active water channel — a stream runs continuously through the floor of every erosion passage. The passages meander across the terrain with smooth curves and no sharp turns, following an unpredictable but naturally flowing path.

**Size:** 3.6 to 6 blocks wide, 3.6 to 5 blocks tall. The water channel occupies the lower portion of the floor.

**Depth:** Y 35–60. These caves do not go deep.

**Length:** Each passage runs 60 to 120 blocks before terminating.

**Decoration:** Moss covers ceiling sections. Hanging roots drape downward in clusters. Clay lines dry portions of the floor. Gravel beds form beneath the water channel.

**Biomes:** All overworld biomes.

**Rarity:** Uncommon. Roughly 0.08% per chunk.

---

## What Is Removed

GeoTectonic overrides all vanilla overworld cave density functions, disabling:

- Spaghetti tunnels
- Noodle caves
- Cheese caves (underground terrain voids)
- Cave entrances
- Pillars
- Canyons and ravines (underground portion)

The surface remains intact. Overworld terrain, biomes, structures, and ores are unaffected. The underground is simply empty except for GeoTectonic's cave systems.

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

## License

[Alkeari License Agreement](https://github.com/Alkeari/GeoTectonic/blob/main/LICENSE.md)
