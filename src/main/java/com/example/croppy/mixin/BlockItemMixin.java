package com.example.croppy.mixin;

import com.example.croppy.PlayerPlacedPumpkinsData;
import com.example.croppy.ComposterBufferData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@Shadow
	@Final
	private Block block;

	@Inject(method = "place", at = @At("RETURN"))
	private void fplus$trackPlayerPlacedPumpkins(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction() || !(context.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		BlockPos pos = context.getClickedPos();
		BlockState placedState = serverLevel.getBlockState(pos);
		if (!placedState.is(this.block)) {
			return;
		}

		ComposterBufferData.clear(serverLevel, pos);

		if (placedState.is(Blocks.PUMPKIN) && context.getPlayer() != null) {
			PlayerPlacedPumpkinsData.markPlaced(serverLevel, pos);
			return;
		}

		PlayerPlacedPumpkinsData.clear(serverLevel, pos);
	}
}
