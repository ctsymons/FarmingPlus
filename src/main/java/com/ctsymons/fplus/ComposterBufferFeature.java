package com.ctsymons.fplus;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class ComposterBufferFeature {
	private static final Component STORED_PREFIX = Component.literal("Stored Bone Meal: ")
		.withStyle(ChatFormatting.GOLD);

	private ComposterBufferFeature() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel && state.getBlock() instanceof ComposterBlock) {
				ComposterBufferData.clear(serverLevel, pos);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				for (Player player : level.players()) {
					showLookingPlayerStoredAmount(level, player);
				}
			}
		});
	}

	public static boolean usesHiddenBuffer(LevelAccessor level, BlockPos pos) {
		return !hasAutomationHopper(level, pos);
	}

	public static boolean hasAutomationHopper(LevelAccessor level, BlockPos pos) {
		return level.getBlockState(pos.above()).is(Blocks.HOPPER)
			|| level.getBlockState(pos.below()).is(Blocks.HOPPER);
	}

	public static int getStoredBoneMeal(ServerLevel level, BlockPos pos) {
		return ComposterBufferData.getStored(level, pos);
	}

	public static boolean tryBufferCompletion(ServerLevel level, BlockPos pos, BlockState state, boolean playSound) {
		if (!(state.getBlock() instanceof ComposterBlock)
			|| state.getValue(ComposterBlock.LEVEL) != 7
			|| !usesHiddenBuffer(level, pos)
			|| !ComposterBufferData.addBoneMeal(level, pos, 1)) {
			return false;
		}

		BlockState emptyState = state.setValue(ComposterBlock.LEVEL, 0);
		level.setBlock(pos, emptyState, Block.UPDATE_ALL);
		if (playSound) {
			level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
		}

		return true;
	}

	public static boolean tryExtractBufferedBoneMeal(ServerLevel level, BlockPos pos, Player player) {
		if (!usesHiddenBuffer(level, pos)) {
			return false;
		}

		int stored = ComposterBufferData.getStored(level, pos);
		if (stored <= 0) {
			return false;
		}

		int extracted = ComposterBufferData.removeBoneMeal(level, pos, Math.min(64, stored));
		if (extracted <= 0) {
			return false;
		}

		player.getInventory().placeItemBackInInventory(new ItemStack(Items.BONE_MEAL, extracted));
		level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
		showStoredAmount(player, ComposterBufferData.getStored(level, pos));
		return true;
	}

	public static void showStoredAmount(Player player, int stored) {
		player.displayClientMessage(STORED_PREFIX.copy()
			.append(Component.literal(Integer.toString(stored)).withStyle(ChatFormatting.WHITE)), true);
	}

	private static void showLookingPlayerStoredAmount(ServerLevel level, Player player) {
		HitResult hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);
		if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPos pos = blockHitResult.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ComposterBlock) || !usesHiddenBuffer(level, pos)) {
			return;
		}

		showStoredAmount(player, ComposterBufferData.getStored(level, pos));
	}
}
