package com.atex.h11.custom.event.sph.test;

import java.io.File;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import com.unisys.media.cr.adapter.ncm.common.event.config.ObjectEventHandler;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectEvent;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectFullEvent;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectMultilinkEvent;
import com.unisys.media.cr.common.data.interfaces.IDataSource;

public class TestHandler extends ObjectEventHandler {
	
	private static final String log4jConfig = "conf/spheventhandler.log4j.properties";	
	private static Logger logger = Logger.getLogger(TestHandler.class.getName()); 
	private static boolean isInitialized = false;
		
	private void initialize() {
		System.out.println("Initializing " + this.getClass().getName() + "...");
		setLogger();
		System.out.println("Initialized " + this.getClass().getName() + " OK");
		isInitialized = true;
	}
	
	private void setLogger() {
		String log4jFileName = log4jConfig;
		if (log4jFileName != null) {
			File log4jFile = new File(log4jFileName);
			if (log4jFile.canRead()) {
				PropertyConfigurator.configure(log4jFileName);
			} else {
				BasicConfigurator.configure();
			}
		} else {
			BasicConfigurator.configure();
		}
	}		
	
	@Override
	public void handleEvent(IDataSource ds, IObjectFullEvent event) {
		if (!isInitialized) {
			initialize();
		}
		logger.debug("TEST handle event: " + event.toString());
	}
	
	@Override
	public void handleEvent(IDataSource ds, IObjectMultilinkEvent event) {
		if (!isInitialized) {
			initialize();
		}
		logger.debug("TEST handle event: " + event.toString());
	}

	@Override
	public void handleEvent(IDataSource ds, IObjectEvent event) { 
		if (!isInitialized) {
			initialize();
		}
		logger.debug("TEST handle event: " + event.toString()); 
	}
		
}
