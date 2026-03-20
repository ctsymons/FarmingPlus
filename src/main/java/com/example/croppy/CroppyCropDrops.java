package com.example.croppy;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CroppyCropDrops {
	private static final int BOOK_DROP_DENOMINATOR = 1000;
	private static final int MELON_DYE_DROP_DENOMINATOR = 1000;
	private static final int MEATROOT_DROP_DENOMINATOR = 128;
	private static final float FORTUNE_ONE_EXTRA_CHANCE_PER_LEVEL = 0.33F;
	private static final float FORTUNE_TWO_EXTRA_CHANCE_PER_LEVEL = 0.15F;
	private static final float FORTUNE_THREE_EXTRA_CHANCE_PER_LEVEL = 0.083F;
	private static final Component GENTLE_BLOCK_MESSAGE = Component.literal("This tool is for farming!");
	private static final List<PendingReplant> PENDING_REPLANTS = new ArrayList<>();

	private CroppyCropDrops() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			BreakRestriction restriction = getBreakRestriction(player.getMainHandItem(), state);
			if (restriction == BreakRestriction.GENTLE_NON_FARMING && !level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				serverPlayer.displayClientMessage(GENTLE_BLOCK_MESSAGE, true);
			}

			return restriction == BreakRestriction.NONE;
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel serverLevel)) {
				return;
			}

			boolean playerPlacedPumpkin = state.is(Blocks.PUMPKIN) && PlayerPlacedPumpkinsData.clearIfPresent(serverLevel, pos);
			tryDropMelonDye(serverLevel, pos, state);
			tryDropMeatroot(serverLevel, pos, state);
			tryDropWisdomXp(serverLevel, pos, state, player.getMainHandItem(), playerPlacedPumpkin);
			tryDropFortuneBonus(serverLevel, pos, state, player.getMainHandItem(), playerPlacedPumpkin);

			ReplantTarget replantTarget = getReplantTarget(serverLevel, pos, state);
			if (replantTarget == null && !isBookDropCrop(state)) {
				return;
			}

			tryReplenishCrop(player.getMainHandItem(), replantTarget);

			if (!isBookDropCrop(state) || serverLevel.getRandom().nextInt(BOOK_DROP_DENOMINATOR) != 0) {
				return;
			}

			Block.popResource(serverLevel, pos, CroppyEnchantmentUtil.createCroppyBook(serverLevel.registryAccess(), 1));
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING_REPLANTS.isEmpty()) {
				return;
			}

			List<PendingReplant> replants = List.copyOf(PENDING_REPLANTS);
			PENDING_REPLANTS.clear();
			for (PendingReplant replant : replants) {
				replant.apply();
			}
		});
	}

	private static void tryDropMelonDye(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.is(Blocks.MELON) || level.getRandom().nextInt(MELON_DYE_DROP_DENOMINATOR) != 0) {
			return;
		}

		Block.popResource(level, pos, CroppyEnchantmentUtil.createMelonRind(1));
	}

	private static void tryDropMeatroot(ServerLevel level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof CropBlock cropBlock) || !state.is(Blocks.BEETROOTS) || !cropBlock.isMaxAge(state)) {
			return;
		}

		if (level.getRandom().nextInt(MEATROOT_DROP_DENOMINATOR) != 0) {
			return;
		}

		Block.popResource(level, pos, CroppyEnchantmentUtil.createMeatroot(1));
	}

	private static void tryDropWisdomXp(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, boolean playerPlacedPumpkin) {
		if (playerPlacedPumpkin) {
			return;
		}

		if (!(tool.getItem() instanceof HoeItem) && !(tool.getItem() instanceof AxeItem)) {
			return;
		}

		int wisdomLevel = CroppyEnchantmentUtil.getWisdomLevel(tool);
		if (wisdomLevel <= 0 || !isWisdomCrop(state)) {
			return;
		}

		if (level.getRandom().nextFloat() >= wisdomLevel * 0.10F) {
			return;
		}

		ExperienceOrb.award(level, Vec3.atCenterOf(pos), 1);
	}

	private static void tryDropFortuneBonus(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, boolean playerPlacedPumpkin) {
		if (playerPlacedPumpkin) {
			return;
		}

		if (!supportsFortuneBonus(tool, state)) {
			return;
		}

		int fortuneLevel = getFortuneLevel(level.registryAccess(), tool);
		if (fortuneLevel <= 0 || !isFortuneCrop(state)) {
			return;
		}

		Item cropItem = getCropItem(state);
		Item seedItem = getSeedItem(state);
		int cropBonus = rollFortuneBonus(level, fortuneLevel);
		if (cropItem != null && cropBonus > 0) {
			Block.popResource(level, pos, new ItemStack(cropItem, cropBonus));
		}

		int seedBonus = rollFortuneBonus(level, fortuneLevel);
		if (seedItem != null && seedBonus > 0) {
			Block.popResource(level, pos, new ItemStack(seedItem, seedBonus));
		}
	}

	private static boolean supportsFortuneBonus(ItemStack tool, BlockState state) {
		if (tool.getItem() instanceof HoeItem) {
			return true;
		}

		return tool.getItem() instanceof AxeItem && state.is(Blocks.PUMPKIN);
	}

	private static int rollFortuneBonus(ServerLevel level, int fortuneLevel) {
		float roll = level.getRandom().nextFloat();
		float oneExtraThreshold = FORTUNE_ONE_EXTRA_CHANCE_PER_LEVEL * fortuneLevel;
		float twoExtraThreshold = FORTUNE_TWO_EXTRA_CHANCE_PER_LEVEL * fortuneLevel;
		float threeExtraThreshold = FORTUNE_THREE_EXTRA_CHANCE_PER_LEVEL * fortuneLevel;

		if (roll < threeExtraThreshold) {
			return 3;
		}

		if (roll < twoExtraThreshold) {
			return 2;
		}

		if (roll < oneExtraThreshold) {
			return 1;
		}

		return 0;
	}

	private static BreakRestriction getBreakRestriction(ItemStack tool, BlockState state) {
		if (tool.getItem() instanceof HoeItem && CroppyEnchantmentUtil.getReplenishLevel(tool) > 0) {
			return isProtectedImmatureCrop(state) ? BreakRestriction.REPLENISH_IMMATURE : BreakRestriction.NONE;
		}

		if (tool.getItem() instanceof AxeItem && CroppyEnchantmentUtil.getGentleLevel(tool) > 0) {
			return isGentleAllowedBlock(state) ? BreakRestriction.NONE : BreakRestriction.GENTLE_NON_FARMING;
		}

		return BreakRestriction.NONE;
	}

	private static boolean isBookDropCrop(BlockState state) {
		if (!(state.getBlock() instanceof CropBlock cropBlock)) {
			return false;
		}

		if (!cropBlock.isMaxAge(state)) {
			return false;
		}

		return state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES) || state.is(Blocks.BEETROOTS);
	}

	private static boolean isProtectedImmatureCrop(BlockState state) {
		if (state.getBlock() instanceof CropBlock cropBlock && getSeedItem(state) != null) {
			return !cropBlock.isMaxAge(state);
		}

		if (state.getBlock() instanceof StemBlock && getSeedItem(state) != null && state.hasProperty(StemBlock.AGE)) {
			return state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE;
		}

		if (state.getBlock() instanceof PitcherCropBlock
			&& state.hasProperty(PitcherCropBlock.AGE)
			&& getSeedItem(state) != null) {
			return state.getValue(PitcherCropBlock.AGE) < PitcherCropBlock.MAX_AGE;
		}

		return false;
	}

	private static boolean isWisdomCrop(BlockState state) {
		if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON) || state.is(Blocks.COCOA)) {
			return true;
		}

		if (state.getBlock() instanceof CropBlock cropBlock) {
			return cropBlock.isMaxAge(state);
		}

		if (state.getBlock() instanceof StemBlock && state.hasProperty(StemBlock.AGE)) {
			return state.getValue(StemBlock.AGE) >= StemBlock.MAX_AGE;
		}

		if (state.getBlock() instanceof PitcherCropBlock && state.hasProperty(PitcherCropBlock.AGE)) {
			return state.getValue(PitcherCropBlock.AGE) >= PitcherCropBlock.MAX_AGE;
		}

		return state.getBlock() instanceof AttachedStemBlock;
	}

	private static boolean isFortuneCrop(BlockState state) {
		if (state.is(Blocks.PUMPKIN)) {
			return true;
		}

		if (state.getBlock() instanceof CropBlock cropBlock) {
			return cropBlock.isMaxAge(state) && (getCropItem(state) != null || getSeedItem(state) != null);
		}

		if (state.getBlock() instanceof PitcherCropBlock
			&& state.hasProperty(PitcherCropBlock.AGE)) {
			return state.getValue(PitcherCropBlock.AGE) >= PitcherCropBlock.MAX_AGE
				&& (getCropItem(state) != null || getSeedItem(state) != null);
		}

		return false;
	}

	private static boolean isGentleAllowedBlock(BlockState state) {
		return state.is(Blocks.COCOA)
			|| state.is(Blocks.MELON)
			|| state.is(Blocks.PUMPKIN);
	}

	private static void tryReplenishCrop(ItemStack tool, ReplantTarget target) {
		if (target == null || !(tool.getItem() instanceof HoeItem) || CroppyEnchantmentUtil.getReplenishLevel(tool) <= 0) {
			return;
		}

		PENDING_REPLANTS.add(new PendingReplant(target.level(), target.pos(), target.replantState(), target.seedItem()));
	}

	private static boolean consumeNearbyDrop(ServerLevel level, net.minecraft.core.BlockPos pos, Item item) {
		for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0D))) {
			ItemStack stack = itemEntity.getItem();
			if (!stack.is(item)) {
				continue;
			}

			stack.shrink(1);
			if (stack.isEmpty()) {
				itemEntity.discard();
			} else {
				itemEntity.setItem(stack);
			}

			return true;
		}

		return false;
	}

	private static ReplantTarget getReplantTarget(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
			return createReplantTarget(level, pos, state, cropBlock.defaultBlockState());
		}

		if (state.getBlock() instanceof StemBlock && state.hasProperty(StemBlock.AGE) && state.getValue(StemBlock.AGE) >= StemBlock.MAX_AGE) {
			return createReplantTarget(level, pos, state, state.getBlock().defaultBlockState());
		}

		if (state.getBlock() instanceof PitcherCropBlock
			&& state.hasProperty(PitcherCropBlock.AGE)
			&& state.getValue(PitcherCropBlock.AGE) >= PitcherCropBlock.MAX_AGE) {
			BlockPos rootPos = state.hasProperty(PitcherCropBlock.HALF) && state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.UPPER
				? pos.below()
				: pos;
			return createReplantTarget(level, rootPos, level.getBlockState(rootPos), Blocks.PITCHER_CROP.defaultBlockState());
		}

		if (state.getBlock() instanceof AttachedStemBlock) {
			if (state.is(Blocks.ATTACHED_PUMPKIN_STEM)) {
				return new ReplantTarget(level, pos.immutable(), Blocks.PUMPKIN_STEM.defaultBlockState(), Items.PUMPKIN_SEEDS);
			}

			if (state.is(Blocks.ATTACHED_MELON_STEM)) {
				return new ReplantTarget(level, pos.immutable(), Blocks.MELON_STEM.defaultBlockState(), Items.MELON_SEEDS);
			}
		}

		return null;
	}

	private static ReplantTarget createReplantTarget(ServerLevel level, BlockPos pos, BlockState sourceState, BlockState replantState) {
		Item seedItem = getSeedItem(sourceState);
		if (seedItem == null) {
			return null;
		}

		return new ReplantTarget(level, pos.immutable(), replantState, seedItem);
	}

	private static Item getSeedItem(BlockState state) {
		if (state.is(Blocks.WHEAT)) {
			return Items.WHEAT_SEEDS;
		}

		if (state.is(Blocks.BEETROOTS)) {
			return Items.BEETROOT_SEEDS;
		}

		if (state.is(Blocks.CARROTS)) {
			return Items.CARROT;
		}

		if (state.is(Blocks.POTATOES)) {
			return Items.POTATO;
		}

		if (state.is(Blocks.TORCHFLOWER_CROP)) {
			return Items.TORCHFLOWER_SEEDS;
		}

		if (state.is(Blocks.PITCHER_CROP)) {
			return Items.PITCHER_POD;
		}

		if (state.is(Blocks.PUMPKIN_STEM) || state.is(Blocks.ATTACHED_PUMPKIN_STEM)) {
			return Items.PUMPKIN_SEEDS;
		}

		if (state.is(Blocks.MELON_STEM) || state.is(Blocks.ATTACHED_MELON_STEM)) {
			return Items.MELON_SEEDS;
		}

		return null;
	}

	private static Item getCropItem(BlockState state) {
		if (state.is(Blocks.PUMPKIN)) {
			return Items.PUMPKIN;
		}

		if (state.is(Blocks.WHEAT)) {
			return Items.WHEAT;
		}

		if (state.is(Blocks.BEETROOTS)) {
			return Items.BEETROOT;
		}

		if (state.is(Blocks.CARROTS)) {
			return Items.CARROT;
		}

		if (state.is(Blocks.POTATOES)) {
			return Items.POTATO;
		}

		if (state.is(Blocks.TORCHFLOWER_CROP)) {
			return Items.TORCHFLOWER;
		}

		if (state.is(Blocks.PITCHER_CROP)) {
			return Items.PITCHER_PLANT;
		}

		return null;
	}

	private static int getFortuneLevel(HolderLookup.Provider lookupProvider, ItemStack tool) {
		HolderLookup.RegistryLookup<Enchantment> enchantments = lookupProvider.lookupOrThrow(Registries.ENCHANTMENT);
		return EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FORTUNE), tool);
	}

	private record PendingReplant(ServerLevel level, BlockPos pos, BlockState replantState, Item seedItem) {
		void apply() {
			if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()) {
				return;
			}

			if (!consumeNearbyDrop(level, pos, seedItem)) {
				return;
			}

			level.setBlock(pos, replantState, Block.UPDATE_ALL);
		}
	}

	private record ReplantTarget(ServerLevel level, BlockPos pos, BlockState replantState, Item seedItem) {
	}

	private enum BreakRestriction {
		NONE,
		REPLENISH_IMMATURE,
		GENTLE_NON_FARMING
	}
}
