package com.loot4everyone;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.advancement.criterion.PlayerHurtEntityCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.nimbus.State;
import java.util.List;
import java.util.Objects;

public class Loot4Everyone implements ModInitializer {
	public static final String MOD_ID = "loot4everyone";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer server = null;

	public static String getModId() {
		return MOD_ID;
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			Loot4Everyone.server = server;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
			if (blockEntity instanceof ChestBlockEntity chest){
				int number_of_players = ChestBlockEntity.getPlayersLookingInChestCount(world,hitResult.getBlockPos());
				if (number_of_players > 0 && (chest.getLootTableSeed() != 0 || StateSaverAndLoader.isChestStatePresent(server,chest.getPos()) || StateSaverAndLoader.isItemFrameStatePresent(server,chest.getPos()))){
					return ActionResult.CONSUME;
				}
				if (chest.getLootTableSeed() != 0 && chest.getLootTable() != null && !StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,chest.getPos())){
					ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, chest.getPos());
					chestData.setLootTable(chest.getLootTable());
					chestData.setLootTableSeed(chest.getLootTableSeed());
					chest.setLootTable(null);
					chest.setLootTableSeed(0);
					StateSaverAndLoader.saveState(Loot4Everyone.server);
				}
				if (StateSaverAndLoader.isItemFrameStatePresent(Loot4Everyone.server,chest.getPos())){
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(server, chest.getPos());
					if (itemFrameData.getPlayersUsed().contains(player.getUuid())){
						return ActionResult.CONSUME;
					}
					else{
						player.giveItemStack(Items.ELYTRA.getDefaultStack());
						itemFrameData.getPlayersUsed().add(player.getUuid());
						StateSaverAndLoader.saveState(server);
						return ActionResult.CONSUME;
					}
				}
			}
			if (blockEntity instanceof BarrelBlockEntity barrel){
				int number_of_players = BarrelViewers.getViewerCount(barrel.getPos());
				if (number_of_players > 0 && (barrel.getLootTableSeed() != 0 || StateSaverAndLoader.isChestStatePresent(server,barrel.getPos()))){
					return ActionResult.CONSUME;
				}
				if (barrel.getLootTableSeed() != 0 && barrel.getLootTable() != null && !StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,barrel.getPos())){
					ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, barrel.getPos());
					chestData.setLootTable(barrel.getLootTable());
					chestData.setLootTableSeed(barrel.getLootTableSeed());
					barrel.setLootTable(null);
					barrel.setLootTableSeed(0);
					StateSaverAndLoader.saveState(Loot4Everyone.server);
				}
			}
			return ActionResult.PASS;
		});
		UseEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
			if (entity instanceof ChestMinecartEntity minecart){
				if (minecart.getLootTableSeed() != 0){
					BlockPos pos = minecart.getBlockPos();
					world.setBlockState(pos, Blocks.CHEST.getDefaultState());
					BlockEntity blockEntity = world.getBlockEntity(pos);
					if (blockEntity instanceof ChestBlockEntity chest){
						chest.setLootTable(minecart.getLootTable(), minecart.getLootTableSeed());
					}
					minecart.setLootTable(null,0);
					minecart.discard();
					return ActionResult.CONSUME;
				}
			}
			if (entity instanceof ItemFrameEntity itemFrame){
				ItemStack heldItem = itemFrame.getHeldItemStack();
				if (heldItem.getItem() == Items.ELYTRA){
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(server, itemFrame.getBlockPos());
					if (itemFrameData.isPlayerPlaced()){
						return ActionResult.PASS;
					}
					else{
						BlockPos pos = itemFrame.getBlockPos();
						world.setBlockState(pos,Blocks.CHEST.getDefaultState());
						itemFrame.discard();
						return ActionResult.CONSUME;
					}
				}
			}
			return ActionResult.PASS;
		}));
		AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
			if (entity instanceof ItemFrameEntity itemFrame){
				ItemStack heldItem = itemFrame.getHeldItemStack();
				if (heldItem.getItem() == Items.ELYTRA){
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(server, itemFrame.getBlockPos());
					if (itemFrameData.isPlayerPlaced()){
						return ActionResult.PASS;
					}
					else{
						BlockPos pos = itemFrame.getBlockPos();
						world.setBlockState(pos,Blocks.CHEST.getDefaultState());
						itemFrame.discard();
						return ActionResult.CONSUME;
					}
				}
			}
			return ActionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) -> {
			if (blockEntity instanceof ChestBlockEntity chest){
				if ((chest.getLootTableSeed() != 0 && chest.getLootTable() != null) || StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,pos)){
					return false;
				}
			}
			else if (blockEntity instanceof BarrelBlockEntity barrel){
				if ((barrel.getLootTableSeed() != 0 && barrel.getLootTable() != null) || StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,pos)){
					return false;
				}
			}
			return true;
		});
		LOGGER.info("Loot4Everyone has been successfully loaded!");
	}
}