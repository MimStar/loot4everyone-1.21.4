package com.loot4everyone;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("lootprotection")
					.requires(source -> source.hasPermissionLevel(4))
					.then(CommandManager.argument("value", BoolArgumentType.bool())
							.executes(this::lootProtectionExecute)));
		});
		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			Loot4Everyone.server = server;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
			if (blockEntity instanceof ChestBlockEntity chest){
				int number_of_players = ChestBlockEntity.getPlayersLookingInChestCount(world,hitResult.getBlockPos());
				if (number_of_players > 0 && (chest.getLootTableSeed() != 0 || StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,chest) != null || StateSaverAndLoader.isItemFrameStatePresent(Loot4Everyone.server,chest.getPos()))){
					return ActionResult.CONSUME;
				}
				if (chest.getLootTableSeed() != 0 && chest.getLootTable() != null && StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,chest) == null){
					ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, chest.getPos());
					chestData.setLootTable(chest.getLootTable());
					chestData.setLootTableSeed(chest.getLootTableSeed());
					chest.setLootTable(null);
					chest.setLootTableSeed(0);
					StateSaverAndLoader.saveState(Loot4Everyone.server);
				}
				if (StateSaverAndLoader.isItemFrameStatePresent(Loot4Everyone.server,chest.getPos()) && world.getDimensionEntry().matchesKey(DimensionTypes.THE_END)){
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(Loot4Everyone.server, chest.getPos());
					if (itemFrameData.getPlayersUsed().contains(player.getUuid())){
						return ActionResult.CONSUME;
					}
					else{
						player.giveItemStack(Items.ELYTRA.getDefaultStack());
						itemFrameData.getPlayersUsed().add(player.getUuid());
						StateSaverAndLoader.saveState(Loot4Everyone.server);
						return ActionResult.CONSUME;
					}
				}
			}
			if (blockEntity instanceof BarrelBlockEntity barrel){
				int number_of_players = BarrelViewers.getViewerCount(barrel.getPos());
				if (number_of_players > 0 && (barrel.getLootTableSeed() != 0 || StateSaverAndLoader.isBarrelStatePresent(Loot4Everyone.server,barrel.getPos()))){
					return ActionResult.CONSUME;
				}
				if (barrel.getLootTableSeed() != 0 && barrel.getLootTable() != null && !StateSaverAndLoader.isBarrelStatePresent(Loot4Everyone.server,barrel.getPos())){
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
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(Loot4Everyone.server, itemFrame.getBlockPos());
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
					ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(Loot4Everyone.server, itemFrame.getBlockPos());
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
				if (StateSaverAndLoader.getSettingsState(Loot4Everyone.server).getLootProtection() && ((chest.getLootTableSeed() != 0 && chest.getLootTable() != null) || StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,chest) != null)){
					if (Loot4Everyone.server.getPlayerManager().isOperator(player.getGameProfile())){
						return true;
					}
					return false;
				}
			}
			else if (blockEntity instanceof BarrelBlockEntity barrel){
				if (StateSaverAndLoader.getSettingsState(Loot4Everyone.server).getLootProtection() && ((barrel.getLootTableSeed() != 0 && barrel.getLootTable() != null) || StateSaverAndLoader.isBarrelStatePresent(Loot4Everyone.server,pos))){
					if (Loot4Everyone.server.getPlayerManager().isOperator(player.getGameProfile())){
						return true;
					}
					return false;
				}
			}
			return true;
		});
		LOGGER.info("Loot4Everyone has been successfully loaded!");
	}

	private int lootProtectionExecute(CommandContext<ServerCommandSource> context){
		boolean value = BoolArgumentType.getBool(context, "value");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		StateSaverAndLoader.getSettingsState(Loot4Everyone.server).setLootProtection(value);
		StateSaverAndLoader.saveState(Loot4Everyone.server);
		if (value){
			context.getSource().sendFeedback(() -> Text.literal("Loot protection enabled.").formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Loot protection disabled.").formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}
}