package com.mactso.regrowth.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.regrowth.events.MoveEntityEvent;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

@Mixin(LivingEntity.class)
	abstract class LivingEntityMixin {

	    @Inject(method = "tickMovement()", at = @At("HEAD"), cancellable = true)
	    private void onEntityMoves(CallbackInfo ci) {
	    	MoveEntityEvent.handleEntityMoveEvents((LivingEntity) (Object) this);
	    }
	}
	

