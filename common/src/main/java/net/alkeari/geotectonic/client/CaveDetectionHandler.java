package net.alkeari.geotectonic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.alkeari.geotectonic.config.ModConfig;
import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CaveDetectionHandler {

    private static final int POLL_TICKS = 40;  // 2 seconds
    private static final int EXIT_POLLS = 3;   // consecutive null polls to exit cave state

    // State machine
    private static int    tickTimer      = 0;
    private static String lockedCaveType = null;  // null = player is outside
    private static int    outsideCount   = 0;     // consecutive null detections while locked

    // Sound
    private static SimpleSoundInstance currentSoundInstance  = null;
    private static String              currentSoundCaveType  = null;

    // Advancements
    private static final java.util.Set<String> awardedThisSession = new java.util.HashSet<>();

    public static void onClientTick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (++tickTimer < POLL_TICKS) return;
        tickTimer = 0;

        BlockPos playerPos = client.player.blockPosition();
        String detected = detectCaveType(client.level, playerPos);

        if (lockedCaveType == null) {
            // OUTSIDE state
            if (detected != null) {
                // Entering a cave — lock type, show subtitle once, start sound
                lockedCaveType = detected;
                outsideCount   = 0;
                if (ModConfig.isDiscoverySubtitleEnabled()) showSubtitle(client, detected);
                if (ModConfig.isAdvancementsEnabled()) sendAdvancementPacket(detected);
                if (ModConfig.isAmbientSoundsEnabled()) startAmbientSound(client, detected);
            }
        } else {
            // INSIDE state — type is locked
            if (detected == null) {
                outsideCount++;
                if (outsideCount >= EXIT_POLLS) {
                    // Consistently outside — exit cave
                    lockedCaveType = null;
                    outsideCount   = 0;
                    stopAmbientSound(client);
                }
            } else {
                outsideCount = 0;
                // Ignore detected type — keep locked type; do NOT show subtitle again
            }
        }
    }

    // --- Subtitle ---

    private static void showSubtitle(Minecraft client, String caveType) {
        client.gui.setTimes(10, 70, 20);
        client.gui.setTitle(Component.empty());
        client.gui.setSubtitle(Component.translatable("subtitle.geotectonic." + caveType));
    }

    // --- Advancement packet ---

    private static void sendAdvancementPacket(String caveType) {
        if (awardedThisSession.add(caveType)) {
            io.netty.buffer.ByteBuf rawBuf = io.netty.buffer.Unpooled.buffer();
            net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(rawBuf);
            buf.writeUtf(caveType);
            dev.architectury.networking.NetworkManager.sendToServer(
                new ResourceLocation("geotectonic", "cave_entered"), buf
            );
        }
    }

    // --- Ambient sound ---

    private static void startAmbientSound(Minecraft client, String caveType) {
        stopAmbientSound(client);
        SoundEvent sound = getSoundForCave(caveType);
        if (sound != null) {
            float volume = caveType.equals("erosion") ? 0.15f : 1.0f;
            currentSoundInstance = new SimpleSoundInstance(
                sound.getLocation(), SoundSource.AMBIENT, volume, 1.0f,
                RandomSource.create(), true, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true
            );
            client.getSoundManager().play(currentSoundInstance);
            currentSoundCaveType = caveType;
        }
    }

    private static void stopAmbientSound(Minecraft client) {
        if (currentSoundInstance != null) {
            client.getSoundManager().stop(currentSoundInstance);
            currentSoundInstance = null;
            currentSoundCaveType = null;
        }
    }

    private static SoundEvent getSoundForCave(String caveType) {
        return switch (caveType) {
            case "fracture" -> SoundEvents.AMBIENT_BASALT_DELTAS_LOOP.value();
            case "karst"    -> SoundEvent.createFixedRangeEvent(
                                   new ResourceLocation("minecraft", "ambient.dripstone_caves.loop"), 16.0f);
            case "cenote"   -> SoundEvents.AMBIENT_UNDERWATER_LOOP;
            case "erosion"       -> SoundEvents.WEATHER_RAIN;
            case "sea_cave"      -> SoundEvents.AMBIENT_UNDERWATER_LOOP;
            case "crystal_vein"  -> SoundEvent.createFixedRangeEvent(
                                       new ResourceLocation("minecraft", "ambient.dripstone_caves.loop"), 16.0f);
            case "volcanic_vent" -> SoundEvents.AMBIENT_BASALT_DELTAS_LOOP.value();
            default              -> null;
        };
    }

    // --- Detection ---

    private static String detectCaveType(Level level, BlockPos pos) {
        BlockState at = level.getBlockState(pos);
        if (at.is(GeoTectonicBlocks.FRACTURE_AIR.get()))      return "fracture";
        if (at.is(GeoTectonicBlocks.KARST_AIR.get()))         return "karst";
        if (at.is(GeoTectonicBlocks.CENOTE_AIR.get()))        return "cenote";
        if (at.is(GeoTectonicBlocks.EROSION_AIR.get()))       return "erosion";
        if (at.is(GeoTectonicBlocks.SEA_CAVE_AIR.get()))      return "sea_cave";
        if (at.is(GeoTectonicBlocks.CRYSTAL_VEIN_AIR.get()))  return "crystal_vein";
        if (at.is(GeoTectonicBlocks.VOLCANIC_VENT_AIR.get())) return "volcanic_vent";
        if (at.is(Blocks.WATER)) {
            // Cenote wet zone: player submerged in the flooded chamber
            if (hasNearby(level, pos, 8, GeoTectonicBlocks.CENOTE_AIR.get()))
                return "cenote";
            // Sea cave flooded passage: ceiling rows are SEA_CAVE_AIR within ~2-3 blocks above
            if (hasNearby(level, pos, 8, GeoTectonicBlocks.SEA_CAVE_AIR.get()))
                return "sea_cave";
        }
        return null;
    }

    private static boolean hasNearby(Level level, BlockPos center, int radius,
                                      net.minecraft.world.level.block.Block... blocks) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(mutable);
                    for (net.minecraft.world.level.block.Block block : blocks) {
                        if (state.is(block)) return true;
                    }
                }
            }
        }
        return false;
    }
}
