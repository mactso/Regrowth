package com.mactso.regrowth.events;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// this is the only Forge Specific Code.
public class HandleForgeEntityMoveEvent {
	@SuppressWarnings("resource")
	@SubscribeEvent
	public static void handleEntityMoveEvents(LivingTickEvent event) {
		
		LivingEntity le = event.getEntity();

		if (le instanceof Player)
			return;

		if (le.getId() % 2 == le.level().getGameTime() % 2)
			return;

		if (le.blockPosition() == null)
			return;

		Level level = le.level();
		if (level.isClientSide) {
			return;
		}

		ServerLevel serverLevel = (ServerLevel) le.level();
		
		Utility.debugMsg(1, "enter serverside Handle Entity Move Events");
		MoveEntityEvent.doRegrowthActions(le, serverLevel);
	}
}
