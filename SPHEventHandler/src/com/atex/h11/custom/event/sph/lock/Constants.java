package com.atex.h11.custom.event.sph.lock;

public class Constants {
	// Logging
	public static final String NAME_LOGGER 				= LockHandler.class.getName();
	public static final String log4jConfig 				= "conf/sphlockhandler.log4j.properties";
	
	// Entries in the configuration properties file. 
	public static final String DefaultConfigurationFile	= "conf/sphlockhandler.properties";
	
	// Event IDs
	public static final int LOCK_OBJ 	= 6;
	public static final int UNLOCK_OBJ 	= 7;
}
