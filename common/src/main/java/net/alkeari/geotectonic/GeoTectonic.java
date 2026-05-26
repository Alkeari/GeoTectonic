package net.alkeari.geotectonic;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import net.alkeari.geotectonic.command.GeoTectonicCommands;
import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.alkeari.geotectonic.registry.GeoTectonicCarvers;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoTectonic {
    public static final String MOD_ID = "geotectonic";
    public static final Logger LOGGER = LoggerFactory.getLogger("GeoTectonic");

    public static void init() {
        GeoTectonicBlocks.init();
        GeoTectonicCarvers.init();
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
            GeoTectonicCommands.register(dispatcher, registryAccess));
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "cave_entered"),
            (buf, ctx) -> {
                String caveType = buf.readUtf();
                ctx.queue(() -> {
                    if (ctx.getPlayer() instanceof ServerPlayer player) {
                        grantAdvancement(player, "root");
                        grantAdvancement(player, caveType);
                    }
                });
            }
        );
        LOGGER.info("{} loaded.", MOD_ID);
    }

    private static void grantAdvancement(ServerPlayer player, String name) {
        AdvancementHolder adv = player.server.getAdvancements()
            .get(ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
        if (adv != null) {
            player.getAdvancements().award(adv, "entered");
        }
    }
}
