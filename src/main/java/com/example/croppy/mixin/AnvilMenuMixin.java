package com.example.croppy.mixin;

import com.example.croppy.CroppyEnchantmentUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private int repairItemCountCost;

	@Inject(method = "createResult", at = @At("TAIL"))
	private void croppy$fixCroppyAnvilOutput(CallbackInfo ci) {
		AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
		var left = menu.getSlot(0).getItem();
		var right = menu.getSlot(1).getItem();
		ItemStack customResult = CroppyEnchantmentUtil.createAnvilResult(left, right);

		if (customResult.isEmpty()) {
			if (!CroppyEnchantmentUtil.usesCustomAnvil(left, right)) {
				return;
			}

			menu.getSlot(2).set(ItemStack.EMPTY);
			this.cost.set(0);
			this.repairItemCountCost = 0;
			menu.broadcastChanges();
			return;
		}

		menu.getSlot(2).set(customResult);
		this.cost.set(1);
		this.repairItemCountCost = 1;
		menu.broadcastChanges();
	}
}
