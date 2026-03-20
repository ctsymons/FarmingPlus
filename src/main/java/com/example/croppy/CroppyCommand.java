package com.example.croppy;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;

public final class CroppyCommand {
	private static final int DEFAULT_LEVEL = 1;
	private static final int DEFAULT_AMOUNT = 1;
	private static final int MIN_LEVEL = 1;
	private static final int RECIPES_MENU_SIZE = 27;

	private CroppyCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("fplus")
				.executes(CroppyCommand::showHelp)
				.then(literal("item")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(argument("item", StringArgumentType.word())
						.suggests((context, builder) -> {
							for (String item : List.of("melon_rind", "meatroot", "xpessence", "farmers_bag", "mysterious_journal", "stretched_hide", "condensed_bonemeal")) {
								if (item.startsWith(builder.getRemainingLowerCase())) {
									builder.suggest(item);
								}
							}

							return builder.buildFuture();
						})
						.executes(context -> giveItem(context, DEFAULT_AMOUNT))
						.then(argument("amount", IntegerArgumentType.integer(1))
							.executes(context -> giveItem(context, IntegerArgumentType.getInteger(context, "amount"))))))
				.then(literal("recipes")
					.executes(CroppyCommand::openRecipes))
				.then(literal("help")
					.executes(CroppyCommand::showHelp)
					.then(literal("recipes")
						.executes(CroppyCommand::openRecipes))
					.then(literal("commands")
						.executes(CroppyCommand::showCommandsHelp))
					.then(literal("itemids")
						.executes(CroppyCommand::showItemIdsHelp))
					.then(literal("info")
						.executes(CroppyCommand::showAdditionalInfoHelp)))
				.then(enchantCommand("e")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
				.then(enchantCommand("enchant")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))))
		);
	}

	private static int showHelp(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.literal("Welcome to Farming+").withStyle(ChatFormatting.YELLOW), false);
		source.sendSuccess(Component::empty, false);
		source.sendSuccess(() -> helpButtons(source), false);
		return 1;
	}

	private static int showCommandsHelp(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.literal("Commands").withStyle(ChatFormatting.GOLD), false);

		if (!isGamemaster(source) || isSurvivalPlayer(source)) {
			source.sendSuccess(() -> Component.literal("/fplus help"), false);
			source.sendSuccess(() -> Component.literal("/fplus recipes"), false);
			return 1;
		}

		source.sendSuccess(() -> Component.literal("/fplus help"), false);
		source.sendSuccess(() -> Component.literal("/fplus recipes"), false);
		source.sendSuccess(() -> Component.literal("/fplus item <melon_rind|meatroot|xpessence|farmers_bag|mysterious_journal|stretched_hide|condensed_bonemeal> [amount]"), false);
		source.sendSuccess(() -> Component.literal("/fplus e <croppy|hearty|wisdom|replenish|gentle> [level]"), false);
		source.sendSuccess(() -> Component.literal("/fplus enchant <croppy|hearty|wisdom|replenish|gentle> [level]"), false);
		return 1;
	}

	private static int showItemIdsHelp(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.literal("Item Id's").withStyle(ChatFormatting.GOLD), false);
		source.sendSuccess(() -> Component.literal("Use /fplus e <croppy|hearty|wisdom|replenish|gentle> [level] for generated books."), false);
		source.sendSuccess(() -> Component.literal("Use /fplus item <melon_rind|meatroot|xpessence|farmers_bag|mysterious_journal|stretched_hide|condensed_bonemeal> [amount] for custom items."), false);
		source.sendSuccess(() -> Component.literal("Book data marker: minecraft:custom_data={fplus:{book:{enchantment:\"replenish\",level:1}}}"), false);
		return 1;
	}

	private static int showAdditionalInfoHelp(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.literal("Additional Info").withStyle(ChatFormatting.GOLD), false);
		source.sendSuccess(() -> Component.literal("Croppy applies to boots and protects farmland from trampling."), false);
		source.sendSuccess(() -> Component.literal("Hearty applies to armor and grants +0.25 hearts per level while worn."), false);
		source.sendSuccess(() -> Component.literal("Wisdom applies to hoes and axes and adds +10% harvest XP chance per level."), false);
		source.sendSuccess(() -> Component.literal("Replenish applies to hoes, replants mature crops, and blocks breaking immature crops."), false);
		source.sendSuccess(() -> Component.literal("Gentle applies to axes and only allows breaking cocoa beans, melons, and pumpkins."), false);
		return 1;
	}

	private static Component helpButtons(CommandSourceStack source) {
		if (isSurvivalPlayer(source)) {
			return button("Commands", "/fplus help commands", "Show Farming+ commands")
				.append(Component.literal(" "))
				.append(button("Recipes", "/fplus help recipes", "Open the Farming+ recipes menu"));
		}

		return button("Commands", "/fplus help commands", "Show Farming+ commands")
			.append(Component.literal(" "))
			.append(button("Recipes", "/fplus help recipes", "Open the Farming+ recipes menu"))
			.append(Component.literal(" "))
			.append(button("Item Id's", "/fplus help itemids", "Show Farming+ enchantment IDs"))
			.append(Component.literal(" "))
			.append(button("Additional Info", "/fplus help info", "Show Farming+ enchantment details"));
	}

	private static boolean isSurvivalPlayer(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player != null && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
	}

	private static boolean isGamemaster(CommandSourceStack source) {
		return !(source.permissions() instanceof LevelBasedPermissionSet levelBasedPermissionSet)
			|| levelBasedPermissionSet.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
	}

	private static MutableComponent button(String label, String command, String hoverText) {
		return Component.literal("[" + label + "]")
			.withStyle(style -> style
				.withColor(ChatFormatting.GOLD)
				.withBold(true)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
	}

	private static int openRecipes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		player.openMenu(new SimpleMenuProvider(
			(containerId, inventory, ignoredPlayer) -> new RootRecipesMenu(containerId, inventory, createRecipesMenu()),
			title("Farming+ Recipes")));
		return 1;
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> enchantCommand(String literalName) {
		return literal(literalName)
			.then(argument("enchant", StringArgumentType.word())
				.suggests((context, builder) -> {
					for (String enchantment : List.of("croppy", "replenish", "gentle", "hearty", "wisdom")) {
						if (enchantment.startsWith(builder.getRemainingLowerCase())) {
							builder.suggest(enchantment);
						}
					}

					return builder.buildFuture();
				})
				.executes(context -> giveBook(context, DEFAULT_LEVEL))
				.then(argument("level", IntegerArgumentType.integer(MIN_LEVEL, 10))
					.executes(context -> giveBook(context, IntegerArgumentType.getInteger(context, "level")))));
	}

	private static int giveItem(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
		String itemName = StringArgumentType.getString(context, "item");
		return switch (itemName.toLowerCase(java.util.Locale.ROOT)) {
			case "melon_rind" -> giveMelonRind(context.getSource(), amount);
			case "meatroot" -> giveMeatroot(context.getSource(), amount);
			case "xpessence" -> giveXpEssence(context.getSource(), amount);
			case "farmers_bag" -> giveFarmersBag(context.getSource(), amount);
			case "mysterious_journal" -> giveMysteriousJournal(context.getSource(), amount);
			case "stretched_hide" -> giveStretchedHide(context.getSource(), amount);
			case "condensed_bonemeal" -> giveCondensedBoneMeal(context.getSource(), amount);
			default -> {
				context.getSource().sendFailure(Component.literal("Unknown Farming+ item: " + itemName));
				yield 0;
			}
		};
	}

	private static int giveMelonRind(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.GREEN_DYE.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createMelonRind(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " melon rind."), false);
		return 1;
	}

	private static int giveMeatroot(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.FERMENTED_SPIDER_EYE.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createMeatroot(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " meatroot."), false);
		return 1;
	}

	private static int giveXpEssence(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.SLIME_BALL.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createXpEssence(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " XP Essence."), false);
		return 1;
	}

	private static int giveFarmersBag(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.LIME_BUNDLE.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createFarmersBag(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " Farmer's Bag."), false);
		return 1;
	}

	private static int giveMysteriousJournal(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.WRITTEN_BOOK.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createMysteriousJournal(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " Mysterious Journal: 01."), false);
		return 1;
	}

	private static int giveStretchedHide(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.RABBIT_HIDE.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createStretchedHide(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " Stretched Hide."), false);
		return 1;
	}

	private static int giveCondensedBoneMeal(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int remaining = amount;

		while (remaining > 0) {
			int stackSize = Math.min(remaining, Items.BONE_BLOCK.getDefaultMaxStackSize());
			player.getInventory().placeItemBackInInventory(CroppyEnchantmentUtil.createCondensedBoneMeal(stackSize));
			remaining -= stackSize;
		}

		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + amount + " Condensed Bone Meal."), false);
		return 1;
	}

	private static SimpleContainer createRecipesMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack customEnchantsBook = new ItemStack(Items.ENCHANTED_BOOK);
		customEnchantsBook.set(DataComponents.ITEM_NAME, Component.literal("Custom Enchants"));
		container.setItem(10, customEnchantsBook);
		ItemStack itemsButton = new ItemStack(Items.WHEAT);
		itemsButton.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		itemsButton.set(DataComponents.ITEM_NAME, Component.literal("Items")
			.withStyle(style -> style.withItalic(false)));
		container.setItem(13, itemsButton);
		container.setItem(16, createRareDropsCategoryButton());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createCustomEnchantsMenu() {
		SimpleContainer container = new SimpleContainer(27);
		container.setItem(0, CroppyEnchantmentUtil.createBook(null, "gentle", 1));
		container.setItem(1, CroppyEnchantmentUtil.createBook(null, "replenish", 1));
		container.setItem(2, CroppyEnchantmentUtil.createBook(null, "hearty", 1));
		container.setItem(3, CroppyEnchantmentUtil.createBook(null, "wisdom", 1));
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createItemsMenu() {
		SimpleContainer container = new SimpleContainer(27);
		container.setItem(0, CroppyEnchantmentUtil.createXpEssence(1));
		container.setItem(1, CroppyEnchantmentUtil.createFarmersBag(1));
		container.setItem(2, createStretchedHideGuiItem());
		container.setItem(3, CroppyEnchantmentUtil.createCondensedBoneMeal(1));
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createStretchedHideRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack leather = new ItemStack(Items.LEATHER, 2);
		container.setItem(3, leather.copy());
		container.setItem(4, leather.copy());
		container.setItem(5, leather.copy());
		container.setItem(12, leather.copy());
		container.setItem(14, leather.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, leather.copy());
		container.setItem(22, leather.copy());
		container.setItem(23, leather.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createFarmersBagRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack hayBale = new ItemStack(Items.HAY_BLOCK, 8);
		container.setItem(3, hayBale.copy());
		container.setItem(4, hayBale.copy());
		container.setItem(5, hayBale.copy());
		container.setItem(12, hayBale.copy());
		container.setItem(13, createStretchedHideGuiItem());
		container.setItem(14, hayBale.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, hayBale.copy());
		container.setItem(22, hayBale.copy());
		container.setItem(23, hayBale.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createRareDropsMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack croppyBook = CroppyEnchantmentUtil.createBook(null, "croppy", 1);
		croppyBook.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("Croppy I", ChatFormatting.GRAY),
			Component.literal(""),
			detailLine("1/1000 from breaking mature wheat, carrots, potatoes, or beetroots.", ChatFormatting.DARK_GRAY)
		)));
		container.setItem(0, croppyBook);
		container.setItem(1, withLoreLines(
			CroppyEnchantmentUtil.createMelonRind(1),
			"1/1000 from breaking melons."
		));
		container.setItem(2, withLoreLines(
			CroppyEnchantmentUtil.createMeatroot(1),
			"1/128 from breaking mature beetroot",
			"crops."
		));
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createXpEssenceRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack xpBottle = new ItemStack(Items.EXPERIENCE_BOTTLE, 2);
		container.setItem(3, xpBottle.copy());
		container.setItem(4, xpBottle.copy());
		container.setItem(5, xpBottle.copy());
		container.setItem(12, xpBottle.copy());
		container.setItem(13, xpBottle.copy());
		container.setItem(14, xpBottle.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, xpBottle.copy());
		container.setItem(22, xpBottle.copy());
		container.setItem(23, xpBottle.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createCondensedBoneMealRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 64);
		container.setItem(3, boneMeal.copy());
		container.setItem(4, boneMeal.copy());
		container.setItem(5, boneMeal.copy());
		container.setItem(12, boneMeal.copy());
		container.setItem(14, boneMeal.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, boneMeal.copy());
		container.setItem(22, boneMeal.copy());
		container.setItem(23, boneMeal.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createGentleRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack melonRind = createMelonRindGuiItem();
		ItemStack silkTouchBook = createSilkTouchBook();
		container.setItem(3, melonRind.copy());
		container.setItem(4, silkTouchBook.copy());
		container.setItem(5, melonRind.copy());
		container.setItem(12, silkTouchBook.copy());
		container.setItem(13, new ItemStack(Items.BOOK));
		container.setItem(14, silkTouchBook.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, melonRind.copy());
		container.setItem(22, silkTouchBook.copy());
		container.setItem(23, melonRind.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createReplenishRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack condensedBoneMeal = CroppyEnchantmentUtil.createCondensedBoneMeal(1);
		ItemStack croppyBook = CroppyEnchantmentUtil.createBook(null, "croppy", 1);
		ItemStack gentleBook = CroppyEnchantmentUtil.createBook(null, "gentle", 1);
		container.setItem(3, condensedBoneMeal.copy());
		container.setItem(4, gentleBook.copy());
		container.setItem(5, condensedBoneMeal.copy());
		container.setItem(12, croppyBook.copy());
		container.setItem(13, new ItemStack(Items.BOOK));
		container.setItem(14, croppyBook.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, condensedBoneMeal.copy());
		container.setItem(22, gentleBook.copy());
		container.setItem(23, condensedBoneMeal.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createHeartyRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack meatroot = createMeatrootGuiItem();
		ItemStack protectionBook = createDisplayEnchantedBook("Protection III");
		ItemStack healthPotion = createInstantHealthPotion();
		container.setItem(3, meatroot.copy());
		container.setItem(4, healthPotion.copy());
		container.setItem(5, meatroot.copy());
		container.setItem(12, protectionBook.copy());
		container.setItem(13, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
		container.setItem(14, protectionBook.copy());
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, meatroot.copy());
		container.setItem(22, healthPotion.copy());
		container.setItem(23, meatroot.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static SimpleContainer createWisdomRecipeMenu() {
		SimpleContainer container = new SimpleContainer(27);
		ItemStack xpEssence = createXpEssenceGuiLinkItem();
		container.setItem(3, xpEssence.copy());
		container.setItem(4, new ItemStack(Items.SCULK));
		container.setItem(5, xpEssence.copy());
		container.setItem(12, new ItemStack(Items.SCULK));
		container.setItem(13, new ItemStack(Items.EMERALD_BLOCK));
		container.setItem(14, new ItemStack(Items.SCULK));
		container.setItem(18, createMenuButton(Items.ARROW, "Back"));
		container.setItem(21, xpEssence.copy());
		container.setItem(22, new ItemStack(Items.SCULK));
		container.setItem(23, xpEssence.copy());
		container.setItem(26, createMenuButton(Items.BARRIER, "Close"));
		return container;
	}

	private static ItemStack createSilkTouchBook() {
		return createDisplayEnchantedBook("Silk Touch");
	}

	private static ItemStack createDisplayEnchantedBook(String enchantmentName) {
		ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
		stack.set(DataComponents.ITEM_NAME, Component.translatable(Items.ENCHANTED_BOOK.getDescriptionId())
			.withStyle(style -> style.withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal(enchantmentName)
				.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false))
		)));
		return stack;
	}

	private static ItemStack createInstantHealthPotion() {
		ItemStack stack = new ItemStack(Items.LINGERING_POTION);
		stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withPotion(Potions.STRONG_HEALING));
		return stack;
	}

	private static ItemStack createStretchedHideGuiItem() {
		ItemStack stack = CroppyEnchantmentUtil.createStretchedHide(1);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("This looks like a sturdy material...", ChatFormatting.DARK_GRAY),
			detailLine("Right Click to view recipe", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	private static ItemStack createXpEssenceGuiLinkItem() {
		ItemStack stack = CroppyEnchantmentUtil.createXpEssence(1);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("You feel the power surging through the air...", ChatFormatting.DARK_GRAY),
			detailLine("Right Click to view recipe", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	private static ItemStack createMelonRindGuiItem() {
		ItemStack stack = CroppyEnchantmentUtil.createMelonRind(1);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("This doesn't look too tasty...", ChatFormatting.DARK_GRAY),
			detailLine("Right Click to view source", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	private static ItemStack createMeatrootGuiItem() {
		ItemStack stack = CroppyEnchantmentUtil.createMeatroot(1);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("Should've added this to the stew...", ChatFormatting.DARK_GRAY),
			detailLine("Right Click to view source", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	private static ItemStack createMenuButton(net.minecraft.world.item.Item item, String name) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.ITEM_NAME, Component.literal(name)
			.withStyle(style -> style.withItalic(false)));
		return stack;
	}

	private static ItemStack createRareDropsCategoryButton() {
		ItemStack stack = new ItemStack(Items.HAY_BLOCK);
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Farming Rare Drops!")
			.withStyle(style -> style.withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			detailLine("Assorted uncraftable rare drops.", ChatFormatting.DARK_GRAY)
		)));
		return stack;
	}

	private static ItemStack withLoreLines(ItemStack stack, String... lines) {
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			java.util.Arrays.stream(lines)
				.map(line -> detailLine(line, ChatFormatting.DARK_GRAY))
				.toArray(Component[]::new)
		)));
		return stack;
	}

	private static Component detailLine(String text, ChatFormatting color) {
		return Component.literal(text)
			.withStyle(style -> style.withColor(color).withItalic(false));
	}

	private static Component title(String text) {
		return Component.literal(text)
			.withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false));
	}

	private static class LockedRecipesMenu extends ChestMenu {
		private LockedRecipesMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.QUICK_CRAFT || slotId < RECIPES_MENU_SIZE) {
				return;
			}

			super.clicked(slotId, button, clickType, player);
		}

		@Override
		public ItemStack quickMoveStack(Player player, int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
			return slot.index >= RECIPES_MENU_SIZE && super.canTakeItemForPickAll(stack, slot);
		}
	}

	private static final class RootRecipesMenu extends LockedRecipesMenu {
		private static final int CUSTOM_ENCHANTS_SLOT = 10;
		private static final int ITEMS_SLOT = 13;
		private static final int RARE_DROPS_SLOT = 16;
		private static final int CLOSE_SLOT = 26;

		private RootRecipesMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == CUSTOM_ENCHANTS_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new CustomEnchantsMenu(containerId, inventory, createCustomEnchantsMenu()),
						title("Custom Enchants")));
					return;
				}

				if (slotId == ITEMS_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new ItemsMenu(containerId, inventory, createItemsMenu()),
						title("Items")));
					return;
				}

				if (slotId == RARE_DROPS_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new RareDropsMenu(containerId, inventory, createRareDropsMenu()),
						title("Farming Rare Drops!")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class CustomEnchantsMenu extends LockedRecipesMenu {
		private static final int GENTLE_SLOT = 0;
		private static final int REPLENISH_SLOT = 1;
		private static final int HEARTY_SLOT = 2;
		private static final int WISDOM_SLOT = 3;
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private CustomEnchantsMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == GENTLE_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new GentleRecipeMenu(containerId, inventory, createGentleRecipeMenu()),
						title("Gentle Recipe")));
					return;
				}

				if (slotId == REPLENISH_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new ReplenishRecipeMenu(containerId, inventory, createReplenishRecipeMenu()),
						title("Replenish Recipe")));
					return;
				}

				if (slotId == HEARTY_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new HeartyRecipeMenu(containerId, inventory, createHeartyRecipeMenu()),
						title("Hearty Recipe")));
					return;
				}

				if (slotId == WISDOM_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new WisdomRecipeMenu(containerId, inventory, createWisdomRecipeMenu()),
						title("Wisdom Recipe")));
					return;
				}

				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new RootRecipesMenu(containerId, inventory, createRecipesMenu()),
						title("Farming+ Recipes")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class GentleRecipeMenu extends LockedRecipesMenu {
		private static final int MELON_RIND_TOP_LEFT_SLOT = 3;
		private static final int MELON_RIND_TOP_RIGHT_SLOT = 5;
		private static final int MELON_RIND_BOTTOM_LEFT_SLOT = 21;
		private static final int MELON_RIND_BOTTOM_RIGHT_SLOT = 23;
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private GentleRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (button == 1 && (slotId == MELON_RIND_TOP_LEFT_SLOT
					|| slotId == MELON_RIND_TOP_RIGHT_SLOT
					|| slotId == MELON_RIND_BOTTOM_LEFT_SLOT
					|| slotId == MELON_RIND_BOTTOM_RIGHT_SLOT)) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new RareDropsMenu(containerId, inventory, createRareDropsMenu()),
						title("Farming Rare Drops!")));
					return;
				}

				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new CustomEnchantsMenu(containerId, inventory, createCustomEnchantsMenu()),
						title("Custom Enchants")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class HeartyRecipeMenu extends LockedRecipesMenu {
		private static final int MEATROOT_TOP_LEFT_SLOT = 3;
		private static final int MEATROOT_TOP_RIGHT_SLOT = 5;
		private static final int MEATROOT_BOTTOM_LEFT_SLOT = 21;
		private static final int MEATROOT_BOTTOM_RIGHT_SLOT = 23;
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private HeartyRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (button == 1 && (slotId == MEATROOT_TOP_LEFT_SLOT
					|| slotId == MEATROOT_TOP_RIGHT_SLOT
					|| slotId == MEATROOT_BOTTOM_LEFT_SLOT
					|| slotId == MEATROOT_BOTTOM_RIGHT_SLOT)) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new RareDropsMenu(containerId, inventory, createRareDropsMenu()),
						title("Farming Rare Drops!")));
					return;
				}

				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new CustomEnchantsMenu(containerId, inventory, createCustomEnchantsMenu()),
						title("Custom Enchants")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class ReplenishRecipeMenu extends LockedRecipesMenu {
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private ReplenishRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new CustomEnchantsMenu(containerId, inventory, createCustomEnchantsMenu()),
						title("Custom Enchants")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class WisdomRecipeMenu extends LockedRecipesMenu {
		private static final int XP_ESSENCE_SLOT_TOP_LEFT = 3;
		private static final int XP_ESSENCE_SLOT_TOP_RIGHT = 5;
		private static final int XP_ESSENCE_SLOT_BOTTOM_LEFT = 21;
		private static final int XP_ESSENCE_SLOT_BOTTOM_RIGHT = 23;
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private WisdomRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == XP_ESSENCE_SLOT_TOP_LEFT
					|| slotId == XP_ESSENCE_SLOT_TOP_RIGHT
					|| slotId == XP_ESSENCE_SLOT_BOTTOM_LEFT
					|| slotId == XP_ESSENCE_SLOT_BOTTOM_RIGHT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new XpEssenceRecipeMenu(containerId, inventory, createXpEssenceRecipeMenu()),
						title("XP Essence Recipe")));
					return;
				}

				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new CustomEnchantsMenu(containerId, inventory, createCustomEnchantsMenu()),
						title("Custom Enchants")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class ItemsMenu extends LockedRecipesMenu {
		private static final int XP_ESSENCE_SLOT = 0;
		private static final int FARMERS_BAG_SLOT = 1;
		private static final int STRETCHED_HIDE_SLOT = 2;
		private static final int CONDESED_BONE_MEAL_SLOT = 3;
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private ItemsMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == XP_ESSENCE_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new XpEssenceRecipeMenu(containerId, inventory, createXpEssenceRecipeMenu()),
						title("XP Essence Recipe")));
					return;
				}

				if (slotId == FARMERS_BAG_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new FarmersBagRecipeMenu(containerId, inventory, createFarmersBagRecipeMenu()),
						title("Farmer's Bag Recipe")));
					return;
				}

				if (slotId == STRETCHED_HIDE_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new StretchedHideRecipeMenu(containerId, inventory, createStretchedHideRecipeMenu()),
						title("Stretched Hide Recipe")));
					return;
				}

				if (slotId == CONDESED_BONE_MEAL_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new CondensedBoneMealRecipeMenu(containerId, inventory, createCondensedBoneMealRecipeMenu()),
						title("Condensed Bone Meal Recipe")));
					return;
				}

				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new RootRecipesMenu(containerId, inventory, createRecipesMenu()),
						title("Farming+ Recipes")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class RareDropsMenu extends LockedRecipesMenu {
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private RareDropsMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new RootRecipesMenu(containerId, inventory, createRecipesMenu()),
						title("Farming+ Recipes")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class XpEssenceRecipeMenu extends LockedRecipesMenu {
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private XpEssenceRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new ItemsMenu(containerId, inventory, createItemsMenu()),
						title("Items")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class CondensedBoneMealRecipeMenu extends LockedRecipesMenu {
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private CondensedBoneMealRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new ItemsMenu(containerId, inventory, createItemsMenu()),
						title("Items")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class StretchedHideRecipeMenu extends LockedRecipesMenu {
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private StretchedHideRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new ItemsMenu(containerId, inventory, createItemsMenu()),
						title("Items")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static final class FarmersBagRecipeMenu extends LockedRecipesMenu {
		private static final int STRETCHED_HIDE_SLOT = 13;
		private static final int BACK_SLOT = 18;
		private static final int CLOSE_SLOT = 26;

		private FarmersBagRecipeMenu(int containerId, Inventory inventory, SimpleContainer container) {
			super(containerId, inventory, container);
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (clickType == ClickType.PICKUP && player instanceof ServerPlayer serverPlayer) {
				if (slotId == STRETCHED_HIDE_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new StretchedHideRecipeMenu(containerId, inventory, createStretchedHideRecipeMenu()),
						title("Stretched Hide Recipe")));
					return;
				}

				if (slotId == BACK_SLOT) {
					serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignoredPlayer) -> new ItemsMenu(containerId, inventory, createItemsMenu()),
						title("Items")));
					return;
				}

				if (slotId == CLOSE_SLOT) {
					serverPlayer.closeContainer();
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}
	}

	private static int giveBook(CommandContext<CommandSourceStack> context, int level) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String enchant = StringArgumentType.getString(context, "enchant");

		if (!CroppyEnchantmentUtil.isKnownEnchantment(enchant)) {
			source.sendFailure(Component.literal("Unknown Farming+ enchantment: " + enchant));
			return 0;
		}

		String normalizedEnchant = CroppyEnchantmentUtil.normalizedEnchantmentName(enchant);
		int maxLevel = CroppyEnchantmentUtil.maxLevel(normalizedEnchant);
		if (level > maxLevel) {
			source.sendFailure(Component.literal(CroppyEnchantmentUtil.displayName(normalizedEnchant, maxLevel) + " is the maximum level."));
			return 0;
		}

		ItemStack book = CroppyEnchantmentUtil.createBook(source.registryAccess(), normalizedEnchant, level);

		player.getInventory().placeItemBackInInventory(book);
		source.sendSuccess(() -> Component.literal("Gave " + source.getTextName() + " " + CroppyEnchantmentUtil.displayName(normalizedEnchant, level) + "."), false);
		return 1;
	}
}
