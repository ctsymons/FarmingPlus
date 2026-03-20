package com.example.croppy.mixin;

import com.example.croppy.CroppyEnchantmentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {
	@Redirect(
		method = "fallOn",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
		)
	)
	private void croppy$preventCroplandTrample(Entity entity, BlockState state, Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel
			&& entity instanceof Player player
			&& CroppyEnchantmentUtil.shouldPreventTrample(serverLevel, player)) {
			return;
		}

		FarmBlock.turnToDirt(entity, state, level, pos);
	}
}
