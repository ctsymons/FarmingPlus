package com.example.croppy.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.croppy.CroppyEnchantmentUtil;
import com.example.croppy.FarmersBagFeature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemEntity.class)
abstract class PlayerPickupMixin {
	@Unique
	private List<ItemStack> fplus$inventoryBeforePickup;

	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void fplus$captureInventoryBeforePickup(Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		ItemStack stack = ((ItemEntity) (Object) this).getItem();
		if (stack.isEmpty() || !playerHasFarmersBag(player) || !FarmersBagFeature.isBagEligibleItem(stack)) {
			return;
		}

		this.fplus$inventoryBeforePickup = FarmersBagFeature.snapshotInventory(serverPlayer.getInventory());
	}

	@Inject(method = "playerTouch", at = @At("TAIL"))
	private void fplus$movePickedUpItemsIntoBag(Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer serverPlayer) || this.fplus$inventoryBeforePickup == null) {
			return;
		}

		FarmersBagFeature.movePickedUpItemsToBag(serverPlayer, this.fplus$inventoryBeforePickup);
		this.fplus$inventoryBeforePickup = null;
	}

	@Unique
	private static boolean playerHasFarmersBag(Player player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (CroppyEnchantmentUtil.isFarmersBag(inventory.getItem(slot))) {
				return true;
			}
		}

		return false;
	}
}
