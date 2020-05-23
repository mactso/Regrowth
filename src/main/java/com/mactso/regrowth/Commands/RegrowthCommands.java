// 15.2 - 1.0.0.0 regrowth
package com.mactso.regrowth.Commands;


import com.mactso.regrowth.config.MyConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sun.jna.platform.win32.WinUser.MSG;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MinecraftGame;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.OnlyIns;

public class RegrowthCommands {
	String subcommand = "";
	String value = "";
	
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("regrowth").requires((source) -> 
			{
				return source.hasPermissionLevel(2);
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
					ServerPlayerEntity p = ctx.getSource().asPlayer();
					World worldName = p.world;
					String objectInfo = "";
					MinecraftServer srv = p.getServer();
					if (!(p.world.isRemote)) {
						Minecraft mc = Minecraft.getInstance();
	                    RayTraceResult object = mc.objectMouseOver;
	                    if (object instanceof EntityRayTraceResult) {
	                    	EntityRayTraceResult ertr = (EntityRayTraceResult) object;
	                    	Entity tempEntity = ertr.getEntity();
	                    	objectInfo = tempEntity.getEntityString();
	                     }
					} else {
						objectInfo = "Dedicated Server.";
					}
					ITextComponent component = new StringTextComponent (worldName.getDimension().getType().getRegistryName() 
		            		+ "\n Current Values");
		            component.applyTextStyle(TextFormatting.BOLD);
		            component.applyTextStyle(TextFormatting.DARK_GREEN);
		            p.sendMessage(component);
		            component = new StringTextComponent (
		              		  "\n  Regrowth Version 1.15.2 1.0.0.0"  
		            		+ "\n  Debug Level...........: " + MyConfig.aDebugLevel
		            		+ "\n  Looking At................:"  + objectInfo
		            		);
		            component.applyTextStyle(TextFormatting.DARK_GREEN);
		            p.sendMessage(component);
					return 1;
			}
			)
			)		
		);

	}

	public static int setDebugLevel (int newDebugLevel) {
		MyConfig.aDebugLevel = newDebugLevel;
		MyConfig.pushValues();
		return 1;
	}
}

