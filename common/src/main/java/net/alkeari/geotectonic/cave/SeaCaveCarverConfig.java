package net.alkeari.geotectonic.cave;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarverDebugSettings;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;

public class SeaCaveCarverConfig extends CarverConfiguration {

    public static final Codec<SeaCaveCarverConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.floatRange(0.0f, 1.0f).fieldOf("probability").forGetter(c -> c.probability)
        ).apply(instance, SeaCaveCarverConfig::new)
    );

    public SeaCaveCarverConfig(float probability) {
        super(
            probability,
            ConstantHeight.of(VerticalAnchor.absolute(64)),
            ConstantFloat.of(1.0f),
            VerticalAnchor.absolute(-64),
            CarverDebugSettings.DEFAULT,
            HolderSet.direct()
        );
    }
}
