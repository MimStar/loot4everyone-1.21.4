package com.loot4everyone;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class StateSaverAndLoader extends PersistentState {

    public HashMap<UUID,PlayerData> players = new HashMap<>();
    public HashMap<BlockPos,ChestData> chests = new HashMap<>();
    public HashMap<BlockPos, ItemFrameData> itemframes = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup){
        NbtCompound playersNbt = new NbtCompound();
        players.forEach(((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putString("inventory",playerData.inventoryToString());
            playersNbt.put(uuid.toString(),playerNbt);
        }));
        nbt.put("players", playersNbt);
        NbtCompound chestsNbt = new NbtCompound();
        chests.forEach(((blockPos, chestData) -> {
            NbtCompound chestNbt = new NbtCompound();
            chestNbt.putString("chestdata",chestData.chestDataToString());
            String pos = blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
            chestsNbt.put(pos,chestNbt);
        }));
        nbt.put("chests", chestsNbt);
        NbtCompound itemframesNbt = new NbtCompound();
        itemframes.forEach((((blockPos, itemFrameData) -> {
            NbtCompound itemframeNbt = new NbtCompound();
            itemframeNbt.putString("itemframedata",itemFrameData.serializeToString());
            String pos = blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
            itemframesNbt.put(pos,itemframeNbt);
        })));
        nbt.put("itemframes", itemframesNbt);
        return nbt;
    }

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        NbtCompound playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();
            playerData.stringToInventory(playersNbt.getCompound(key).getString("inventory"));
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });
        NbtCompound chestsNbt = tag.getCompound("chests");
        chestsNbt.getKeys().forEach(key -> {
            ChestData chestData = new ChestData();
            chestData.stringToChestData(chestsNbt.getCompound(key).getString("chestdata"));
            String[] posParts = key.split(",");
            BlockPos pos = new BlockPos(Integer.parseInt(posParts[0]), Integer.parseInt(posParts[1]), Integer.parseInt(posParts[2]));
            state.chests.put(pos, chestData);
        });
        NbtCompound itemframesNbt = tag.getCompound("itemframes");
        itemframesNbt.getKeys().forEach(key -> {
            ItemFrameData itemFrameData = ItemFrameData.deserializeFromString(itemframesNbt.getCompound(key).getString("itemframedata"));
            String[] posParts = key.split(",");
            BlockPos pos = new BlockPos(Integer.parseInt(posParts[0]), Integer.parseInt(posParts[1]), Integer.parseInt(posParts[2]));
            state.itemframes.put(pos, itemFrameData);
        });
        return state;
    }

    private static final Type<StateSaverAndLoader> type = new Type<>(
            StateSaverAndLoader::new,
            StateSaverAndLoader::createFromNbt,
            null
    );

    public static StateSaverAndLoader getServerState(MinecraftServer server){
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();

        StateSaverAndLoader state = persistentStateManager.getOrCreate(type, Loot4Everyone.getModId());

        return state;
    }

    public static PlayerData getPlayerState(MinecraftServer server, LivingEntity player){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        return serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    public static ChestData getChestState(MinecraftServer server, BlockPos blockPos){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        return serverState.chests.computeIfAbsent(blockPos, blockPos1 -> new ChestData());
    }

    public static ItemFrameData getItemFrameState(MinecraftServer server, BlockPos blockPos){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        return serverState.itemframes.computeIfAbsent(blockPos, blockPos1 -> new ItemFrameData());
    }

    public static boolean isItemFrameStatePresent(MinecraftServer server, BlockPos blockPos){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        return serverState.itemframes.containsKey(blockPos);
    }

    public static boolean isBarrelStatePresent(MinecraftServer server, BlockPos blockPos){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        return serverState.chests.containsKey(blockPos);
    }

    public static BlockPos isChestStatePresent(MinecraftServer server, ChestBlockEntity chest){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        BlockPos blockPos = chest.getPos();

        if (serverState.chests.containsKey(blockPos)){
            return blockPos;
        }

        BlockState blockState = chest.getCachedState();
        if (blockState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE){
            Direction facing = blockState.get(ChestBlock.FACING);
            BlockPos otherPos = blockPos.offset(blockState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
            if (serverState.chests.containsKey(otherPos)){
                return otherPos;
            }
        }

        return null;
    }

    public static boolean isBarrelStatePresentInPlayerState(MinecraftServer server, LivingEntity player, BlockPos blockPos){
        PlayerData playerState = getPlayerState(server, player);
        return playerState.getInventory().containsKey(blockPos);
    }

    public static BlockPos isChestStatePresentInPlayerState(MinecraftServer server, LivingEntity player, ChestBlockEntity chest){
        PlayerData playerState = getPlayerState(server, player);
        BlockPos blockPos = chest.getPos();

        if (playerState.getInventory().containsKey(blockPos)){
            return blockPos;
        }

        BlockState blockState = chest.getCachedState();
        if (blockState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE){
            Direction facing = blockState.get(ChestBlock.FACING);
            BlockPos otherPos = blockPos.offset(blockState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
            if(playerState.getInventory().containsKey(otherPos)){
                return otherPos;
            }
        }

        return null;
    }

    public static void saveState(MinecraftServer server) {
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        serverState.markDirty();
    }
}
