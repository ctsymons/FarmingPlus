package com.example.croppy;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

public final class FarmersBagFeature {
	private static final int BAG_ROWS = 3;
	private static final int BAG_SIZE = BAG_ROWS * 9;
	private static final int PLAYER_INVENTORY_START = BAG_SIZE;
	private static final int HOTBAR_START = PLAYER_INVENTORY_START + 27;
	private static final int OFFHAND_SWAP_BUTTON = 40;

	private FarmersBagFeature() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!CroppyEnchantmentUtil.isFarmersBag(stack)) {
				return InteractionResult.PASS;
			}

			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			ServerPlayer serverPlayer = (ServerPlayer) player;
			if (player.isShiftKeyDown()) {
				dropBagContents(serverPlayer, hand);
				return InteractionResult.SUCCESS;
			}

			serverPlayer.openMenu(new SimpleMenuProvider(
				(containerId, inventory, ignoredPlayer) -> new FarmersBagMenu(containerId, inventory, serverPlayer, hand),
				title("Farmer's Bag")));
			return InteractionResult.SUCCESS;
		});

		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!player.isShiftKeyDown() || !CroppyEnchantmentUtil.isFarmersBag(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}

			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof ComposterBlock) {
				if (level.isClientSide()) {
					return InteractionResult.SUCCESS;
				}

				transferBagContentsToComposter((ServerPlayer) player, hand, (ServerLevel) level, pos, state);
				return InteractionResult.SUCCESS;
			}

			Container container = HopperBlockEntity.getContainerAt(level, pos);
			if (container == null) {
				return InteractionResult.PASS;
			}

			if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity && !blockEntity.canOpen(player)) {
				return InteractionResult.SUCCESS;
			}

			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			transferBagContentsToContainer((ServerPlayer) player, hand, container);
			return InteractionResult.SUCCESS;
		});
	}

	private static Component title(String text) {
		return Component.literal(text)
			.withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false));
	}

	private static boolean isAllowedContent(ItemStack stack) {
		return stack.is(Items.WHEAT_SEEDS)
			|| stack.is(Items.BEETROOT_SEEDS)
			|| stack.is(Items.CARROT)
			|| stack.is(Items.POTATO)
			|| stack.is(Items.MELON_SEEDS)
			|| stack.is(Items.PUMPKIN_SEEDS)
			|| stack.is(Items.TORCHFLOWER_SEEDS)
			|| stack.is(Items.PITCHER_POD)
			|| stack.is(Items.WHEAT)
			|| stack.is(Items.BEETROOT)
			|| stack.is(Items.PUMPKIN)
			|| stack.is(Items.MELON_SLICE)
			|| stack.is(Items.TORCHFLOWER)
			|| stack.is(Items.SUGAR_CANE)
			|| CroppyEnchantmentUtil.isMeatroot(stack)
			|| CroppyEnchantmentUtil.isMelonRind(stack);
	}

	public static boolean isBagEligibleItem(ItemStack stack) {
		return isAllowedContent(stack);
	}

	public static List<ItemStack> snapshotInventory(Inventory inventory) {
		List<ItemStack> snapshot = new ArrayList<>(inventory.getContainerSize());
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			snapshot.add(inventory.getItem(slot).copy());
		}

		return snapshot;
	}

	public static void movePickedUpItemsToBag(ServerPlayer player, List<ItemStack> previousInventory) {
		if (player.containerMenu instanceof FarmersBagMenu) {
			return;
		}

		Inventory inventory = player.getInventory();
		if (previousInventory == null || previousInventory.size() != inventory.getContainerSize()) {
			return;
		}

		List<ItemStack> bags = findFarmersBags(inventory);
		if (bags.isEmpty()) {
			return;
		}

		boolean changed = false;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || CroppyEnchantmentUtil.isFarmersBag(stack) || !isAllowedContent(stack)) {
				continue;
			}

			ItemStack previousStack = previousInventory.get(slot);
			ItemStack pickedUpStack = pickedUpDelta(stack, previousStack);
			if (pickedUpStack.isEmpty()) {
				continue;
			}

			int pickedUpCount = pickedUpStack.getCount();
			if (!tryStoreInBags(pickedUpStack, bags)) {
				continue;
			}

			int movedIntoBag = pickedUpCount - pickedUpStack.getCount();
			if (movedIntoBag <= 0) {
				continue;
			}

			stack.shrink(movedIntoBag);
			if (stack.isEmpty()) {
				inventory.setItem(slot, ItemStack.EMPTY);
			} else {
				inventory.setItem(slot, stack);
			}

			changed = true;
		}

		if (changed) {
			inventory.setChanged();
			player.containerMenu.broadcastChanges();
		}
	}

	private static ItemStack pickedUpDelta(ItemStack currentStack, ItemStack previousStack) {
		if (currentStack.isEmpty() || !isAllowedContent(currentStack)) {
			return ItemStack.EMPTY;
		}

		if (previousStack.isEmpty()) {
			return currentStack.copy();
		}

		if (!ItemStack.isSameItemSameComponents(currentStack, previousStack)) {
			return currentStack.copy();
		}

		int delta = currentStack.getCount() - previousStack.getCount();
		if (delta <= 0) {
			return ItemStack.EMPTY;
		}

		return currentStack.copyWithCount(delta);
	}

	private static List<ItemStack> findFarmersBags(Inventory inventory) {
		List<ItemStack> bags = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (CroppyEnchantmentUtil.isFarmersBag(stack)) {
				bags.add(stack);
			}
		}

		return bags;
	}

	private static boolean tryStoreInBags(ItemStack sourceStack, List<ItemStack> bags) {
		boolean changed = false;
		for (ItemStack bag : bags) {
			if (sourceStack.isEmpty()) {
				break;
			}

			if (insertIntoBag(bag, sourceStack)) {
				changed = true;
			}
		}

		return changed;
	}

	private static boolean insertIntoBag(ItemStack bagStack, ItemStack sourceStack) {
		if (!CroppyEnchantmentUtil.isFarmersBag(bagStack) || sourceStack.isEmpty() || !isAllowedContent(sourceStack)) {
			return false;
		}

		ItemContainerContents contents = bagStack.get(DataComponents.CONTAINER);
		NonNullList<ItemStack> items = NonNullList.withSize(BAG_SIZE, ItemStack.EMPTY);
		if (contents != null) {
			contents.copyInto(items);
		}

		boolean changed = mergeIntoExistingSlots(items, sourceStack);
		if (!sourceStack.isEmpty()) {
			changed |= moveIntoEmptySlots(items, sourceStack);
		}

		if (changed) {
			bagStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
		}

		return changed;
	}

	private static boolean mergeIntoExistingSlots(NonNullList<ItemStack> bagItems, ItemStack sourceStack) {
		boolean changed = false;
		for (ItemStack bagItem : bagItems) {
			if (sourceStack.isEmpty()) {
				break;
			}

			if (bagItem.isEmpty() || !ItemStack.isSameItemSameComponents(bagItem, sourceStack)) {
				continue;
			}

			int space = bagItem.getMaxStackSize() - bagItem.getCount();
			if (space <= 0) {
				continue;
			}

			int moved = Math.min(space, sourceStack.getCount());
			bagItem.grow(moved);
			sourceStack.shrink(moved);
			changed = true;
		}

		return changed;
	}

	private static boolean moveIntoEmptySlots(NonNullList<ItemStack> bagItems, ItemStack sourceStack) {
		boolean changed = false;
		for (int index = 0; index < bagItems.size(); index++) {
			if (sourceStack.isEmpty()) {
				break;
			}

			if (!bagItems.get(index).isEmpty()) {
				continue;
			}

			int moved = Math.min(sourceStack.getMaxStackSize(), sourceStack.getCount());
			bagItems.set(index, sourceStack.copyWithCount(moved));
			sourceStack.shrink(moved);
			changed = true;
		}

		return changed;
	}

	private static void transferBagContentsToContainer(ServerPlayer player, InteractionHand hand, Container container) {
		ItemStack bagStack = player.getItemInHand(hand);
		if (!CroppyEnchantmentUtil.isFarmersBag(bagStack)) {
			return;
		}

		NonNullList<ItemStack> bagItems = NonNullList.withSize(BAG_SIZE, ItemStack.EMPTY);
		ItemContainerContents contents = bagStack.get(DataComponents.CONTAINER);
		if (contents != null) {
			contents.copyInto(bagItems);
		}

		boolean changed = false;
		boolean containerFull = false;
		List<DepositEntry> depositedItems = new ArrayList<>();
		for (int index = 0; index < bagItems.size(); index++) {
			ItemStack stack = bagItems.get(index);
			if (stack.isEmpty()) {
				continue;
			}

			ItemStack remaining = moveIntoContainer(container, stack.copy());
			int moved = stack.getCount() - remaining.getCount();
			if (moved > 0) {
				changed = true;
				recordDeposit(depositedItems, stack, moved);
			}

			if (!remaining.isEmpty()) {
				containerFull = true;
			}

			bagItems.set(index, remaining);
		}

		if (!changed) {
			if (containerFull) {
				player.displayClientMessage(Component.literal("Container is Full"), true);
			}
			return;
		}

		bagStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(bagItems));
		container.setChanged();
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
		sendDepositSummary(player, depositedItems);

		if (containerFull) {
			player.displayClientMessage(Component.literal("Container is Full"), true);
		}
	}

	private static void recordDeposit(List<DepositEntry> depositedItems, ItemStack stack, int moved) {
		for (DepositEntry entry : depositedItems) {
			if (ItemStack.isSameItemSameComponents(entry.stack, stack)) {
				entry.count += moved;
				return;
			}
		}

		depositedItems.add(new DepositEntry(stack.copyWithCount(1), moved));
	}

	private static void sendDepositSummary(ServerPlayer player, List<DepositEntry> depositedItems) {
		if (depositedItems.isEmpty()) {
			return;
		}

		player.sendSystemMessage(Component.literal("Farmer's Bag Deposit:")
			.withStyle(ChatFormatting.GOLD));
		for (DepositEntry entry : depositedItems) {
			player.sendSystemMessage(entry.stack.getHoverName().copy()
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(" - ")
					.withStyle(ChatFormatting.GRAY))
				.append(Component.literal("[x" + entry.count + "]")
					.withStyle(ChatFormatting.GREEN)));
		}
	}

	private static void transferBagContentsToComposter(ServerPlayer player, InteractionHand hand, ServerLevel level, BlockPos pos, BlockState state) {
		ItemStack bagStack = player.getItemInHand(hand);
		if (!CroppyEnchantmentUtil.isFarmersBag(bagStack) || !(state.getBlock() instanceof ComposterBlock)) {
			return;
		}

		boolean usesHiddenBuffer = ComposterBufferFeature.usesHiddenBuffer(level, pos);
		if (usesHiddenBuffer && ComposterBufferFeature.getStoredBoneMeal(level, pos) >= ComposterBufferData.MAX_STORED_BONE_MEAL) {
			return;
		}

		int composterLevel = state.getValue(ComposterBlock.LEVEL);
		if (composterLevel >= 7) {
			return;
		}

		NonNullList<ItemStack> bagItems = NonNullList.withSize(BAG_SIZE, ItemStack.EMPTY);
		ItemContainerContents contents = bagStack.get(DataComponents.CONTAINER);
		if (contents != null) {
			contents.copyInto(bagItems);
		}

		boolean changed = false;
		List<DepositEntry> depositedItems = new ArrayList<>();
		for (int index = 0; index < bagItems.size() && composterLevel < 7; index++) {
			ItemStack stack = bagItems.get(index);
			if (stack.isEmpty() || !ComposterBlock.COMPOSTABLES.containsKey(stack.getItem())) {
				continue;
			}

			while (!stack.isEmpty() && composterLevel < 7) {
				int beforeCount = stack.getCount();
				state = ComposterBlock.insertItem(player, state, level, stack, pos);
				if (stack.getCount() == beforeCount) {
					break;
				}

				changed = true;
				recordDeposit(depositedItems, stack, 1);
				composterLevel = state.getValue(ComposterBlock.LEVEL);
				if (usesHiddenBuffer && composterLevel == 7) {
					if (!ComposterBufferFeature.tryBufferCompletion(level, pos, state, false)) {
						break;
					}

					state = level.getBlockState(pos);
					composterLevel = state.getValue(ComposterBlock.LEVEL);
					if (ComposterBufferFeature.getStoredBoneMeal(level, pos) >= ComposterBufferData.MAX_STORED_BONE_MEAL) {
						break;
					}
				}
			}

			bagItems.set(index, stack);
		}

		if (!changed) {
			return;
		}

		bagStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(bagItems));
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
		sendDepositSummary(player, depositedItems);
	}

	private static void dropBagContents(ServerPlayer player, InteractionHand hand) {
		ItemStack bagStack = player.getItemInHand(hand);
		if (!CroppyEnchantmentUtil.isFarmersBag(bagStack)) {
			return;
		}

		ItemContainerContents contents = bagStack.get(DataComponents.CONTAINER);
		if (contents == null || contents.equals(ItemContainerContents.EMPTY)) {
			return;
		}

		NonNullList<ItemStack> bagItems = NonNullList.withSize(BAG_SIZE, ItemStack.EMPTY);
		contents.copyInto(bagItems);
		boolean droppedAny = false;
		for (ItemStack stack : bagItems) {
			if (stack.isEmpty()) {
				continue;
			}

			player.drop(stack.copy(), false);
			droppedAny = true;
		}

		if (!droppedAny) {
			return;
		}

		bagStack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
	}

	private static ItemStack moveIntoContainer(Container container, ItemStack sourceStack) {
		mergeIntoContainerStacks(container, sourceStack);
		if (!sourceStack.isEmpty()) {
			moveIntoEmptyContainerSlots(container, sourceStack);
		}

		return sourceStack;
	}

	private static void mergeIntoContainerStacks(Container container, ItemStack sourceStack) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (sourceStack.isEmpty()) {
				return;
			}

			ItemStack containerStack = container.getItem(slot);
			if (containerStack.isEmpty()
				|| !ItemStack.isSameItemSameComponents(containerStack, sourceStack)
				|| !container.canPlaceItem(slot, sourceStack)) {
				continue;
			}

			int maxStackSize = Math.min(container.getMaxStackSize(sourceStack), containerStack.getMaxStackSize());
			int space = maxStackSize - containerStack.getCount();
			if (space <= 0) {
				continue;
			}

			int moved = Math.min(space, sourceStack.getCount());
			containerStack.grow(moved);
			sourceStack.shrink(moved);
			container.setItem(slot, containerStack);
		}
	}

	private static void moveIntoEmptyContainerSlots(Container container, ItemStack sourceStack) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (sourceStack.isEmpty()) {
				return;
			}

			if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, sourceStack)) {
				continue;
			}

			int moved = Math.min(container.getMaxStackSize(sourceStack), sourceStack.getCount());
			container.setItem(slot, sourceStack.copyWithCount(moved));
			sourceStack.shrink(moved);
		}
	}

	private static final class DepositEntry {
		private final ItemStack stack;
		private int count;

		private DepositEntry(ItemStack stack, int count) {
			this.stack = stack;
			this.count = count;
		}
	}

	private static final class FarmersBagMenu extends AbstractContainerMenu {
		private final ServerPlayer player;
		private final InteractionHand hand;
		private final int mainHandInventorySlot;
		private final int lockedMenuSlot;
		private final SimpleContainer bagContainer;

		private FarmersBagMenu(int containerId, Inventory inventory, ServerPlayer player, InteractionHand hand) {
			super(MenuType.GENERIC_9x3, containerId);
			this.player = player;
			this.hand = hand;
			this.mainHandInventorySlot = inventory.getSelectedSlot();
			this.lockedMenuSlot = hand == InteractionHand.MAIN_HAND ? HOTBAR_START + this.mainHandInventorySlot : -1;
			this.bagContainer = new SimpleContainer(BAG_SIZE);
			loadContents();
			addBagSlots();
			addPlayerInventorySlots(inventory);
			addHotbarSlots(inventory);
		}

		@Override
		public boolean stillValid(Player player) {
			return CroppyEnchantmentUtil.isFarmersBag(getBagStack());
		}

		@Override
		public void clicked(int slotId, int button, ClickType clickType, Player player) {
			if (slotId == lockedMenuSlot) {
				return;
			}

			if (clickType == ClickType.SWAP) {
				if (hand == InteractionHand.MAIN_HAND && button == mainHandInventorySlot) {
					return;
				}

				if (hand == InteractionHand.OFF_HAND && button == OFFHAND_SWAP_BUTTON) {
					return;
				}
			}

			super.clicked(slotId, button, clickType, player);
		}

		@Override
		public ItemStack quickMoveStack(Player player, int slot) {
			if (slot == lockedMenuSlot) {
				return ItemStack.EMPTY;
			}

			Slot clickedSlot = slots.get(slot);
			if (!clickedSlot.hasItem()) {
				return ItemStack.EMPTY;
			}

			ItemStack stack = clickedSlot.getItem();
			ItemStack original = stack.copy();
			if (slot < BAG_SIZE) {
				if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!isAllowedContent(stack) || !moveItemStackTo(stack, 0, BAG_SIZE, false)) {
					return ItemStack.EMPTY;
				}
			}

			if (stack.isEmpty()) {
				clickedSlot.setByPlayer(ItemStack.EMPTY);
			} else {
				clickedSlot.setChanged();
			}

			return original;
		}

		@Override
		public void slotsChanged(Container container) {
			super.slotsChanged(container);
			if (container == bagContainer) {
				saveContents();
			}
		}

		@Override
		public void removed(Player player) {
			saveContents();
			super.removed(player);
		}

		private void addBagSlots() {
			for (int row = 0; row < BAG_ROWS; row++) {
				for (int column = 0; column < 9; column++) {
					int slotIndex = column + row * 9;
					addSlot(new FarmersBagSlot(bagContainer, slotIndex, 8 + column * 18, 18 + row * 18));
				}
			}
		}

		private void addPlayerInventorySlots(Inventory inventory) {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 9; column++) {
					int slotIndex = column + row * 9 + 9;
					addSlot(new Slot(inventory, slotIndex, 8 + column * 18, 84 + row * 18));
				}
			}
		}

		private void addHotbarSlots(Inventory inventory) {
			for (int column = 0; column < 9; column++) {
				int slotIndex = column;
				if (hand == InteractionHand.MAIN_HAND && slotIndex == mainHandInventorySlot) {
					addSlot(new LockedBagSourceSlot(inventory, slotIndex, 8 + column * 18, 142));
					continue;
				}

				addSlot(new Slot(inventory, slotIndex, 8 + column * 18, 142));
			}
		}

		private void loadContents() {
			ItemContainerContents contents = getBagStack().get(DataComponents.CONTAINER);
			if (contents == null) {
				return;
			}

			NonNullList<ItemStack> items = NonNullList.withSize(BAG_SIZE, ItemStack.EMPTY);
			contents.copyInto(items);
			for (int index = 0; index < BAG_SIZE; index++) {
				bagContainer.setItem(index, items.get(index).copy());
			}
		}

		private void saveContents() {
			ItemStack bagStack = getBagStack();
			if (!CroppyEnchantmentUtil.isFarmersBag(bagStack)) {
				return;
			}

			List<ItemStack> items = new ArrayList<>(BAG_SIZE);
			for (int index = 0; index < BAG_SIZE; index++) {
				items.add(bagContainer.getItem(index).copy());
			}

			bagStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
		}

		private ItemStack getBagStack() {
			return hand == InteractionHand.MAIN_HAND
				? player.getInventory().getItem(mainHandInventorySlot)
				: player.getOffhandItem();
		}
	}

	private static final class FarmersBagSlot extends Slot {
		private FarmersBagSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isAllowedContent(stack);
		}
	}

	private static final class LockedBagSourceSlot extends Slot {
		private LockedBagSourceSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
