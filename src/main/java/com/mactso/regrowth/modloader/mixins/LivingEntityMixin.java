package com.mactso.regrowth.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.regrowth.actions.ActionCoordinator;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
	abstract class LivingEntityMixin {

	    @Inject(method = "tick", at = @At("HEAD"))
	    private void onEntityMoves(CallbackInfo ci) {
	    	
			LivingEntity le = ((LivingEntity) (Object) this);

			if (le instanceof Player)
				return;
			
			Level level = le.level();
			if (level == null)
				return;
			
			if (level.isClientSide) {
				return;
			}
			
			if (!le.level().isLoaded(le.blockPosition())) {
			    // World or chunk not ready
			    return;
			}

			MyUtilities.debugMsg(1, "enter serverside Handle Entity Move Events");
			ActionCoordinator.coordinateLivingEntityActions(le);

	    }
	}
	

