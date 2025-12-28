package com.mactso.regrowth.actions;

import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * Handles farmland trampling in a platform-agnostic way.
 * 
 * Called by modloader-specific farmland events or mixins (Fabric, Forge,
 * NeoForge). Determines if an entity can trample farmland based on type and
 * status.
 */

public class TrampleAction {

	// Platform-agnostic trample handler: called by modloader-specific farmland
	// events/mixins
	// called by Fabric Farmland Mixin, Forge FarmlandTrampleEvent, NeoForge
	// Farmland Trample Event.

	public static boolean doTrampleAction(Entity entity) {

		BlockPos pos = entity.blockPosition();
		MyUtilities.debugMsg(1, pos, "Enter handleTrampleEvent");
		
		if (entity instanceof Villager ve) {
			if ((ve.getVillagerData().getProfession() == VillagerProfession.FARMER)
					&& (ve.getVillagerData().getLevel() > 3)) {
				MyUtilities.debugMsg(1, pos, "Villager is L3 farmer");
				return true;
			}
		}
		if ((entity instanceof ServerPlayer sp)) {
			if (sp.isCreative()) {
				return true;
			}
			MyUtilities.debugMsg(1, pos, "FarmlandTrampleCancelled");
		}

		return false;
	}
	
}
