package com.example.croppy.mixin;

import com.example.croppy.ComposterBufferFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
abstract class ComposterBlockMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void fplus$bufferReadyCompost(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (ComposterBufferFeature.tryBufferCompletion(level, pos, state, true)) {
			ci.cancel();
		}
	}

	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void fplus$extractBufferedBoneMeal(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel serverLevel) || state.getValue(ComposterBlock.LEVEL) == 8) {
			return;
		}

		if (ComposterBufferFeature.tryExtractBufferedBoneMeal(serverLevel, pos, player)) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
