package com.mactso.regrowth.events;

import com.mactso.regrowth.actions.ActionCoordinator;
import com.mactso.regrowth.utility.Utility;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// this is the only Forge Specific Code.
@Mod.EventBusSubscriber()
public class HandleForgeEntityMoveEvent {


	@SubscribeEvent
	public static void handleEntityMoveEvents(LivingTickEvent event) {
		
		
		LivingEntity le = event.getEntity();

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

		Utility.debugMsg(1, "enter serverside Handle Entity Move Events");
		ActionCoordinator.coordinateLivingEntityActions(le);
	}
}
