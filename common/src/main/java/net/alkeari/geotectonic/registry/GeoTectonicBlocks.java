package net.alkeari.geotectonic.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.alkeari.geotectonic.GeoTectonic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class GeoTectonicBlocks {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(GeoTectonic.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> FRACTURE_AIR =
        BLOCKS.register("fracture_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static final RegistrySupplier<Block> KARST_AIR =
        BLOCKS.register("karst_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static final RegistrySupplier<Block> CENOTE_AIR =
        BLOCKS.register("cenote_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static final RegistrySupplier<Block> EROSION_AIR =
        BLOCKS.register("erosion_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static final RegistrySupplier<Block> SEA_CAVE_AIR =
        BLOCKS.register("sea_cave_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static final RegistrySupplier<Block> CRYSTAL_VEIN_AIR =
        BLOCKS.register("crystal_vein_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static final RegistrySupplier<Block> VOLCANIC_VENT_AIR =
        BLOCKS.register("volcanic_vent_air", () -> new AirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()));

    public static Block forCaveType(String name) {
        return switch (name) {
            case "fracture"      -> FRACTURE_AIR.get();
            case "karst"         -> KARST_AIR.get();
            case "cenote"        -> CENOTE_AIR.get();
            case "erosion"       -> EROSION_AIR.get();
            case "sea_cave"      -> SEA_CAVE_AIR.get();
            case "crystal_vein"  -> CRYSTAL_VEIN_AIR.get();
            case "volcanic_vent" -> VOLCANIC_VENT_AIR.get();
            default -> throw new IllegalArgumentException("Unknown cave type: " + name);
        };
    }

    public static void init() {
        BLOCKS.register();
    }
}
