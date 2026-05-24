# GeoTectonic

A world generation mod for Minecraft 1.20.1 that replaces the vanilla cave system with geologically realistic underground structures driven by simulated erosion, lithological filtering, and tectonic fracturing.

**Supports:** Forge 47.4.x · Fabric 0.14.x  
**Requires:** No dependencies

---

## What It Does

GeoTectonic suppresses all vanilla cave generation and replaces it with surface-seeded cave systems that behave more like real geology:

- **Surface-connected only** — caves only exist where they open to the surface; no floating underground voids
- **Biome-influenced rarity** — cave opening frequency varies by biome (jungles more, badlands less)
- **Lithological filtering** — cave shapes respond to rock hardness; resistant rock narrows passages, impenetrable rock blocks them entirely
- **Tectonic fractures** — rare deep-reaching fracture caves descend from surface fissures to Y=−60, branching at depth into 2–3 sub-passages
- **Volcanic decoration** — fracture caves below Y=0 gain volcanic features

Cave systems are fully deterministic and seed-stable.

---

## Cave Types

### Plumber Caves (Phase 1)
Surface-seeded downward walks with two morphology profiles:
- **Capillary** — narrow, winding passages with high sinuosity
- **Artesian** — straighter, deeper descents with larger cross-sections and potential aquifer fill

### Tectonic Fractures (Phase 2)
Very rare (~1 opening per ~750 block radius). Descend from surface fissures on flat ground or cliff-face openings on slopes. Scale from slot-canyon entries to wide vaults at depth. Compatible with terrain-raising mods (Tectonic, etc.).

---

## Compatibility

- Vanilla caves are fully suppressed via datapack density-function overrides — no conflicts with mods that read vanilla cave density functions
- Tectonic / terrain-raising mods: fracture carver queries actual surface height, so cave entry points are always correct regardless of terrain elevation
- No chunk-loading side effects — generation is deterministic from source chunk coordinates only

---

## Building

```
./gradlew build
```

Output jars: `forge/build/libs/` and `fabric/build/libs/`

---

## License

See [LICENSE.md](LICENSE.md).

**Author:** Alkeari
