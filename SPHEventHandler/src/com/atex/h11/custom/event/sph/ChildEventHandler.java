package com.atex.h11.custom.event.sph;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMObjectBuildProperties;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectEvent;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMStatusPropertyValue;
import com.unisys.media.cr.common.data.interfaces.IDataSource;
import com.unisys.media.cr.common.data.interfaces.INodePK;

public class ChildEventHandler {

	static Logger logger = Logger.getLogger(Constants.NAME_LOGGER); 
	private IDataSource m_ds;
	private Initializer m_init;
	
	public ChildEventHandler(Initializer init, IDataSource ds) {
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
		
		NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
		objProps.setGetByObjId(true);
		
		try {
			// get object
			int objId = event.getObjId();
			NCMObjectPK pk = new  NCMObjectPK(objId);
			NCMObjectValueClient obj = (NCMObjectValueClient) ((NCMDataSource)m_ds).getNode(pk, objProps);
			
			// get object status
			NCMStatusPropertyValue objStatus = 
				(NCMStatusPropertyValue) obj.getLayout().getStatus().getValue();			
						
			if (m_init.isExportStatus(objStatus.getStatus())) {
				// export SP
				exportParentPackage(objId, obj, objStatus);
			}
			else {
				// update the status of the parent SP and sibling objects 
				// to follow that of the current object's
				updateRelatedObjectsStatus(objId, obj, objStatus);
			}
			
		} catch(Exception e) {
			logger.error("Error encountered while handling Object event. Event=" + event.toString(), e);
		}		
	}
	
	private NCMObjectValueClient getParentSP(NCMObjectValueClient obj) {
		NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
		objProps.setGetByObjId(true);
		
		NCMObjectPK pk = new NCMObjectPK(obj.getSpId());
		NCMObjectValueClient sp = (NCMObjectValueClient) ((NCMDataSource)m_ds).getNode(pk, objProps);
		return sp;
	}
	
	/**
	 * update story package's textual children's status so that they
	 * have the same status as the story package 
	 * 
	 */
	private void updateRelatedObjectsStatus(int objId, NCMObjectValueClient obj, NCMStatusPropertyValue objStatus) {		
		try {
			logger.debug("Object saved: name=" + obj.getNCMName() + ", id=" + Integer.toString(objId) + ", type=" + obj.getType()
				+ ", status=" + Short.toString(objStatus.getStatus()));
				
			// get parent SP and change its status
			NCMObjectValueClient sp = getParentSP(obj);		
			// get sp status
			NCMStatusPropertyValue spStatus = (NCMStatusPropertyValue) sp.getLayout().getStatus().getValue();
			// update sp status
			if ((short)objStatus.getStatus() != (short)spStatus.getStatus()) {
				changeStatus(sp, objStatus.getStatus());
			}

			NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
			objProps.setGetByObjId(true);
			
			// get SP child objects and change their statuses
			NCMObjectPK[] childPKs = (NCMObjectPK[]) sp.getChildPKs();
			
			if (childPKs != null) {
				for (int i = 0; i < childPKs.length; i++ ) {
					try {
						// get child
    					NCMObjectPK childPK = new NCMObjectPK(childPKs[i].getObjId());
    					NCMObjectValueClient child = (NCMObjectValueClient) ((NCMDataSource)m_ds).getNode(childPK, objProps);
    					
    					if (m_init.isTextualType(child.getType())) {	// check only "Textual" objects
    						// get child status
    						NCMStatusPropertyValue childStatus = 
    							(NCMStatusPropertyValue) child.getLayout().getStatus().getValue();
    						// update child status
    						if ((short)objStatus.getStatus() != (short)childStatus.getStatus()) {
	    						changeStatus(child, objStatus.getStatus());
    						}
    					}
					} catch (Exception e) {
						logger.error("Error encountered while getting child object: " + Integer.toString(childPKs[i].getObjId()), e);
					}						
				}
			}
			
		} catch(Exception e) {
			logger.error("Error encountered while updating status of related objects. name=" + obj.getNCMName() + ", id=" + Integer.toString(objId), e);
		}
	}
	
