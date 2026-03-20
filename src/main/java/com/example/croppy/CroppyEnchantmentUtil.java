package com.example.croppy;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;

public final class CroppyEnchantmentUtil {
	private static final String FPLUS_KEY = "fplus";
	private static final String BOOK_KEY = "book";
	private static final String APPLIED_KEY = "applied";
	private static final String DISPLAY_MARKER_KEY = "FPlusDisplay";
	private static final String GENERATED_KEY = "Generated";
	private static final String DISPLAY_TYPE_KEY = "Type";
	private static final String APPLIED_DISPLAY_TYPE = "applied";
	private static final String BOOK_DISPLAY_TYPE = "book";
	private static final String GENERATED_LINE_COUNT_KEY = "GeneratedLineCount";
	private static final String ENCHANTMENT_KEY = "enchantment";
	private static final String LEVEL_KEY = "level";
	private static final String CUSTOM_BOOK_KEY = "custom_book";
	private static final String MELON_RIND_KEY = "melon_rind";
	private static final String MEATROOT_KEY = "meatroot";
	private static final String XP_ESSENCE_KEY = "xp_essence";
	private static final String FARMERS_BAG_KEY = "farmers_bag";
	private static final String MYSTERIOUS_JOURNAL_KEY = "mysterious_journal";
	private static final String STRETCHED_HIDE_KEY = "stretched_hide";
	private static final String CONDENSED_BONE_MEAL_KEY = "condensed_bone_meal";

	private CroppyEnchantmentUtil() {
	}

	public static ItemStack createBook(HolderLookup.Provider lookupProvider, String enchantmentId, int level) {
		FPlusEnchantment enchantment = FPlusEnchantment.fromCommandName(enchantmentId);
		ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
		setBookData(stack, enchantment, level);
		syncBookDisplay(stack);
		return stack;
	}

