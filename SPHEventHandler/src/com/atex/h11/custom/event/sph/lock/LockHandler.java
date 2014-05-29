package com.atex.h11.custom.event.sph.lock;

import java.util.Vector;

import org.apache.log4j.Logger;

import com.atex.h11.custom.event.sph.lock.Constants;
import com.atex.h11.custom.event.sph.lock.Initializer;
import com.unisys.media.cr.adapter.ncm.common.business.interfaces.INCMMetadataNodeManager;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMCustomMetadataPK;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMCustomMetadataJournal;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMMetadataPropertyValue;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMObjectBuildProperties;
import com.unisys.media.cr.adapter.ncm.common.event.config.ObjectEventHandler;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectEvent;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectFullEvent;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectMultilinkEvent;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.cr.common.data.interfaces.IDataSource;
import com.unisys.media.cr.common.data.interfaces.INodePK;
import com.unisys.media.cr.common.data.types.IPropertyDefType;
import com.unisys.media.extension.common.exception.NodeAlreadyLockedException;
import com.unisys.media.ncm.cfg.common.data.values.MetadataSchemaValue;
import com.unisys.media.ncm.cfg.model.values.UserHermesCfgValueClient;

public class LockHandler extends ObjectEventHandler {

	static Logger logger = Logger.getLogger(Constants.NAME_LOGGER); 
	private static boolean isInitialized = false;
	private static Initializer m_init = null;		
	
	private void initialize(IDataSource ds) {
		System.out.println("Initializing " + this.getClass().getName() + "...");
		m_init = new Initializer(ds);
		System.out.println("Initialized " + this.getClass().getName() + " OK");
		isInitialized = true;
	}	
	
	@Override
	public void handleEvent(IDataSource ds, IObjectFullEvent event) {
		if (!isInitialized) {
			initialize(ds);
		}		
		handleLockEvent(event);		
	}
	
	@Override
	public void handleEvent(IDataSource ds, IObjectEvent event) {
		if (!isInitialized) {
			initialize(ds);
		}		
		handleLockEvent(event);		
	}
	
	@Override
	public void handleEvent(IDataSource ds, IObjectMultilinkEvent event) {
		if (!isInitialized) {
			initialize(ds);
		}		
		handleLockEvent(event);
	}
	
	private void handleLockEvent(IObjectEvent event) {
		String modifier = Integer.toString(event.getModifingId());
		if (! m_init.getIgnoreUserId(event.getJEvent().EventId).equals(modifier)) {
			return;		// ignore event initiated by a particular user
		}
		
		if (event.getJEvent().EventId == Constants.LOCK_OBJ) {
			logger.debug("Lock event received: " + event.toString()); 			
		}
		else if (event.getJEvent().EventId == Constants.UNLOCK_OBJ) {
			logger.debug("Unlock event received: " + event.toString()); 
		}
			
		NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
		objProps.setGetByObjId(true);
		objProps.setDoNotChekPermissions(true);
		objProps.setIncludeMetadataGroups(new Vector<String>());
		
		// get object
		NCMObjectPK objPK = new  NCMObjectPK(event.getObjId());
		NCMObjectValueClient obj = (NCMObjectValueClient) m_init.getHermesDataSource().getNode(objPK, objProps);
		
		// get parent story package
		NCMObjectPK spPK = new  NCMObjectPK(obj.getSpId());
		NCMObjectValueClient sp = (NCMObjectValueClient) m_init.getHermesDataSource().getNode(spPK, objProps);
		
		try {
			boolean metaValue = false;
			if (event.getJEvent().EventId == Constants.LOCK_OBJ) {
				metaValue = true;
			}
			else if (event.getJEvent().EventId == Constants.UNLOCK_OBJ) {
				metaValue = false;
			}
			else {
				throw new Exception("Invalid event id: " + event.getJEvent().EventId);
			}
			
			// update Lock indicator metadata 
			setMetadata(sp, m_init.getLockMetadataGroup(), m_init.getLockMetadataField(), Boolean.toString(metaValue));
			
		} catch (Exception e) {
			logger.error("handleLockEvent: Error encountered for Object [" + 
					getObjIdFromPK(sp.getPK()) + "," + sp.getNCMName() + "," + sp.getType() + "]", e);
		}			
	}
	
	private void setMetadata(NCMObjectValueClient objVC, String metaGroup, String metaField, String metaValue) {
		String objName = objVC.getNCMName();
		Integer objId = getObjIdFromPK(objVC.getPK());
		logger.debug("setMetadata: Object [" + objId.toString() + "," + objName + "," + Integer.toString(objVC.getType()) + "]" +
			", Meta=" + metaGroup + "." + metaField + ", Value=" + metaValue);
		
		NCMDataSource hermesDS = m_init.getHermesDataSource();		
		UserHermesCfgValueClient cfg = hermesDS.getUserHermesCfg();
		
		// Get from configuration the schemaId using schemaName for metadata
		MetadataSchemaValue schema = cfg.getMetadataSchemaByName(metaGroup);
		int schemaId = schema.getId();
		
		// Get metadata property
		IPropertyDefType metaGroupDefType = hermesDS.getPropertyDefType(metaGroup);
		//IPropertyValueClient metaGroupPK = objVC.getProperty(metaGroupDefType.getPK());		
		
		INCMMetadataNodeManager metaMgr = m_init.getMetadataManager();
		NCMMetadataPropertyValue pValue = new NCMMetadataPropertyValue(
				metaGroupDefType.getPK(), null, schema);
		pValue.setMetadataValue(metaField, metaValue);
		NCMCustomMetadataPK cmPk = new NCMCustomMetadataPK(
				objId, (short) objVC.getType(), schemaId);
		schemaId = schema.getId();
		NCMCustomMetadataPK[] nodePKs = new NCMCustomMetadataPK[] { cmPk };
		
		try {
			try {
				metaMgr.lockMetadataGroup(schemaId, nodePKs);
			} catch (NodeAlreadyLockedException e) {
			}
			NCMCustomMetadataJournal j = new NCMCustomMetadataJournal();
			j.setCreateDuringUpdate(true);
			metaMgr.updateMetadataGroup(schemaId, nodePKs, pValue, j);
			logger.debug("setMetadata: Update metadata successful for [" + objId.toString() + "," + objName + "," + Integer.toString(objVC.getType()) + "]");
		} catch (Exception e) {
			logger.error("setMetadata: Update metadata failed for [" + objId.toString() + "," + objName + "," + Integer.toString(objVC.getType()) + "]", e); 
		} finally {
			try {
				metaMgr.unlockMetadataGroup(schemaId, nodePKs);
			} catch (Exception e) {
			}
		}				
	}

	private int getObjIdFromPK(INodePK pk) {
		String s = pk.toString();
		int delimIdx = s.indexOf(":");
		if (delimIdx >= 0)
			s = s.substring(0, delimIdx);
		return Integer.parseInt(s);
	}	
	
}
