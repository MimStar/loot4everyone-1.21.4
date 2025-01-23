package com.loot4everyone;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loot4Everyone implements ModInitializer {
	public static final String MOD_ID = "loot4everyone";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
			if (blockEntity instanceof ChestBlockEntity chest){
				int number_of_players = ChestBlockEntity.getPlayersLookingInChestCount(world,hitResult.getBlockPos());
				if (number_of_players > 0 || chest.getLootTableSeed() != 0){
					if (chest instanceof PersistentLootTableAccessor accessor){
						System.out.println(accessor.getPersistentLootTable());
					}
					return ActionResult.CONSUME;
				}
			}
			return ActionResult.PASS;
		});
		LOGGER.info("Loot4Everyone has been successfully loaded!");
	}
}