package com.loot4everyone;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.advancement.criterion.PlayerHurtEntityCriterion;
import net.minecraft.block.Block;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
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

	public static String getModId() {
		return MOD_ID;
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
			if (blockEntity instanceof ChestBlockEntity chest){
				int number_of_players = ChestBlockEntity.getPlayersLookingInChestCount(world,hitResult.getBlockPos());
				if (number_of_players > 0 && (chest.getLootTableSeed() != 0 || StateSaverAndLoader.isChestStatePresent(player,chest.getPos()))){
					return ActionResult.CONSUME;
				}
				if (chest.getLootTableSeed() != 0 && !StateSaverAndLoader.isChestStatePresent(player,chest.getPos())){
					addLootChest(player,chest);
					return ActionResult.PASS;
				}
				if (StateSaverAndLoader.isChestStatePresent(player,chest.getPos())){
					if (StateSaverAndLoader.isChestStatePresentInPlayerState(player,chest.getPos())){
						setInventoryLootChest(player,chest);
						return ActionResult.PASS;
					}
					else{
						generateLootChest(player,chest);
						return ActionResult.PASS;
					}
				}
			}
			if (blockEntity instanceof BarrelBlockEntity barrel){
				int number_of_players = BarrelViewers.getViewerCount(barrel.getPos());
				if (number_of_players > 0 && (barrel.getLootTableSeed() != 0 || StateSaverAndLoader.isChestStatePresent(player,barrel.getPos()))){
					return ActionResult.CONSUME;
				}
				if (barrel.getLootTableSeed() != 0 && !StateSaverAndLoader.isChestStatePresent(player,barrel.getPos())){
					addLootChest(player,barrel);
					BarrelViewers.addViewer(player,barrel.getPos());
					return ActionResult.PASS;
				}
				if (StateSaverAndLoader.isChestStatePresent(player,barrel.getPos())){
					if (StateSaverAndLoader.isChestStatePresentInPlayerState(player,barrel.getPos())){
						setInventoryLootChest(player,barrel);
						BarrelViewers.addViewer(player,barrel.getPos());
						return ActionResult.PASS;
					}
					else{
						generateLootChest(player,barrel);
						BarrelViewers.addViewer(player,barrel.getPos());
						return ActionResult.PASS;
					}
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
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(itemFrame, itemFrame.getBlockPos());
					if (itemFrameData.isPlayerPlaced()){
						return ActionResult.PASS;
					}
					else{
						if (itemFrameData.getPlayersUsed().contains(playerEntity.getUuid())){
							return ActionResult.CONSUME;
						}
						else{
							playerEntity.giveItemStack(heldItem);
							itemFrameData.getPlayersUsed().add(playerEntity.getUuid());
							StateSaverAndLoader.saveState(Objects.requireNonNull(playerEntity.getServer()));
							return ActionResult.CONSUME;
						}
					}
				}
			}
			return ActionResult.PASS;
		}));
		AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
			if (entity instanceof ItemFrameEntity itemFrame){
				ItemStack heldItem = itemFrame.getHeldItemStack();
				if (heldItem.getItem() == Items.ELYTRA){
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(itemFrame, itemFrame.getBlockPos());
					if (itemFrameData.isPlayerPlaced()){
						return ActionResult.PASS;
					}
					else{
						if (itemFrameData.getPlayersUsed().contains(playerEntity.getUuid())){
							return ActionResult.CONSUME;
						}
						else{
							playerEntity.giveItemStack(heldItem);
							itemFrameData.getPlayersUsed().add(playerEntity.getUuid());
							StateSaverAndLoader.saveState(Objects.requireNonNull(playerEntity.getServer()));
							return ActionResult.CONSUME;
						}
					}
				}
			}
			return ActionResult.PASS;
		});
		LOGGER.info("Loot4Everyone has been successfully loaded!");
	}

	private void addLootChest(PlayerEntity player, BlockEntity block){
		if (block instanceof ChestBlockEntity chest) {
			ChestData chestData = StateSaverAndLoader.getChestState(player, chest.getPos());
			chestData.setLootTable(chest.getLootTable());
			chestData.setLootTableSeed(chest.getLootTableSeed());
			StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
		}
		else if (block instanceof BarrelBlockEntity barrel){
			ChestData chestData = StateSaverAndLoader.getChestState(player, barrel.getPos());
			chestData.setLootTable(barrel.getLootTable());
			chestData.setLootTableSeed(barrel.getLootTableSeed());
			StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
		}
	}

	private void generateLootChest(PlayerEntity player, BlockEntity block){
		if (block instanceof ChestBlockEntity chest) {
			ChestData chestData = StateSaverAndLoader.getChestState(player, chest.getPos());
			chest.setLootTable(chestData.getLootTable(), chestData.getLootTableSeed());
			chest.generateLoot(player);
		}
		else if (block instanceof BarrelBlockEntity barrel){
			ChestData chestData = StateSaverAndLoader.getChestState(player, barrel.getPos());
			barrel.setLootTable(chestData.getLootTable(), chestData.getLootTableSeed());
			barrel.generateLoot(player);
		}
	}

	private void setInventoryLootChest(PlayerEntity player, BlockEntity block){
		if (block instanceof ChestBlockEntity chest) {
			PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
			List<ItemStack> inventory = playerData.getInventory().get(chest.getPos());
			for (int i = 0; i < inventory.size(); i++) {
				chest.setStack(i, inventory.get(i));
			}
		}
		else if (block instanceof BarrelBlockEntity barrel){
			PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
			List<ItemStack> inventory = playerData.getInventory().get(barrel.getPos());
			for (int i = 0; i < inventory.size(); i++) {
				barrel.setStack(i, inventory.get(i));
			}
		}
	}
}