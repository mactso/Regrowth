package com.mactso.regrowth.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.regrowth.actions.TrampleAction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;


@Mixin(FarmBlock.class)
	abstract class FarmlandBlockMixin {

	    @Inject(method = "fallOn(Lnet/minecraft/world/level/Level;"
	    		+ "Lnet/minecraft/world/level/block/state/BlockState;"
	    		+ "Lnet/minecraft/core/BlockPos;"
	    		+ "Lnet/minecraft/world/entity/Entity;F)V", at = @At("HEAD"), cancellable = true)
	    private void onFarmlandTrampled(Level w, BlockState bs, BlockPos pos, Entity entity, float f, CallbackInfo ci) {

	    	if (TrampleAction.doTrampleAction(entity)) {
	    		ci.cancel();
	    	}
	    }
	}
	

