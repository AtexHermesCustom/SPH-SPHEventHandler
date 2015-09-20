package com.atex.h11.custom.event.sph;

/**
 * All constants go there
 * 
 */
public class Constants {
	
	// Logging
	public static final String NAME_LOGGER 				= CustomObjectEventHandler.class.getName();
	public static final String log4jConfig 				= "conf/spheventhandler.log4j.properties";
	
	// Entries in the configuration properties file. 
	public static final String DefaultConfigurationFile	= "conf/spheventhandler.properties";
	
	// Event IDs
	public static final int SAVE_OBJ 	= 5;
	public static final int MOVE_OBJ 	= 10;
	public static final int UNLINK_OBJ	= 9;
	public static final int LAYOUTSAVE_OBJ = 63;
	
	// Status synchronization modes
	public static final int FOLLOW_SP_STATUS = 0;
	public static final int FOLLOW_TEXT_STATUS = 1;
	
}	
