package net.alkeari.geotectonic.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

public record AddCarverBiomeModifier(
    HolderSet<Biome> biomes,
    HolderSet<ConfiguredWorldCarver<?>> carvers
) implements BiomeModifier {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<AddCarverBiomeModifier> CODEC = RecordCodecBuilder.create(builder -> builder.group(
        Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddCarverBiomeModifier::biomes),
        ((Codec<HolderSet<ConfiguredWorldCarver<?>>>) (Codec) RegistryCodecs.homogeneousList(Registries.CONFIGURED_CARVER))
            .fieldOf("carvers").forGetter(AddCarverBiomeModifier::carvers)
    ).apply(builder, AddCarverBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome)) {
            carvers.forEach(carver ->
                builder.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).add(carver));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return GeoTectonicForge.ADD_CARVER_MODIFIER_CODEC.get();
    }
}
