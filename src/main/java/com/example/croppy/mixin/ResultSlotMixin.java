package com.example.croppy.mixin;

import com.example.croppy.FPlusCrafting;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.Level;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
	@Shadow
	@Final
	private CraftingContainer craftSlots;

	@Unique
	private int[] fplus$extraConsumeCounts;

	@Inject(method = "onTake", at = @At("HEAD"))
	private void fplus$markSpecialCraft(Player player, ItemStack stack, CallbackInfo ci) {
		CraftingInput input = this.craftSlots.asPositionedCraftInput().input();
		this.fplus$extraConsumeCounts = FPlusCrafting.getExtraConsumeCounts(input);
	}

	@Inject(method = "getRemainingItems", at = @At("HEAD"), cancellable = true)
	private void fplus$clearVanillaRemaindersForCustomCrafts(
		CraftingInput input,
		Level level,
		CallbackInfoReturnable<NonNullList<ItemStack>> cir
	) {
		if (!FPlusCrafting.getCraftingResult(input, level.registryAccess()).isEmpty()) {
			cir.setReturnValue(NonNullList.withSize(input.size(), ItemStack.EMPTY));
		}
	}

	@Inject(method = "onTake", at = @At("TAIL"))
	private void fplus$consumeExtraCraftingInputs(Player player, ItemStack stack, CallbackInfo ci) {
		if (this.fplus$extraConsumeCounts == null) {
			return;
		}

		for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
			int extraCount = slot < this.fplus$extraConsumeCounts.length ? this.fplus$extraConsumeCounts[slot] : 0;
			if (extraCount > 0 && !this.craftSlots.getItem(slot).isEmpty()) {
				this.craftSlots.removeItem(slot, extraCount);
			}
		}

		this.fplus$extraConsumeCounts = null;
	}
}
