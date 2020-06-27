package com.mactso.regrowth;

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

	System.out.println("Regrowth: clientServer version ");


}
