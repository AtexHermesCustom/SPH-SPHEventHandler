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
import com.unisys.media.extension.common.exception.MediaException;

public class SPEventHandler {

	static Logger logger = Logger.getLogger(Constants.NAME_LOGGER); 
	private IDataSource m_ds;
	private Initializer m_init;
	
	public SPEventHandler(Initializer init, IDataSource ds) {
		setDataSource(ds);
		setInitializer(init);
	}
	
	private void setDataSource(IDataSource ds) {
	 	m_ds = ds;
	}

	private void setInitializer(Initializer init) {
	 	m_init = init;
	}	
	
	public void handleSPEvent(IObjectEvent event) {
		logger.debug("SP event received: " + event.toString()); 
		
		NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
		objProps.setGetByObjId(true);
		
		try {
			// get SP object
			int objId = event.getObjId();
			NCMObjectPK pk = new  NCMObjectPK(objId);
			NCMObjectValueClient sp = (NCMObjectValueClient) ((NCMDataSource)m_ds).getNode(pk, objProps);
			
			// get SP status
			NCMStatusPropertyValue spStatus = 
				(NCMStatusPropertyValue) sp.getLayout().getStatus().getValue();			
			
			if (m_init.isExportStatus(spStatus.getStatus())) {
				// export SP
				exportPackage(objId, sp, spStatus);
			}
			else {
				// update the SP children's status to follow the parent SP's status
				updateChildrenStatus(objId, sp, spStatus);
			}
			
		} catch(Exception e) {
			logger.error("Error encountered while handling SP event. Event=" + event.toString(), e);
		}		
	}
	
	/**
	 * update story package's textual children's status so that they
	 * have the same status as the story package 
	 * 
	 */
	private void updateChildrenStatus(int objId, NCMObjectValueClient sp, NCMStatusPropertyValue spStatus) {		
		boolean spLocked = false;	
		
		try {
			logger.debug("Package saved: name=" + sp.getNCMName() + ", id=" + Integer.toString(objId)
				+ ", status=" + Short.toString(spStatus.getStatus()));

			try {
				// try to lock the package. 
				// this is not really necessary to update the children's status.
				// locking the package is used as an indicator that its children are being updated.
				if (!sp.getLockInfo().isLocked()) {
					sp.lock();
					spLocked = true;
				}
				else {
					logger.warn("Unable to lock package: name=" + sp.getNCMName() + 
						". Locked by user=" + sp.getLockInfo().getUserName() + " on ws=" + sp.getLockInfo().getWsName());
				}				
			}
			catch (Exception e) {
				logger.error("Unable to lock package: " + sp.getNCMName() + ".", e);
			}			
			
			NCMObjectBuildProperties objProps = new NCMObjectBuildProperties();
			objProps.setGetByObjId(true);
			
			// get SP child objects
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
    						
    						if ((short)spStatus.getStatus() != (short)childStatus.getStatus()) {
	    						// update object status to follow that of the SP
	    						changeStatus(child, spStatus.getStatus());
    						}
    					}
					} catch (Exception e) {
						logger.error("Error encountered while getting child object: " + Integer.toString(childPKs[i].getObjId()), e);
					}						
				}
			}
			
		} catch(Exception e) {
			logger.error("Error encountered while updating status of child objects. name=" + sp.getNCMName() + ", id=" + Integer.toString(objId), e);
		} finally {
			try {
				if (spLocked) {				
					sp.unlock(false);	// unlock
				}				
			} catch(Exception e) {
				logger.error("Error encountered while unlocking object: " + sp.getNCMName(), e);					
			}
		}
	}
	
	private void changeStatus(NCMObjectValueClient obj, Short newStatusValue) {
		boolean objLocked = false;		
		
		try {
			logger.debug("Attempt to update object status: name=" + obj.getNCMName() + ", id=" + obj.getPK().toString()
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
				logger.debug("Updated status of object: name=" + obj.getNCMName() + ", id=" + obj.getPK().toString()
					+ ", new status=" + Short.toString(newStatusValue));
			}
			
		} catch(MediaException e) {
			logger.error("Error encountered while changing status of object: " + obj.getNCMName(), e);
		} catch(Exception e) {
			logger.error("Error encountered while changing status of object: " + obj.getNCMName(), e);					
		} finally {
			try {
				if (objLocked) {				
					obj.unlock(false);	// unlock
				}				
			} catch(Exception e) {
				logger.error("Error encountered while unlocking object: " + obj.getNCMName(), e);					
			}
		}
	}
	
	private void exportPackage(int objId, NCMObjectValueClient sp, NCMStatusPropertyValue spStatus) {		
        URL url = null;
        HttpURLConnection conn = null;  
        
        try {				
			logger.debug("Send package to web: name=" + sp.getNCMName() + ", id=" + Integer.toString(objId) 
				+ ", status=" + Short.toString(spStatus.getStatus()));
						
			String urlStr = m_init.getExportURL(objId, spStatus.getStatus());
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
            	logger.debug("Package: name=" + sp.getNCMName() + ", id=" + Integer.toString(objId) 
            		+ " exported");
            	
            	
            	if (m_init.getStatusAfterExport(spStatus.getStatus()) > 0) {
            		// change status of package
            		// changing of status of child objects will follow, after the package's status is changed
            		changeStatus(sp, m_init.getStatusAfterExport(spStatus.getStatus()));
            	}
            	else {
            		// just change the status of child objects
            		updateChildrenStatus(objId, sp, spStatus);
            	}
            }
            else {
            	logger.error("Error encountered while sending to web. Error: " + 
            		conn.getResponseCode() + " - " + conn.getResponseMessage());
            }
            
		} catch(Exception e) {
			logger.error("Error encountered while exporting package. name=" + sp.getNCMName() + ", id=" + Integer.toString(objId), e);
		}		
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }		
	}
}
