// 1.12.2
package com.mactso.regrowth;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

import com.mactso.regrowth.events.MoveEntityEvent;
import com.mactso.regrowth.util.Reference;

// Special Note: You must choose one of the following annotations to compile against.
// NOTE: Compile with *THIS* annotation for client/server version.

@Mod(modid = Reference.MOD_ID, 
name = Reference.NAME, 
version = Reference.VERSION  
)
//NOTE: Compile with *THIS* annotation for server side only version.
//@Mod(modid = Reference.MOD_ID, 
//	name = Reference.NAME, 
//	version = Reference.VERSION , 
//	serverSideOnly = true,
//	acceptableRemoteVersions = "*")

public class Main
{

	@Instance
	public static Main instance;

	@EventHandler
	public void preInit (FMLPreInitializationEvent event) {
		System.out.println("Regrowth 16.4 1.1.0.18: Registering Handler");
		MinecraftForge.EVENT_BUS.register(new MoveEntityEvent());
		MinecraftForge.EVENT_BUS.register(this);		
	}

	@EventHandler
	public void serverLoad (FMLServerStartingEvent event) {
		System.out.println("Regrowth: Registering Commands");
		event.registerServerCommand(new RegrowthCommands());
	}
	
//	@EventHandler
//	public void init (FMLInitializationEvent event) {
//		Register.initPackets();
//	}

	
	
	@SubscribeEvent
	public void clientConnectionEvent (PlayerLoggedInEvent event) {
	    if (event.player instanceof EntityPlayerMP)
	    {
//	    	MyConfig.serverSide = true;
	    }			
	}
	
//	@EventHandler
//	public void init (FMLInitializationEvent event) {
//		Register.initPackets();
//	}


}
