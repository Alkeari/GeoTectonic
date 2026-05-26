package net.alkeari.geotectonic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.alkeari.geotectonic.registry.GeoTectonicBlocks;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.minecraft.commands.Commands;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GeoTectonicCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("geotectonic")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("find")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        CaveFinder.CAVES.keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> executeFind(ctx, 200))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 500))
                        .executes(ctx -> executeFind(ctx,
                            IntegerArgumentType.getInteger(ctx, "radius"))))))
            .then(Commands.literal("list")
                .executes(GeoTectonicCommands::executeList))
            .then(Commands.literal("stats")
                .executes(ctx -> executeStats(ctx, 50))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 200))
                    .executes(ctx -> executeStats(ctx,
                        IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("help")
                .executes(GeoTectonicCommands::executeHelp)));
    }

    private static int executeFind(CommandContext<CommandSourceStack> ctx, int radius) {
        String type = StringArgumentType.getString(ctx, "type");
        CaveFinder.CaveType cave = CaveFinder.CAVES.get(type);
        if (cave == null) {
            ctx.getSource().sendFailure(
                Component.literal("[GeoTectonic] Unknown cave type: " + type +
                    ". Valid types: " + String.join(", ", CaveFinder.CAVES.keySet())));
            return 0;
        }

        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = ctx.getSource().getPosition();
        int playerCX = (int) Math.floor(pos.x) >> 4;
        int playerCZ = (int) Math.floor(pos.z) >> 4;

        Optional<BlockPos> found = CaveFinder.findNearest(level, playerCX, playerCZ, cave, radius,
            GeoTectonicBlocks.forCaveType(type));
        if (found.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(
                "[GeoTectonic] No " + type + " cave found within " + radius + " chunks."));
            return 0;
        }

        BlockPos tpPos = found.get();
        int dx = (tpPos.getX() >> 4) - playerCX;
        int dz = (tpPos.getZ() >> 4) - playerCZ;
        int blockDist = (int) Math.sqrt((double)(dx * dx + dz * dz)) * 16;

        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            player.teleportTo(level, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "[GeoTectonic] Found " + type + " cave at " + tpPos.toShortString() +
            " — " + blockDist + " blocks away."), false);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("[GeoTectonic] Cave types:\n");
        for (CaveFinder.CaveType c : CaveFinder.CAVE_LIST) {
            boolean enabled = CaveFinder.isEnabled(c.name());
            float prob = CaveFinder.configProbability(c.name());
            sb.append(String.format("  %-10s prob=%.4f  range=%d  teleportY=%-4d  biomes=%s%s%n",
                c.name(), prob, c.range(), c.teleportY(), c.biomeNote(),
                enabled ? "" : "  [DISABLED]"));
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }

    private static int executeStats(CommandContext<CommandSourceStack> ctx, int radius) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = ctx.getSource().getPosition();
        int playerCX = (int) Math.floor(pos.x) >> 4;
        int playerCZ = (int) Math.floor(pos.z) >> 4;

        Map<String, Integer> counts = CaveFinder.countAll(level, playerCX, playerCZ, radius);
        StringBuilder sb = new StringBuilder(
            "[GeoTectonic] In " + radius + "-chunk radius:");
        counts.forEach((name, count) ->
            sb.append("  ").append(name).append(" x").append(count));

        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
            "[GeoTectonic] Commands (OP level 4 required):\n" +
            "  /geotectonic find <type> [radius]  — TP to nearest cave (default radius: 200 chunks)\n" +
            "  /geotectonic list                  — Show all cave types and properties\n" +
            "  /geotectonic stats [radius]         — Count caves in radius (default: 50 chunks)\n" +
            "  /geotectonic help                   — Show this message\n" +
            "Types: fracture, karst, cenote, erosion"), false);
        return 1;
    }
}