	private void changeStatus(NCMObjectValueClient obj, Short newStatusValue) {
		boolean objLocked = false;		
		
		try {
			int objId = getObjIdFromPK(obj.getPK());
			logger.debug("Attempt to update object status: name=" + obj.getNCMName() + ", id=" + objId + ", type=" + obj.getType()
				+ ", new status=" + Short.toString(newStatusValue));

			try {
				// try to lock object
				if (!obj.getLockInfo().isLocked()) {
					obj.lock();	
					objLocked = true;
				}
				else {
					logger.warn("Unable to lock object: name=" + obj.getNCMName() + 
						". Locked by user=" + obj.getLockInfo().getUserName() + " on ws=" + obj.getLockInfo().getWsName() +
						". Status not updated.");
				}						
			}
			catch (Exception e) {
				logger.error("Unable to lock object: " + obj.getNCMName() + ". Status not updated.", e);
			}
			
			if (objLocked) {
				// get object current status
				NCMStatusPropertyValue curStatus = 
					(NCMStatusPropertyValue) obj.getLayout().getStatus().getValue();
				
				obj.changeStatus(obj.getPK(), newStatusValue, curStatus.getExtStatus().shortValue(), 
					curStatus.getComplexStatus().intValue(), curStatus.getAttribute().shortValue(), 
					new short[0]);
				logger.debug("Updated status of object: name=" + obj.getNCMName() + ", id=" + objId + ", type=" + obj.getType()
					+ ", new status=" + Short.toString(newStatusValue));
			}
			
		} catch(Exception e) {
			logger.error("Error encountered while changing status of object: name=" + obj.getNCMName(), e);					
		} finally {
			try {
				if (objLocked) {				
					obj.unlock(false);	// unlock
				}				
			} catch(Exception e) {
				logger.error("Error encountered while unlocking object: name=" + obj.getNCMName(), e);					
			}
		}
	}
	
	private void exportParentPackage(int objId, NCMObjectValueClient obj, NCMStatusPropertyValue expStatus) {		
        URL url = null;
        HttpURLConnection conn = null;  
        
		// get parent SP
    	int spId = obj.getSpId();
		NCMObjectValueClient sp = getParentSP(obj);	       	        
        
        try {				        	
			logger.debug("Send package to web: name=" + sp.getNCMName() + ", id=" + Integer.toString(spId) 
				+ ", status=" + Short.toString(expStatus.getStatus()));
						
			String urlStr = m_init.getExportURL(spId, expStatus.getStatus());
			logger.debug("Call servlet: " + urlStr);
            url = new URL(urlStr);
            
            // Connect
            conn = (HttpURLConnection) url.openConnection();       
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.connect();
           
            if (conn.getResponseCode() == 200) { 	// 200 = OK
            	logger.debug("Package: name=" + sp.getNCMName() + ", id=" + Integer.toString(spId) 
            		+ " exported");            	
            	
            	if (m_init.getStatusAfterExport(expStatus.getStatus()) > 0) {
            		// change status of the current object
            		// changing of status of related objects will follow, after the current object's status is changed
            		changeStatus(obj, m_init.getStatusAfterExport(expStatus.getStatus()));
            	}
            	else {
            		// just change the status of related objects
            		updateRelatedObjectsStatus(objId, obj, expStatus);
            	}
            }
            else {
            	logger.error("Error encountered while sending to web. Error: " + 
            		conn.getResponseCode() + " - " + conn.getResponseMessage());
            }
            
		} catch(Exception e) {
			logger.error("Error encountered while exporting package. name=" + sp.getNCMName() + ", id=" + Integer.toString(spId), e);
		}		
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }		
	}
	
	private int getObjIdFromPK(INodePK pk) {
		String s = pk.toString();
		int delimIdx = s.indexOf(":");
		if (delimIdx >= 0) {
			s = s.substring(0, delimIdx);
		}
		return Integer.parseInt(s);
	}		
}
