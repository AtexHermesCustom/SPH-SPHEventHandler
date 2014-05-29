package com.atex.h11.custom.event.sph;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectEvent;
import com.unisys.media.cr.common.data.interfaces.IDataSource;

import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMObjectBuildProperties;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMLayoutValueClient;

public class ObjectEventDumper {
	
	static Logger logger = Logger.getLogger(Constants.NAME_LOGGER); 
	private IDataSource m_ds;
	private Initializer m_init;
	
	public ObjectEventDumper(Initializer init, IDataSource ds) {
		setDataSource(ds);
		setInitializer(init);
	}
	
	private void setDataSource(IDataSource ds) {
	 	m_ds = ds;
	}

	private void setInitializer(Initializer init) {
	 	m_init = init;
	}
	
	public void handleObjectEvent(IObjectEvent event) {
		logger.debug("Object event received: " + event.toString()); 
		
		String modifier = Integer.toString(event.getModifingId());
		// check if the event by the specific modifier should be ignored 
		if (! m_init.getIgnoreUserId(event.getJEvent().EventId, event.getObjectType()).equals(modifier)) {
			dumpObjectEventInfo(event);
		}
		else {
			logger.debug("Ignore event " + event.getJEvent().EventId + " by user id " + modifier);
		}
	}
	
	/**
	 * write relevant Object Event info to a file
	 * 
	 */	
	private void dumpObjectEventInfo(IObjectEvent event) { 
		NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
		objProps.setGetByObjId(true);
		
		try {
			// get object and layout
			NCMObjectPK pk = new  NCMObjectPK(event.getObjId());
			NCMObjectValueClient obj = (NCMObjectValueClient) ((NCMDataSource)m_ds).getNode(pk, objProps);
			NCMLayoutValueClient layout = obj.getLayout();

			try {
				// create file containing event info
				// the file will be processed through hermdaemon
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
				Date ts = new Date();
				String outputFileName = 
					m_init.getOutputFolder(event.getJEvent().EventId, event.getObjectType()) 
					+ "/" + event.getObjId() + "-" + formatter.format(ts) 
					+ "." + m_init.getOutputFileExtension();
				
				
				Properties props = new Properties();
				
				// include data required by event scripts
				// if more data is needed, add them here
				props.setProperty("EventId", Integer.toString(event.getJEvent().EventId));
				props.setProperty("ObjId", Integer.toString(event.getObjId()));
				props.setProperty("StatusId", Integer.toString(event.getStatusId()));
				props.setProperty("Modifier", Integer.toString(event.getModifingId()));
				props.setProperty("PubDate", Integer.toString(event.getPubData()));
				
				props.setProperty("ObjName", obj.getNCMName());
				props.setProperty("ObjType", Integer.toString(obj.getType()));
				props.setProperty("LevelId", new String(obj.getLevelId()));
				props.setProperty("EditionId", Integer.toString(obj.getEditionId()));
				props.setProperty("ExpPubDate", Integer.toString(obj.getExpPubDate())); 

				props.setProperty("LayId", (layout != null ? Integer.toString(layout.getLayId()) : ""));
				props.setProperty("PageId", (layout != null ? Integer.toString(layout.getPageId()) : ""));
				props.setProperty("LayReference", 
					(layout != null ? (layout.getReference() != null ? layout.getReference() : "") : ""));	

				props.store(new FileOutputStream(new File(outputFileName)), null);
				logger.info("Event: id=" + event.getJEvent().EventId 
					+ ". Object: name=" + obj.getNCMName() + ", id=" + event.getObjId() 
					+ ", type=" + obj.getType() + ", status=" + Integer.toString(event.getStatusId())
					+ ". Event file created: " + outputFileName);
			}
			catch (IOException e){
				logger.error("Error encountered while writing event info. Event=" + event.toString(), e);
			}
			catch (Exception e) {
				logger.error("Error encountered while writing event info. Event=" + event.toString(), e);
			}			
		} catch(Exception e) {
			logger.error("Error encountered while getting object and layout. Event=" + event.toString(), e);
		}
	}

}
