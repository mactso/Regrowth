package com.mactso.regrowth.commands;

import java.util.List;
import java.util.Optional;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.modloader.main.RegrowthMain;
import com.mactso.regrowth.utilities.MyUtilities;
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

public class RegrowthCommands {

	private static final String MOD_VERSION = "Forge Regrowth 1.21.9 CC 36.4";
	
	private static final String DEBUG_LEVEL_ARG = "level 0-2";
	
	// ---------------------------
	// Permission constants
	// ---------------------------
	public final class PermissionLevel {
	    public static final int ALL   = 0; // All players; /say, /help, /spawnpoint
	    public static final int MOD   = 1; // Moderators; /gamemode <mode>, /teleport <player>
	    public static final int OP    = 2; // Operators; /setblock, /time set, /regrowth
	    public static final int ADMIN = 3; // Admins; /stop, /ban, /kick
	    public static final int OWNER = 4; // Server owner; /op <player>, /deop <player>, /reload
	    // No constructor needed because the class is final and contains only static members
	}

	// ---------------------------
	// Command result constants
	// ---------------------------
	public final class CommandResult {
	    public static final int NONE     = 0; // Command had no effect
	    public static final int SUCCESS  = 1; // Standard success
	    public static final int MULTIPLE = 2; // Multiple entities/things affected
	    // No constructor needed because the class is final and contains only static members
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
	    MyUtilities.debugMsg(0, "Registering " + RegrowthMain.MODID + " commands.");

	    dispatcher.register(
	        Commands.literal("regrowth")
	            .requires(source -> source.hasPermission(PermissionLevel.OP))
	            .then(
	                Commands.literal("debugLevel")
	                    .then(
	                        Commands.argument(DEBUG_LEVEL_ARG, IntegerArgumentType.integer(0, 2))
	                            .executes(ctx -> setDebugLevel(
	                                IntegerArgumentType.getInteger(ctx, DEBUG_LEVEL_ARG)
	                            ))
	                    )
	            )
	            .then(
	                Commands.literal("info")
	                    .executes(ctx -> doInfoCommand(
	                        ctx.getSource().getPlayerOrException()
	                    ))
	            )
	    );
	}

	public static int setDebugLevel(int newDebugLevel) {
		MyConfig.setDebugLevel(newDebugLevel); 
		MyConfig.pushDebugLevel();
		return CommandResult.SUCCESS;
	}

	// Server-side utility: finds the nearest entity the player is looking at
	public static LivingEntity getLookedAtEntity(ServerPlayer player, double range) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 eyePos = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getLookAngle();
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

			double distSq = eyePos.distanceTo(optionalHit.get());
			if (distSq < closestDistSq) {
				closestDistSq = distSq;
				closest = entity;
			}
		}

		return closest;
	}

	// Handles the logic of the "info" command
	private static int doInfoCommand(ServerPlayer sp) {
		ServerLevel serverLevel = (ServerLevel) sp.level();

		LivingEntity target = getLookedAtEntity(sp, 8.0D);
		String objectInfo = target != null ? "You are looking at: " + EntityType.getKey(target.getType()).toString()
				: "You see no entity at all.";

		ResourceLocation rl = serverLevel.dimension().location();

		MyUtilities.sendBoldChat(sp, "Regrowth "+ MOD_VERSION +" in "+ rl + "\n Current Values", ChatFormatting.DARK_GREEN);

		String msg = "  Debug Level 0-2 : " + MyConfig.getDebugLevel() + "\n  Looking At : "
				+ objectInfo;

		MyUtilities.sendChat(sp, msg, ChatFormatting.DARK_GREEN);

		return CommandResult.SUCCESS;
	}
}
