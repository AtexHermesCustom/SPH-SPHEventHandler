package com.atex.h11.custom.event.sph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public abstract class InitializerAbstract {

	protected Logger logger = null;
	protected Properties m_props = null;
	
	/**
	 * Set logging
	 */
	protected void setLogger(String loggerName, String log4jFileName) {
		logger = Logger.getLogger(loggerName);
		if (log4jFileName != null) {
			File log4jFile = new File (log4jFileName);
			if (log4jFile.canRead()) {
				PropertyConfigurator.configure(log4jFileName);
			} else {
				BasicConfigurator.configure();
			}
		} else {
			BasicConfigurator.configure();
		}
	}	
	
	/**
	 * create and load default properties
	 * @return
	 */
	protected Properties loadProperties(File confFile) {
		Properties defaultProps = new Properties();
		if (confFile != null && confFile.exists() == true) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(confFile);
				defaultProps.load(in);	
				logger.debug("Configuration file " + confFile.getName() + " loaded");
			} catch (FileNotFoundException e) {
				logger.error("Configuration file " + confFile.getName() + " not found", e);
			} catch (IOException e) {
				logger.error("Error encountered while loading configuration file " + confFile.getName(), e);
			}
			finally {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Error encountered", e);
				}
			}
		} else {
			logger.error("Configuration file " + confFile.getName() + " not found");
		}
		return defaultProps;
	}
	
	protected String getProperty(String key) {
		String value = m_props.getProperty(key);
		if (value == null) {
			logger.warn("Could not find property " + key);
			value = "";
		}
		return value.trim();
	}		
}