	public static ItemStack createMelonRind(int count) {
		ItemStack stack = new ItemStack(Items.GREEN_DYE, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(MELON_RIND_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Melon Rind")
			.withStyle(style -> style.withColor(ChatFormatting.DARK_PURPLE).withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("This doesn't look too tasty...", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	public static ItemStack createMeatroot(int count) {
		ItemStack stack = new ItemStack(Items.FERMENTED_SPIDER_EYE, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(MEATROOT_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Meatroot")
			.withStyle(style -> style.withColor(ChatFormatting.DARK_PURPLE).withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("Should've added this to the stew...", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	public static ItemStack createXpEssence(int count) {
		ItemStack stack = new ItemStack(Items.SLIME_BALL, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(XP_ESSENCE_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("XP Essence")
			.withStyle(style -> style.withColor(ChatFormatting.BLUE).withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("You feel the power surging through the air...", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	public static ItemStack createFarmersBag(int count) {
		ItemStack stack = new ItemStack(Items.LIME_BUNDLE, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(FARMERS_BAG_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Farmer's Bag")
			.withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
		stack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		return stack;
	}

	public static ItemStack createMysteriousJournal(int count) {
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(MYSTERIOUS_JOURNAL_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("Mysterious Journal: 01"),
			"Unknown",
			0,
			List.of(
				bookPage("""
					Page 1
					
					The young think a field is
					only seed and rain.
					They are wrong.
					
					Iron remembers.
					Gold encourages.
					Magic lingers in the
					furrows long after the
					hand has passed.
					"""),
				bookPage("""
					If your tools begin to
					feel... different...
					watch the harvest
					closely.
					"""),
				bookPage("""
					Page 2
					
					Some blessings do not
					wake at planting, but
					at the moment of
					return.
					
					A patient hand may
					bring back more than it
					buried.
					"""),
				bookPage("""
					Not always.
					Not evenly.
					
					But enough to make a
					wise farmer smile and
					a fool call it luck.
					"""),
				bookPage("""
					Page 3
					
					I have seen certain
					tools stir life in
					crops before their
					proper hour.
					
					Not by much at first.
					More, perhaps, in
					richer hands.
					
					Wheat answers gently.
					Other plants keep their
					own counsel.
					"""),
				bookPage("""
					Page 4
					
					There are harvests that
					seem cleaner than
					others.
					
					Fewer wasted stalks.
					Fewer empty pulls.
					More of the useful part
					finding its way into
					the basket.
					"""),
				bookPage("""
					Whether this is skill,
					enchantment, or favor
					from the soil, I leave
					to better minds.
					"""),
				bookPage("""
					Page 5
					
					Some effects seem
					strongest when the
					reaper works in rhythm.
					
					Row after row.
					Dawn to dusk.
					
					Break the pattern, and
					the field grows quiet
					again.
					"""),
				bookPage("""
					Page 6
					
					I once lent an
					enchanted hoe to a boy
					who swore the land
					around him felt wider.
					
					Said he worked faster
					without moving faster.
					"""),
				bookPage("""
					I told him to stop
					talking nonsense.
					
					Still, he filled three
					chests before noon.
					"""),
				bookPage("""
					Page 7
					
					Mind this above all:
					
					Not every enchantment
					speaks in sparks or
					sound.
					
					Some only reveal
					themselves to those who
					compare one harvest to
					the next.
					"""),
				bookPage("""
					Page 8
					
					So keep count.
					Watch the seed.
					Watch the growth.
					Watch what returns to
					your hand.
					
					The earth tells the
					truth eventually.
					""")
			),
			true));
		return stack;
	}

	public static ItemStack createStretchedHide(int count) {
		ItemStack stack = new ItemStack(Items.RABBIT_HIDE, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(STRETCHED_HIDE_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Stretched Hide")
			.withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("This looks like a sturdy material...", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	public static ItemStack createCondensedBoneMeal(int count) {
		ItemStack stack = new ItemStack(Items.BONE_BLOCK, count);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(CONDENSED_BONE_MEAL_KEY, true);
			tag.put(FPLUS_KEY, fplusTag);
		});
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Condensed Bone Meal")
			.withStyle(style -> style.withColor(ChatFormatting.BLUE).withItalic(false)));
		return stack;
	}

	public static ItemStack createCroppyBook(HolderLookup.Provider lookupProvider, int level) {
		return createBook(lookupProvider, FPlusEnchantment.CROPPY.commandName(), level);
	}

	public static void syncBookDisplay(ItemStack stack) {
		FPlusEnchantment enchantment = getBookEnchantment(stack);
		int level = getBookLevel(stack);
		boolean generatedDisplay = hasGeneratedDisplay(stack);

		if (enchantment == null || level <= 0) {
			if (generatedDisplay) {
				clearGeneratedDisplay(stack);
			}

			return;
		}

		stack.set(DataComponents.ITEM_NAME, Component.translatable(Items.ENCHANTED_BOOK.getDescriptionId()));
		stack.set(DataComponents.LORE, new ItemLore(enchantment.bookLore(level)));
		markGeneratedDisplay(stack, BOOK_DISPLAY_TYPE, enchantment.bookLore(level).size());
	}

	public static boolean shouldPreventTrample(ServerLevel level, Player player) {
		int croppyLevel = getAppliedLevel(player.getItemBySlot(EquipmentSlot.FEET), FPlusEnchantment.CROPPY);
		if (croppyLevel <= 0) {
			return false;
		}

		if (croppyLevel >= 4) {
			return true;
		}

		return level.getRandom().nextInt(4) < croppyLevel;
	}

	public static int getReplenishLevel(ItemStack stack) {
		return getAppliedLevel(stack, FPlusEnchantment.REPLENISH);
	}

	public static int getGentleLevel(ItemStack stack) {
		return getAppliedLevel(stack, FPlusEnchantment.GENTLE);
	}

	public static int getWisdomLevel(ItemStack stack) {
		return getAppliedLevel(stack, FPlusEnchantment.WISDOM);
	}

	public static int getTotalHeartyLevel(Player player) {
		return getAppliedLevel(player.getItemBySlot(EquipmentSlot.HEAD), FPlusEnchantment.HEARTY)
			+ getAppliedLevel(player.getItemBySlot(EquipmentSlot.CHEST), FPlusEnchantment.HEARTY)
			+ getAppliedLevel(player.getItemBySlot(EquipmentSlot.LEGS), FPlusEnchantment.HEARTY)
			+ getAppliedLevel(player.getItemBySlot(EquipmentSlot.FEET), FPlusEnchantment.HEARTY);
	}

	public static boolean isMeatroot(ItemStack stack) {
		return hasFPlusFlag(stack, MEATROOT_KEY);
	}

	public static boolean isMelonRind(ItemStack stack) {
		return hasFPlusFlag(stack, MELON_RIND_KEY);
	}

	public static boolean isXpEssence(ItemStack stack) {
		return hasFPlusFlag(stack, XP_ESSENCE_KEY);
	}

	public static boolean isFarmersBag(ItemStack stack) {
		return stack.is(Items.LIME_BUNDLE) && hasFPlusFlag(stack, FARMERS_BAG_KEY);
	}

	public static boolean isMysteriousJournal(ItemStack stack) {
		return stack.is(Items.WRITTEN_BOOK) && hasFPlusFlag(stack, MYSTERIOUS_JOURNAL_KEY);
	}

	public static boolean isStretchedHide(ItemStack stack) {
		return stack.is(Items.RABBIT_HIDE) && hasFPlusFlag(stack, STRETCHED_HIDE_KEY);
	}

	public static boolean isCondensedBoneMeal(ItemStack stack) {
		return stack.is(Items.BONE_BLOCK) && hasFPlusFlag(stack, CONDENSED_BONE_MEAL_KEY);
	}

	public static boolean isBook(ItemStack stack, String enchantmentId, int level) {
		FPlusEnchantment enchantment = getBookEnchantment(stack);
		return enchantment != null
			&& enchantment.commandName().equals(FPlusEnchantment.fromCommandName(enchantmentId).commandName())
			&& getBookLevel(stack) == level;
	}

	public static boolean isKnownEnchantment(String enchantmentId) {
		return FPlusEnchantment.fromCommandNameOrNull(enchantmentId) != null;
	}

	public static String normalizedEnchantmentName(String enchantmentId) {
		return FPlusEnchantment.fromCommandName(enchantmentId).commandName();
	}

	public static String displayName(String enchantmentId, int level) {
		FPlusEnchantment enchantment = FPlusEnchantment.fromCommandName(enchantmentId);
		return enchantment.displayName(level);
	}

	public static int maxLevel(String enchantmentId) {
		return FPlusEnchantment.fromCommandName(enchantmentId).maxLevel();
	}

	public static boolean usesCustomAnvil(ItemStack left, ItemStack right) {
		return isFPlusBook(left) || isFPlusBook(right);
	}

	public static ItemStack createAnvilResult(ItemStack left, ItemStack right) {
		if (left.isEmpty() || right.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (isFPlusBook(left) && isFPlusBook(right)) {
			return combineBooks(left, right);
		}

		if (isFPlusBook(right)) {
			return applyBookToItem(left, right);
		}

		return ItemStack.EMPTY;
	}

	private static ItemStack combineBooks(ItemStack left, ItemStack right) {
		FPlusEnchantment leftEnchantment = getBookEnchantment(left);
		FPlusEnchantment rightEnchantment = getBookEnchantment(right);
		if (leftEnchantment == null || leftEnchantment != rightEnchantment) {
			return ItemStack.EMPTY;
		}

		int combinedLevel = combineLevels(getBookLevel(left), getBookLevel(right), leftEnchantment.maxLevel());
		if (combinedLevel <= Math.max(getBookLevel(left), getBookLevel(right))) {
			return ItemStack.EMPTY;
		}

		ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
		setBookData(result, leftEnchantment, combinedLevel);
		syncBookDisplay(result);
		return result;
	}

	private static ItemStack applyBookToItem(ItemStack target, ItemStack book) {
		FPlusEnchantment enchantment = getBookEnchantment(book);
		int bookLevel = getBookLevel(book);
		if (enchantment == null || bookLevel <= 0 || !enchantment.supports(target)) {
			return ItemStack.EMPTY;
		}

		int currentLevel = getAppliedLevel(target, enchantment);
		int combinedLevel = combineLevels(currentLevel, bookLevel, enchantment.maxLevel());
		if (combinedLevel <= currentLevel) {
			return ItemStack.EMPTY;
		}

		ItemStack result = target.copy();
		setAppliedLevel(result, enchantment, combinedLevel);
		result.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		syncAppliedDisplay(result);
		return result;
	}

	private static int combineLevels(int leftLevel, int rightLevel, int maxLevel) {
		if (leftLevel <= 0) {
			return Math.min(rightLevel, maxLevel);
		}

		if (leftLevel == rightLevel) {
			return Math.min(leftLevel + 1, maxLevel);
		}

		return Math.min(Math.max(leftLevel, rightLevel), maxLevel);
	}

	private static int getBookLevel(ItemStack stack) {
		FPlusEnchantment enchantment = getBookEnchantment(stack);
		if (enchantment == null) {
			return 0;
		}

		return getBookTag(stack)
			.map(bookTag -> bookTag.getIntOr(LEVEL_KEY, 0))
			.orElse(0);
	}

	private static FPlusEnchantment getBookEnchantment(ItemStack stack) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return null;
		}

		return getBookTag(stack)
			.map(bookTag -> FPlusEnchantment.fromCommandNameOrNull(bookTag.getStringOr(ENCHANTMENT_KEY, "")))
			.orElse(null);
	}

	private static boolean isFPlusBook(ItemStack stack) {
		return getBookEnchantment(stack) != null && getBookLevel(stack) > 0;
	}

	private static int getAppliedLevel(ItemStack stack, FPlusEnchantment enchantment) {
		return getAppliedTag(stack)
			.map(appliedTag -> appliedTag.getIntOr(enchantment.commandName(), 0))
			.orElse(0);
	}

	private static Optional<CompoundTag> getBookTag(ItemStack stack) {
		return getFPlusTag(stack).flatMap(tag -> tag.getCompound(BOOK_KEY));
	}

	private static Optional<CompoundTag> getAppliedTag(ItemStack stack) {
		return getFPlusTag(stack).flatMap(tag -> tag.getCompound(APPLIED_KEY));
	}

	private static Optional<CompoundTag> getFPlusTag(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return Optional.empty();
		}

		return customData.copyTag().getCompound(FPLUS_KEY);
	}

	private static boolean hasFPlusFlag(ItemStack stack, String key) {
		return getFPlusTag(stack)
			.map(tag -> tag.getBooleanOr(key, false))
			.orElse(false);
	}

	private static void setBookData(ItemStack stack, FPlusEnchantment enchantment, int level) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			fplusTag.putBoolean(CUSTOM_BOOK_KEY, true);
			CompoundTag bookTag = new CompoundTag();
			bookTag.putString(ENCHANTMENT_KEY, enchantment.commandName());
			bookTag.putInt(LEVEL_KEY, level);
			fplusTag.put(BOOK_KEY, bookTag);
			tag.put(FPLUS_KEY, fplusTag);
		});
	}

	private static void setAppliedLevel(ItemStack stack, FPlusEnchantment enchantment, int level) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag fplusTag = tag.getCompoundOrEmpty(FPLUS_KEY);
			CompoundTag appliedTag = fplusTag.getCompoundOrEmpty(APPLIED_KEY);
			appliedTag.putInt(enchantment.commandName(), level);
			fplusTag.put(APPLIED_KEY, appliedTag);
			tag.put(FPLUS_KEY, fplusTag);
		});
	}

	private static Filterable<Component> bookPage(String text) {
		return Filterable.passThrough(Component.literal(text.stripIndent().trim()));
	}

	private static Component detailLine(String text, ChatFormatting color) {
		return Component.literal(text)
			.withStyle(style -> style.withColor(color).withItalic(false));
	}

	private static void syncAppliedDisplay(ItemStack stack) {
		List<Component> generatedLines = appliedLore(stack);
		if (generatedLines.isEmpty()) {
			clearGeneratedAppliedDisplay(stack);
			return;
		}

		List<Component> baseLines = baseLoreWithoutGeneratedAppliedLines(stack);
		baseLines.addAll(generatedLines);
		stack.set(DataComponents.LORE, new ItemLore(baseLines));
		markGeneratedDisplay(stack, APPLIED_DISPLAY_TYPE, generatedLines.size());
	}

	private static List<Component> appliedLore(ItemStack stack) {
		List<Component> lines = new java.util.ArrayList<>();
		for (FPlusEnchantment enchantment : FPlusEnchantment.values()) {
			int level = getAppliedLevel(stack, enchantment);
			if (level > 0) {
				lines.add(detailLine(enchantment.displayName(level), ChatFormatting.GRAY));
			}
		}

		return lines;
	}

	private static List<Component> baseLoreWithoutGeneratedAppliedLines(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		List<Component> lines = lore == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(lore.lines());
		int generatedCount = getGeneratedAppliedLineCount(stack);
		if (generatedCount <= 0 || generatedCount > lines.size()) {
			return lines;
		}

		return new java.util.ArrayList<>(lines.subList(0, lines.size() - generatedCount));
	}

	private static Component enchantmentName(String name, int level) {
		String suffix = level > 1 ? " " + toRoman(level) : "";
		return Component.literal(name + suffix)
			.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	private static String toRoman(int level) {
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			case 9 -> "IX";
			case 10 -> "X";
			default -> Integer.toString(level);
		};
	}

	private static void markGeneratedDisplay(ItemStack stack, String displayType, int generatedLineCount) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag marker = new CompoundTag();
			marker.putBoolean(GENERATED_KEY, true);
			marker.putString(DISPLAY_TYPE_KEY, displayType);
			marker.putInt(GENERATED_LINE_COUNT_KEY, generatedLineCount);
			tag.put(DISPLAY_MARKER_KEY, marker);
		});
	}

	private static boolean hasGeneratedDisplay(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return false;
		}

		return customData.copyTag()
			.getCompound(DISPLAY_MARKER_KEY)
			.map(marker -> marker.getBooleanOr(GENERATED_KEY, false))
			.orElse(false);
	}

	private static void clearGeneratedDisplay(ItemStack stack) {
		stack.remove(DataComponents.ITEM_NAME);
		stack.remove(DataComponents.LORE);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(DISPLAY_MARKER_KEY));
	}

	private static void clearGeneratedAppliedDisplay(ItemStack stack) {
		List<Component> baseLines = baseLoreWithoutGeneratedAppliedLines(stack);
		if (baseLines.isEmpty()) {
			stack.remove(DataComponents.LORE);
		} else {
			stack.set(DataComponents.LORE, new ItemLore(baseLines));
		}

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(DISPLAY_MARKER_KEY));
	}

	private static int getGeneratedAppliedLineCount(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return 0;
		}

		return customData.copyTag()
			.getCompound(DISPLAY_MARKER_KEY)
			.filter(marker -> marker.getStringOr(DISPLAY_TYPE_KEY, "").equals(APPLIED_DISPLAY_TYPE))
			.map(marker -> marker.getIntOr(GENERATED_LINE_COUNT_KEY, 0))
			.orElse(0);
	}

	private enum FPlusEnchantment {
		CROPPY("croppy", "Croppy", 4) {
			@Override
			List<Component> bookLore(int level) {
				return List.of(
					enchantmentName(baseName(), level),
					detailLine("+" + level * 25 + "% Farmland Trample Protection", ChatFormatting.BLUE),
					detailLine("Boots Enchantment", ChatFormatting.DARK_GRAY)
				);
			}

			@Override
			boolean supports(ItemStack stack) {
				return stack.is(ItemTags.FOOT_ARMOR);
			}
		},
		HEARTY("hearty", "Hearty", 3) {
			@Override
			List<Component> bookLore(int level) {
				return List.of(
					enchantmentName(baseName(), level),
					detailLine("+" + (level * 0.25F) + " Hearts While Worn", ChatFormatting.BLUE),
					detailLine("Armor Enchantment", ChatFormatting.DARK_GRAY)
				);
			}

			@Override
			boolean supports(ItemStack stack) {
				return stack.is(ItemTags.HEAD_ARMOR)
					|| stack.is(ItemTags.CHEST_ARMOR)
					|| stack.is(ItemTags.LEG_ARMOR)
					|| stack.is(ItemTags.FOOT_ARMOR);
			}
		},
		WISDOM("wisdom", "Wisdom", 10) {
			@Override
			List<Component> bookLore(int level) {
				return List.of(
					enchantmentName(baseName(), level),
					detailLine("+" + level * 10 + "% chance for crops to drop 1 XP", ChatFormatting.BLUE),
					detailLine("Hoe / Axe Enchantment", ChatFormatting.DARK_GRAY)
				);
			}

			@Override
			boolean supports(ItemStack stack) {
				return stack.getItem() instanceof HoeItem || stack.getItem() instanceof AxeItem;
			}
		},
		REPLENISH("replenish", "Replenish", 1) {
			@Override
			List<Component> bookLore(int level) {
				return List.of(
					enchantmentName(baseName(), level),
					detailLine("Automatically replants harvested crops", ChatFormatting.BLUE),
					detailLine("Hoe Enchantment", ChatFormatting.DARK_GRAY)
				);
			}

			@Override
			boolean supports(ItemStack stack) {
				return stack.getItem() instanceof HoeItem;
			}
		},
		GENTLE("gentle", "Gentle", 1) {
			@Override
			List<Component> bookLore(int level) {
				return List.of(
					enchantmentName(baseName(), level),
					detailLine("Only breaks cocoa, melons, and pumpkins", ChatFormatting.BLUE),
					detailLine("Axe Enchantment", ChatFormatting.DARK_GRAY)
				);
			}

			@Override
			boolean supports(ItemStack stack) {
				return stack.getItem() instanceof AxeItem;
			}
		};

		private final String commandName;
		private final String displayName;
		private final int maxLevel;

		FPlusEnchantment(String commandName, String displayName, int maxLevel) {
			this.commandName = commandName;
			this.displayName = displayName;
			this.maxLevel = maxLevel;
		}

		abstract List<Component> bookLore(int level);

		abstract boolean supports(ItemStack stack);

		String commandName() {
			return commandName;
		}

		String baseName() {
			return displayName;
		}

		int maxLevel() {
			return maxLevel;
		}

		String displayName(int level) {
			return level > 1 ? displayName + " " + toRoman(level) : displayName;
		}

		static FPlusEnchantment fromCommandName(String enchantmentId) {
			FPlusEnchantment enchantment = fromCommandNameOrNull(enchantmentId);
			if (enchantment == null) {
				throw new IllegalArgumentException("Unknown Farming+ enchantment: " + enchantmentId);
			}

			return enchantment;
		}

		static FPlusEnchantment fromCommandNameOrNull(String enchantmentId) {
			for (FPlusEnchantment enchantment : values()) {
				if (enchantment.commandName.equalsIgnoreCase(enchantmentId)) {
					return enchantment;
				}
			}

			return null;
		}
	}
}
