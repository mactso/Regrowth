package com.mactso.regrowth.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.regrowth.events.TrampleEventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;


@Mixin(FarmBlock.class)
	abstract class FarmlandBlockMixin {
	// 	public void fallOn(Level level, BlockState blockState, BlockPos blockPos, Entity entity, double d) {

	    @Inject(method = "fallOn(Lnet/minecraft/world/level/Level;"
	    		+ "Lnet/minecraft/world/level/block/state/BlockState;"
	    		+ "Lnet/minecraft/core/BlockPos;"
	    		+ "Lnet/minecraft/world/entity/Entity;D)V", at = @At("HEAD"), cancellable = true)
	    private void onFarmlandTrampled(Level w, BlockState bs, BlockPos pos, Entity entity, double f, CallbackInfo ci) {

	    	if (TrampleEventHandler.handleTrampleEvent(entity)) {
	    		ci.cancel();
	    	}
	    }
	}
	

