// 16.3 v14
package com.mactso.regrowth.commands;


import com.mactso.regrowth.config.MyConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class RegrowthCommands {
	String subcommand = "";
	String value = "";
	
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		System.out.println("Enter register");
		dispatcher.register(Commands.literal("regrowth").requires((source) -> 
			{
				return source.hasPermission(2);
			}
		)
		.then(Commands.literal("debugLevel").then(
				Commands.argument("debugLevel", IntegerArgumentType.integer(0,2)).executes(ctx -> {
					return setDebugLevel(IntegerArgumentType.getInteger(ctx, "debugLevel"));
			}
			)
			)
			)
		.then(Commands.literal("info").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrException();

					World worldName = p.level;
					String objectInfo = "";
					MinecraftServer srv = p.getServer();
					if (!(p.level.isClientSide)) {
						Minecraft mc = Minecraft.getInstance();
	                    RayTraceResult object = mc.hitResult;
	                    if (object instanceof EntityRayTraceResult) {
	                    	EntityRayTraceResult ertr = (EntityRayTraceResult) object;
	                    	Entity tempEntity = ertr.getEntity();
	                    	objectInfo = tempEntity.getEncodeId();
	                     } else {
	                   		objectInfo = "You are not looking at an entity.";	                    	 
	                     }
					} else {
						objectInfo = "Load single player game to see entity you are looking at.";
					}
					//ITextComponent component = new StringTextComponent (worldName.getDimension().getType().getRegistryName() 
		            //		+ "\n Current Values");

					MyConfig.sendBoldChat(p, worldName.dimension().toString()
		            		+ "\n Current Values", Color.fromLegacyFormat(TextFormatting.DARK_GREEN));

		            String msg = 
		              		  "\n  Regrowth Version 1.16.1 06/29/2020"  
		            		+ "\n  Debug Level...........: " + MyConfig.aDebugLevel
		            		+ "\n  Looking At................:"  + objectInfo;
		            MyConfig.sendChat(p, msg, Color.fromLegacyFormat(TextFormatting.DARK_GREEN));
					return 1;
			}
			)
			)		
		);
		System.out.println("Exit Register");
	}

	public static int setDebugLevel (int newDebugLevel) {
		MyConfig.aDebugLevel = newDebugLevel;
		MyConfig.pushDebugLevel();
		return 1;
	}
}
