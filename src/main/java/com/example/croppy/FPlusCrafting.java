package com.example.croppy;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class FPlusCrafting {
	private static final int GRID_SIZE = 9;

	private FPlusCrafting() {
	}

	public static ItemStack getCraftingResult(CraftingInput input, HolderLookup.Provider lookupProvider) {
		if (matchesStretchedHide(input)) {
			return CroppyEnchantmentUtil.createStretchedHide(1);
		}

		if (matchesXpEssence(input)) {
			return CroppyEnchantmentUtil.createXpEssence(1);
		}

		if (matchesFarmersBag(input)) {
			return CroppyEnchantmentUtil.createFarmersBag(1);
		}

		if (matchesCondensedBoneMeal(input)) {
			return CroppyEnchantmentUtil.createCondensedBoneMeal(1);
		}

		if (lookupProvider == null) {
			return ItemStack.EMPTY;
		}

		if (matchesGentle(input, lookupProvider)) {
			return CroppyEnchantmentUtil.createBook(lookupProvider, "gentle", 1);
		}

		if (matchesReplenish(input)) {
			return CroppyEnchantmentUtil.createBook(lookupProvider, "replenish", 1);
		}

		if (matchesHearty(input, lookupProvider)) {
			return CroppyEnchantmentUtil.createBook(lookupProvider, "hearty", 1);
		}

		if (matchesWisdom(input)) {
			return CroppyEnchantmentUtil.createBook(lookupProvider, "wisdom", 1);
		}

		return ItemStack.EMPTY;
	}

	public static int[] getExtraConsumeCounts(CraftingInput input) {
		if (matchesXpEssence(input)) {
			return filledGrid(1);
		}

		if (matchesCondensedBoneMeal(input)) {
			return outerGrid(63);
		}

		if (matchesStretchedHide(input)) {
			return outerGrid(1);
		}

		if (matchesFarmersBag(input)) {
			return outerGrid(7);
		}

		return null;
	}

	private static boolean matchesXpEssence(CraftingInput input) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}

		for (int y = 0; y < input.height(); y++) {
			for (int x = 0; x < input.width(); x++) {
				ItemStack stack = input.getItem(x, y);
				if (!stack.is(Items.EXPERIENCE_BOTTLE) || stack.getCount() < 2) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean matchesCondensedBoneMeal(CraftingInput input) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}

		for (int y = 0; y < input.height(); y++) {
			for (int x = 0; x < input.width(); x++) {
				if (x == 1 && y == 1) {
					if (!input.getItem(x, y).isEmpty()) {
						return false;
					}
					continue;
				}

				ItemStack stack = input.getItem(x, y);
				if (!stack.is(Items.BONE_MEAL) || stack.getCount() < 64) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean matchesStretchedHide(CraftingInput input) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}

		for (int y = 0; y < input.height(); y++) {
			for (int x = 0; x < input.width(); x++) {
				if (x == 1 && y == 1) {
					if (!input.getItem(x, y).isEmpty()) {
						return false;
					}
					continue;
				}

				ItemStack stack = input.getItem(x, y);
				if (!stack.is(Items.LEATHER) || stack.getCount() < 2) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean matchesFarmersBag(CraftingInput input) {
		return input.width() == 3
			&& input.height() == 3
			&& input.getItem(0, 0).is(Items.HAY_BLOCK) && input.getItem(0, 0).getCount() >= 8
			&& input.getItem(1, 0).is(Items.HAY_BLOCK) && input.getItem(1, 0).getCount() >= 8
			&& input.getItem(2, 0).is(Items.HAY_BLOCK) && input.getItem(2, 0).getCount() >= 8
			&& input.getItem(0, 1).is(Items.HAY_BLOCK) && input.getItem(0, 1).getCount() >= 8
			&& CroppyEnchantmentUtil.isStretchedHide(input.getItem(1, 1))
			&& input.getItem(2, 1).is(Items.HAY_BLOCK) && input.getItem(2, 1).getCount() >= 8
			&& input.getItem(0, 2).is(Items.HAY_BLOCK) && input.getItem(0, 2).getCount() >= 8
			&& input.getItem(1, 2).is(Items.HAY_BLOCK) && input.getItem(1, 2).getCount() >= 8
			&& input.getItem(2, 2).is(Items.HAY_BLOCK) && input.getItem(2, 2).getCount() >= 8;
	}

	private static boolean matchesGentle(CraftingInput input, HolderLookup.Provider lookupProvider) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}

		Holder<Enchantment> silkTouch = lookupProvider.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.SILK_TOUCH);
		return CroppyEnchantmentUtil.isMelonRind(input.getItem(0, 0))
			&& isEnchantedBook(input.getItem(1, 0), silkTouch, 1)
			&& CroppyEnchantmentUtil.isMelonRind(input.getItem(2, 0))
			&& isEnchantedBook(input.getItem(0, 1), silkTouch, 1)
			&& input.getItem(1, 1).is(Items.BOOK)
			&& isEnchantedBook(input.getItem(2, 1), silkTouch, 1)
			&& CroppyEnchantmentUtil.isMelonRind(input.getItem(0, 2))
			&& isEnchantedBook(input.getItem(1, 2), silkTouch, 1)
			&& CroppyEnchantmentUtil.isMelonRind(input.getItem(2, 2));
	}

	private static boolean matchesReplenish(CraftingInput input) {
		return input.width() == 3
			&& input.height() == 3
			&& CroppyEnchantmentUtil.isCondensedBoneMeal(input.getItem(0, 0))
			&& CroppyEnchantmentUtil.isBook(input.getItem(1, 0), "gentle", 1)
			&& CroppyEnchantmentUtil.isCondensedBoneMeal(input.getItem(2, 0))
			&& CroppyEnchantmentUtil.isBook(input.getItem(0, 1), "croppy", 1)
			&& input.getItem(1, 1).is(Items.BOOK)
			&& CroppyEnchantmentUtil.isBook(input.getItem(2, 1), "croppy", 1)
			&& CroppyEnchantmentUtil.isCondensedBoneMeal(input.getItem(0, 2))
			&& CroppyEnchantmentUtil.isBook(input.getItem(1, 2), "gentle", 1)
			&& CroppyEnchantmentUtil.isCondensedBoneMeal(input.getItem(2, 2));
	}

	private static boolean matchesHearty(CraftingInput input, HolderLookup.Provider lookupProvider) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}

		Holder<Enchantment> protection = lookupProvider.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.PROTECTION);
		return CroppyEnchantmentUtil.isMeatroot(input.getItem(0, 0))
			&& isInstantHealthPotion(input.getItem(1, 0))
			&& CroppyEnchantmentUtil.isMeatroot(input.getItem(2, 0))
			&& isEnchantedBook(input.getItem(0, 1), protection, 3)
			&& input.getItem(1, 1).is(Items.ENCHANTED_GOLDEN_APPLE)
			&& isEnchantedBook(input.getItem(2, 1), protection, 3)
			&& CroppyEnchantmentUtil.isMeatroot(input.getItem(0, 2))
			&& isInstantHealthPotion(input.getItem(1, 2))
			&& CroppyEnchantmentUtil.isMeatroot(input.getItem(2, 2));
	}

	private static boolean matchesWisdom(CraftingInput input) {
		return input.width() == 3
			&& input.height() == 3
			&& CroppyEnchantmentUtil.isXpEssence(input.getItem(0, 0))
			&& input.getItem(1, 0).is(Items.SCULK)
			&& CroppyEnchantmentUtil.isXpEssence(input.getItem(2, 0))
			&& input.getItem(0, 1).is(Items.SCULK)
			&& input.getItem(1, 1).is(Items.EMERALD_BLOCK)
			&& input.getItem(2, 1).is(Items.SCULK)
			&& CroppyEnchantmentUtil.isXpEssence(input.getItem(0, 2))
			&& input.getItem(1, 2).is(Items.SCULK)
			&& CroppyEnchantmentUtil.isXpEssence(input.getItem(2, 2));
	}

	private static boolean isEnchantedBook(ItemStack stack, Holder<Enchantment> enchantment, int level) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return false;
		}

		ItemEnchantments storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
		return storedEnchantments != null && storedEnchantments.getLevel(enchantment) == level;
	}

	private static boolean isInstantHealthPotion(ItemStack stack) {
		if (!stack.is(Items.LINGERING_POTION)) {
			return false;
		}

		PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
		return potionContents != null && potionContents.is(Potions.STRONG_HEALING);
	}

	private static int[] filledGrid(int amount) {
		int[] counts = new int[GRID_SIZE];
		java.util.Arrays.fill(counts, amount);
		return counts;
	}

	private static int[] outerGrid(int amount) {
		int[] counts = new int[GRID_SIZE];
		for (int index : new int[] {0, 1, 2, 3, 5, 6, 7, 8}) {
			counts[index] = amount;
		}
		return counts;
	}
}
