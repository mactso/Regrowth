
package com.mactso.regrowth.commands;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.utility.Utility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class RegrowthCommands {
	String subcommand = "";
	String value = "";
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SuppressWarnings("resource")
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
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
					ServerPlayer p = ctx.getSource().getPlayerOrException();

					Level worldName = p.level();
					String objectInfo = "";
					MinecraftServer srv = p.getServer();
					if (!(p.level().isClientSide)) {
						Minecraft mc = Minecraft.getInstance();
	                    HitResult object = mc.hitResult;
	                    if (object instanceof EntityHitResult) {
	                    	EntityHitResult ertr = (EntityHitResult) object;
	                    	Entity tempEntity = ertr.getEntity();
	                    	objectInfo = EntityType.getKey(tempEntity.getType()).toString();
	                     } else {
	                   		objectInfo = "You are not looking at an entity.";	                    	 
	                     }
					} else {
						objectInfo = "Load single player game to see entity you are looking at.";
					}
					//ITextComponent component = new StringTextComponent (worldName.getDimension().getType().getRegistryName() 
		            //		+ "\n Current Values");

					Utility.sendBoldChat(p, worldName.dimension().toString()
		            		+ "\n Current Values", ChatFormatting.DARK_GREEN);

		            String msg = 
		              		  "\n  Regrowth (Fabric) "  
		            		+ "\n  Debug Level...........: " + MyConfig.getDebugLevel()
		            		+ "\n  Looking At................:"  + objectInfo;
		            Utility.sendChat(p, msg, ChatFormatting.DARK_GREEN);
					return 1;
			}
			)
			)		
		);
		System.out.println("Exit Register");
	}

	public static int setDebugLevel (int newDebugLevel) {
		MyConfig.setDebugLevel(newDebugLevel);
		MyConfig.pushDebugLevel();
		return 1;
	}
}

