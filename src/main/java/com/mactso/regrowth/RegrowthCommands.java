package com.mactso.regrowth;


import java.util.ArrayList;
import java.util.List;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.util.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

	public class RegrowthCommands implements ICommand {

		
		@Override
		public int compareTo(ICommand o) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public String getUsage(ICommandSender sender) {
			return ("Gives Regrowth Info.");
		}
		

		@Override
		public String getName() {
			return "/Regrowth";
		}
		
		@Override
		public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			if (sender.canUseCommand(3,"SetDebugLevel")) {
				return true;
			}
			return false;
		}

		@Override
		public boolean isUsernameIndex(String[] args, int index) {
			return false;
		}
		
		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (sender instanceof EntityPlayerMP) {
				EntityPlayer player = (EntityPlayer) sender;
//				World worldName = player.world;
				if (args[0].equalsIgnoreCase("info")) {
		            showInfo(player);	
				} else if (args[0].equalsIgnoreCase("debug")) {
			        setDebugLevel(args, player);
				}
			}
		}
		
		public static void showInfo(EntityPlayer player) {
			IBlockState bs;
			Block block;
			int meta = 0;
			
			ITextComponent component = new TextComponentString ("\n Current Values");
			component.getStyle().setColor(TextFormatting.GREEN);
			player.sendMessage(component);
			bs = player.world.getBlockState(player.getPosition());
			block = bs.getBlock();
			meta = block.getMetaFromState(bs);
			if (block instanceof BlockAir) {
				bs = player.world.getBlockState(player.getPosition().down());
				block = bs.getBlock();
				meta = block.getMetaFromState(bs);
			}

			String blockKey = block.getRegistryName().toString() + ">" + meta;

			component = new TextComponentString (
					  "\n  Standing On.............: " + blockKey  
			  		+ "\n  Debug Level...........: " + MyConfig.aDebugLevel
					);
			component.getStyle().setColor(TextFormatting.GREEN);
			player.sendMessage(component);
		}
		
		public static void setDebugLevel (String[] args, EntityPlayer player) {
			if(args[1] != null) {
				int debugLevel = MyConfig.aDebugLevel;
				try {
			    	debugLevel = Integer.valueOf(args[1]);
			    	if ((debugLevel >= 0) && (debugLevel <= 2)) {
			    		MyConfig.setaDebugLevel(debugLevel);
			    		ConfigManager.sync (Reference.MOD_ID, Config.Type.INSTANCE);
			    		ITextComponent component = 
			    				new TextComponentString (
				  		  		  "Debug Value set to : " + debugLevel	
				  				);		        		
				  		component.getStyle().setColor(TextFormatting.GREEN);
				  		player.sendMessage(component);
						if (MyConfig.getaDebugLevel() > 0) {
							System.out.println(component.toString());
						}		
			    	}
				}
				catch (NumberFormatException e){
					ITextComponent component = 
							new TextComponentString (
			  		  		  "Debug Values should be : 0 to 2"	
			  				);		        		
			  		component.getStyle().setColor(TextFormatting.RED);
			  		player.sendMessage(component);	
				}
			}
		}

		@Override
		public List<String> getAliases() {
			List<String> commandAliases = new ArrayList<>();
			commandAliases.add("regrowth");
			commandAliases.add("/regrowth");
			return commandAliases;
		}

		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
				BlockPos targetPos) {
			List<String> commandTabCompletions = new ArrayList<String>();
			commandTabCompletions.add("info");
			commandTabCompletions.add("debug");
			return commandTabCompletions;
		}


	}

