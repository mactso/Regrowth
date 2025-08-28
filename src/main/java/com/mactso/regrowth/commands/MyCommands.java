// 16.3 v14
package com.mactso.regrowth.commands;

import java.util.List;
import java.util.Optional;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.utility.Utility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MyCommands {
	String subcommand = "";
	String value = "";
	

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		System.out.println("Enter register");
		dispatcher.register(Commands.literal("regrowth").requires((source) -> {
				return source.hasPermission(2);
		}).then(Commands.literal("debugLevel")
				.then(Commands.argument("debugLevel", IntegerArgumentType.integer(0, 2)).executes(ctx -> {
					return setDebugLevel(IntegerArgumentType.getInteger(ctx, "debugLevel"));
				}))).then(Commands.literal("info").executes(ctx -> {
					ServerPlayer sp = ctx.getSource().getPlayerOrException();
					ServerLevel serverLevel = sp.level();
					String objectInfo = "";
					LivingEntity target = getLookedAtEntity(sp, 8.0D);
					if (target != null) {
						objectInfo = "You are looking at: " +  EntityType.getKey(target.getType()).toString();
	                     } else {
					    objectInfo = "You don't see an entity.";
					}
						ResourceLocation rl =serverLevel.dimension().location();

					Utility.sendBoldChat(sp, "Regrowth " + rl.toString() + " Current Values", ChatFormatting.DARK_GREEN);

					String msg = "  Debug Level...........: " + MyConfig.getDebugLevel()
		            		+ "\n  Looking At................:"  + objectInfo;
					Utility.sendChat(sp, msg, ChatFormatting.DARK_GREEN);
					return 1;
			}
		)));
		System.out.println("Exit Register");
	}

	public static int setDebugLevel(int newDebugLevel) {
		MyConfig.setDebugLevel(newDebugLevel);
	
		return 1;
	}

	
	// a serverside utility that checks what the nearest entity seen by the player is.
	public static LivingEntity getLookedAtEntity(ServerPlayer player, double range) {
	ServerLevel level = player.level();
		Vec3 eyePos = player.getEyePosition(1.0F); // Player eye location
		Vec3 lookVec = player.getLookAngle(); // Direction the player is looking
		Vec3 reachVec = eyePos.add(lookVec.scale(range));

//		// Bounding box along line of sight
		AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);

		List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
				e -> e != player && e.isPickable() && e.isAlive());

		LivingEntity closest = null;
		double closestDistSq = range * range;

		for (LivingEntity entity : entities) {
			AABB bb = entity.getBoundingBox().inflate(0.3D); // make hitbox a bit more forgiving
			Optional<Vec3> optionalHit = bb.clip(eyePos, reachVec); // note: before 1.21.5, clip returns a nullable Vec3.
			if (optionalHit.isEmpty())
				continue;
			Vec3 hitVec = optionalHit.get();
			double distSq = eyePos.distanceTo(hitVec);
			if (distSq < closestDistSq) {
				closestDistSq = distSq;
				closest = entity;
			}
		}

		return closest;
	}
}
