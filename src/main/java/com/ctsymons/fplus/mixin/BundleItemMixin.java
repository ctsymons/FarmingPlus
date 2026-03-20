package com.ctsymons.fplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ctsymons.fplus.CroppyEnchantmentUtil;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(BundleItem.class)
abstract class BundleItemMixin {
	@Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
	private void fplus$disableFarmersBagStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (CroppyEnchantmentUtil.isFarmersBag(stack)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
	private void fplus$disableFarmersBagOtherStackedOnMe(ItemStack incomingStack, ItemStack stack, Slot slot, ClickAction action, Player player, SlotAccess slotAccess, CallbackInfoReturnable<Boolean> cir) {
		if (CroppyEnchantmentUtil.isFarmersBag(stack)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void fplus$disableFarmersBagUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (CroppyEnchantmentUtil.isFarmersBag(player.getItemInHand(hand))) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
