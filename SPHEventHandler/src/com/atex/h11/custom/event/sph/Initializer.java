package com.atex.h11.custom.event.sph;

import java.io.File;

/**
 * Initialize environment for export to consortium
 *
 */
public class Initializer extends InitializerAbstract {
	
	static int[] m_textualTypes = null;
	static short[] m_exportStatuses = null;
	
	/**
	 * Creates a new Initializer
	 */
	public Initializer() {
		setLogger(Constants.NAME_LOGGER, Constants.log4jConfig);
		m_props = loadProperties(new File(Constants.DefaultConfigurationFile));
		
		try {
			String[] strArr;
			
			// textual object types
			strArr = null;
			strArr = getTextualTypes().split(",");
			m_textualTypes = new int[strArr.length];
			for (int i = 0; i < strArr.length; i++) {
				m_textualTypes[i] = Integer.parseInt(strArr[i].trim());
			}
			
			// web statuses
			strArr = null;
			strArr = getExportStatuses().split(",");
			m_exportStatuses = new short[strArr.length];
			for (int i = 0; i < strArr.length; i++) {
				m_exportStatuses[i] = Short.parseShort(strArr[i].trim());
			}			
			
		} catch (Exception e) {
			logger.error("Error encountered while getting textual object types", e);
		}
	}
	
	/**
	 * get file extension from properties file
	 * @return
	 */	
	public String getOutputFileExtension() {
		return getProperty("OutputFileExtension");
	}
	
	/**
	 * get output folder from properties file
	 * @return
	 */	
	public String getOutputFolder(int eventId, int objType) {
		return getProperty("Event" + eventId + ".Type" + objType + ".OutputFolder");
	}
	
	/**
	 * get Hermes user id to be ignored from properties file
	 * @return
	 */	
	public String getIgnoreUserId(int eventId, int objType) {
		return getProperty("Event" + eventId + ".Type" + objType + ".IgnoreUserId");
	}

	/**
	 * get comma-separated list of Hermes textual object type id's
	 * @return
	 */	
	public String getTextualTypes() {
		return getProperty("TextualTypes");
	}

	/**
	 * returns true or false - whether the passed object type is a textual object type or not
	 * @return
	 */	
	public boolean isTextualType(int objType) {
		for (int i = 0; i < m_textualTypes.length; i++) {
			if (objType == m_textualTypes[i]) {
				return true;
			}
		}	
		return false;
	}
	
	/**
	 * get comma-separated list of status id's where package export should be initiated
	 * @return
	 */	
	public String getExportStatuses() {
		return getProperty("ExportStatuses");
	}	
	
	/**
	 * returns true or false - whether the passed status is an export status
	 * @return
	 */	
	public boolean isExportStatus(short statusId) {
		for (int i = 0; i < m_exportStatuses.length; i++) {
			if (statusId == m_exportStatuses[i]) {
				return true;
			}
		}	
		return false;
	}	
	
	/**
	 * returns true or false - whether to lock the sp or not during processing
	 * @return
	 */	
	public boolean getLockSPSetting() {
		String s = getProperty("LockSP");
		if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes")) {
			return true;
		}
		else {
			return false;
		}
	}		
	
	/**
	 * get status to set after exporting
	 * @return
	 */	
	public short getStatusAfterExport(short status) {
		String statusStr = getProperty("ExportStatus" + Short.toString(status) + ".StatusAfterExport");
		if (statusStr != null && ! statusStr.isEmpty()) {
			return Short.parseShort(statusStr);
		}
		else {
			return 0;
		}
	}		
	
	/**
	 * get export to Web URL
	 * @return
	 */	
	public String getExportURL(int objId, short status) throws Exception {
		// init export url
		String exportURL = getProperty("ExportStatus" + Short.toString(status) + ".ExportURL");
		if (exportURL == null || exportURL.isEmpty()) {
			throw new Exception("Empty or null URL value for ExportStatus" + Integer.toString(status) + ".SendToWebURL");
		}
		// get values from system environment
		exportURL = exportURL.replace("$J2EE_IP", System.getenv("J2EE_IP"));	
		exportURL = exportURL.replace("$J2EE_HTTPPORT", System.getenv("J2EE_HTTPPORT"));
		// pass and replace the object Id to the URL string
		exportURL = exportURL.replace("$SP_ID", Integer.toString(objId)); 
		return exportURL;
	}		
	
}
