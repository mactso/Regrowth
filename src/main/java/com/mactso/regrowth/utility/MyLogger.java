package com.mactso.regrowth.utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.minecraft.server.level.ServerPlayer;

public class MyLogger {

	public static void logItem(ServerPlayer cheater, String violation, boolean header) {

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	    LocalDateTime now = LocalDateTime.now();  

		   
		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/hardercheating/activity.log", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		String pos = cheater.blockPosition().toString();
		String name = cheater.getName().getString();

		if (header) p.println(dtf.format(now) + " ("+String.format("%-20s", pos) + ")  " +String.format("%-16s", name) + ") " + cheater.getStringUUID());
		p.println("   " + violation);
		
		if (p != System.out) {
			p.close();
		}
		
	}
}
