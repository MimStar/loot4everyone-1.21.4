package com.loot4everyone;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BarrelViewers {
    private static final Map<BlockPos, Set<PlayerEntity>> viewers = new HashMap<>();

    public static void addViewer(PlayerEntity player, BlockPos pos) {
        viewers.computeIfAbsent(pos, k -> new HashSet<>()).add(player);
    }

    public static void removeViewer(PlayerEntity player) {
        viewers.values().forEach(set -> set.remove(player));
    }

    public static int getViewerCount(BlockPos pos) {
        return viewers.getOrDefault(pos, Collections.emptySet()).size();
    }
}
