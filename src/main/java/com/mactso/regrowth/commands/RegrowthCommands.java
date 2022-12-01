
package com.mactso.regrowth.commands;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.utility.Utility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;


public class RegrowthCommands {
	String subcommand = "";
	String value = "";
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated)
	{
		System.out.println("Register Regrowth Commands");
		dispatcher.register(CommandManager.literal("regrowth").requires((source) -> 
			{
				return source.hasPermissionLevel(2);
			}
		)
		.then(CommandManager.literal("debugLevel").then(
				CommandManager.argument("debugLevel", IntegerArgumentType.integer(0,2)).executes(ctx -> {
					return setDebugLevel(IntegerArgumentType.getInteger(ctx, "debugLevel"));
			}
			)
			)
			)
		.then(CommandManager.literal("info").executes(ctx -> {
			ServerPlayerEntity p = ctx.getSource().getPlayer(); // or exception!
					World world = p.world;
					String objectInfo = "";

					if (p.world.isClient()) {
						MinecraftClient mc = MinecraftClient.getInstance();
	                    HitResult object = mc.crosshairTarget;
	                    if (object instanceof EntityHitResult) {
	                    	EntityHitResult ertr = (EntityHitResult) object;
	                    	Entity tempEntity = ertr.getEntity();
	                    	objectInfo = Utility.getResourceLocationString(tempEntity);
	                     } else {
	                   		objectInfo = "You are not looking at an entity.";	                    	 
	                     }
					} else {
						objectInfo = "Load single player game to see entity you are looking at.";
					}


					Utility.sendBoldChat(p, Utility.getResourceLocationString(world).toUpperCase()
		            		+ "\n Current Values", TextColor.fromFormatting(Formatting.DARK_GREEN));

		            String msg = 
		              		  "\n  Regrowth (Fabric) "  
		            		+ "\n  Debug Level...........: " + MyConfig.getDebugLevel()
		            		+ "\n  Looking At................: "  + objectInfo;
		            Utility.sendChat(p, msg, TextColor.fromFormatting(Formatting.DARK_GREEN));
					return 1;
			}
			)
			)		
		);
		System.out.println("Exit Register");
	}

	public static int setDebugLevel (int newDebugLevel) {
		MyConfig.setDebugLevel(newDebugLevel);
		// ModConfigs.pushDebugLevel();
		return 1;
	}
}

