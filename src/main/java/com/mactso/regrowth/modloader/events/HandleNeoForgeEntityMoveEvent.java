package com.mactso.regrowth.modloader.events;

import com.mactso.regrowth.actions.ActionCoordinator;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;



// this is the only NeoForge Specific Code.

public class HandleNeoForgeEntityMoveEvent {

    public static void register() {
        NeoForge.EVENT_BUS.register(new HandleNeoForgeEntityMoveEvent());
    }

	@SubscribeEvent
	public void handleEntityMoveEvents(EntityTickEvent.Pre event) {
		
		if (!(event.getEntity() instanceof LivingEntity le))
			return;

		if (le instanceof Player)
			return;
		
		Level level = le.level();
		if (level == null)
			return;
		
		if (level.isClientSide()) {
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
