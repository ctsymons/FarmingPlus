package com.example.croppy.mixin;

import com.example.croppy.FPlusCrafting;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
	private static void fplus$setCustomCraftingResult(
		AbstractContainerMenu menu,
		ServerLevel level,
		Player player,
		CraftingContainer craftSlots,
		ResultContainer resultSlots,
		RecipeHolder<CraftingRecipe> recipe,
		CallbackInfo ci
	) {
		if (!resultSlots.getItem(0).isEmpty()) {
			return;
		}

		CraftingInput input = craftSlots.asCraftInput();
		ItemStack customResult = FPlusCrafting.getCraftingResult(input, level.registryAccess());
		if (customResult.isEmpty()) {
			return;
		}

		resultSlots.setRecipeUsed(null);
		resultSlots.setItem(0, customResult);
		menu.setRemoteSlot(0, customResult);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
				menu.containerId,
				menu.incrementStateId(),
				0,
				customResult
			));
		}
	}
}
