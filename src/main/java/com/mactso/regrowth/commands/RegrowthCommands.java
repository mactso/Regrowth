//// 16.3 v14
//package com.mactso.regrowth.commands;
//
//
//
//import com.mactso.regrowth.config.ModConfigs;
//import com.mojang.brigadier.Command;
//import com.mojang.brigadier.CommandDispatcher;
//import com.mojang.brigadier.arguments.IntegerArgumentType;
//
//import net.minecraft.command.CommandSource;
//
//
//public class RegrowthCommands {
//	String subcommand = "";
//	String value = "";
//
//	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
//	{
//		System.out.println("Register Regrowth Commands");
//		dispatcher.register(Commands.literal("regrowth").requires((source) -> 
//			{
//				return source.hasPermission(2);
//			}
//		)
//		.then(Commands.literal("debugLevel").then(
//				Commands.argument("debugLevel", IntegerArgumentType.integer(0,2)).executes(ctx -> {
//					return setDebugLevel(IntegerArgumentType.getInteger(ctx, "debugLevel"));
//			}
//			)
//			)
//			)
//		.then(Commands.literal("info").executes(ctx -> {
//					ServerPlayer p = ctx.getSource().getPlayerOrException();
//
//					Level worldName = p.level;
//					String objectInfo = "";
//					MinecraftServer srv = p.getServer();
//					if (!(p.level.isClientSide)) {
//						Minecraft mc = Minecraft.getInstance();
//	                    HitResult object = mc.hitResult;
//	                    if (object instanceof EntityHitResult) {
//	                    	EntityHitResult ertr = (EntityHitResult) object;
//	                    	Entity tempEntity = ertr.getEntity();
//	                    	objectInfo = tempEntity.getEncodeId();
//	                     } else {
//	                   		objectInfo = "You are not looking at an entity.";	                    	 
//	                     }
//					} else {
//						objectInfo = "Load single player game to see entity you are looking at.";
//					}
//					//ITextComponent component = new StringTextComponent (worldName.getDimension().getType().getRegistryName() 
//		            //		+ "\n Current Values");
//
//					ModConfigs.sendBoldChat(p, worldName.dimension().toString()
//		            		+ "\n Current Values", TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN));
//
//		            String msg = 
//		              		  "\n  Regrowth Version 1.16.1 06/29/2020"  
//		            		+ "\n  Debug Level...........: " + ModConfigs.aDebugLevel
//		            		+ "\n  Looking At................:"  + objectInfo;
//		            ModConfigs.sendChat(p, msg, TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN));
//					return 1;
//			}
//			)
//			)		
//		);
//		System.out.println("Exit Register");
//	}
//
//	public static int setDebugLevel (int newDebugLevel) {
//		ModConfigs.setDebugLevel(newDebugLevel);
//		ModConfigs.pushDebugLevel();
//		return 1;
//	}
//}

