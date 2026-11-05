package dev.jax.jaxclient;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/** Client-side locator for ore and container blocks in already-loaded chunks. */
final class WorldScanner {
    private WorldScanner() {}

    record Found(String name, BlockPos pos, double distance) {}

    private static final Set<Block> ORES = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS
    );
    private static final Set<Block> CONTAINERS = Set.of(
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
            Blocks.SHULKER_BOX, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX
    );

    private static List<Found> ores = List.of();
    private static List<Found> containers = List.of();
    private static int ticks;
    private static final Set<Block> CUSTOM_BLOCKS = new HashSet<>();

    static void loadCustomBlocks(List<String> ids) {
        CUSTOM_BLOCKS.clear();
        if (ids == null) return;
        for (String raw : ids) {
            net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(raw);
            if (id != null && net.minecraft.registry.Registries.BLOCK.containsId(id)) {
                CUSTOM_BLOCKS.add(net.minecraft.registry.Registries.BLOCK.get(id));
            }
        }
    }
    static boolean addCustomBlock(Block block) {
        boolean added = CUSTOM_BLOCKS.add(block);
        if (added) saveCustomBlocks();
        return added;
    }
    static boolean removeCustomBlock(Block block) {
        boolean removed = CUSTOM_BLOCKS.remove(block);
        if (removed) saveCustomBlocks();
        return removed;
    }
    static void clearCustomBlocks() { CUSTOM_BLOCKS.clear(); saveCustomBlocks(); }
    private static void saveCustomBlocks() {
        Config cfg = JaxClient.config();
        cfg.customXrayBlocks = CUSTOM_BLOCKS.stream()
                .map(net.minecraft.registry.Registries.BLOCK::getId)
                .map(net.minecraft.util.Identifier::toString).sorted().toList();
        cfg.save();
    }
    static Set<Block> customBlocks() { return Set.copyOf(CUSTOM_BLOCKS); }

    static void tick(MinecraftClient mc) {
        if (++ticks % 10 != 0 || mc.player == null || mc.world == null) return;
        boolean findOres = Features.xray != null && Features.xray.isEnabled();
        boolean findContainers = Features.chestEsp != null && Features.chestEsp.isEnabled();
        if (!findOres && !findContainers) {
            ores = List.of();
            containers = List.of();
            return;
        }

        BlockPos origin = mc.player.getBlockPos();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        List<Found> oreHits = new ArrayList<>();
        List<Found> containerHits = new ArrayList<>();
        int horizontal = 24;
        int vertical = 16;
        for (int x = -horizontal; x <= horizontal; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    double distanceSq = (double)x*x + (double)y*y + (double)z*z;
                    if (distanceSq > horizontal * horizontal) continue;
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!mc.world.isChunkLoaded(cursor)) continue;
                    Block block = mc.world.getBlockState(cursor).getBlock();
                    if (findOres && (ORES.contains(block) || CUSTOM_BLOCKS.contains(block)) && oreHits.size() < 80) {
                        oreHits.add(new Found(label(block), cursor.toImmutable(), Math.sqrt(distanceSq)));
                    }
                    if (findContainers && CONTAINERS.contains(block) && containerHits.size() < 80) {
                        containerHits.add(new Found(label(block), cursor.toImmutable(), Math.sqrt(distanceSq)));
                    }
                }
            }
        }
        Comparator<Found> nearest = Comparator.comparingDouble(Found::distance);
        oreHits.sort(nearest);
        containerHits.sort(nearest);
        ores = List.copyOf(oreHits.subList(0, Math.min(8, oreHits.size())));
        containers = List.copyOf(containerHits.subList(0, Math.min(8, containerHits.size())));
    }

    private static String label(Block block) {
        String id = net.minecraft.registry.Registries.BLOCK.getId(block).getPath();
        return id.replace("deepslate_", "deep ").replace("_ore", " ore").replace('_', ' ');
    }

    static List<Found> ores() { return ores; }
    static List<Found> containers() { return containers; }
}
