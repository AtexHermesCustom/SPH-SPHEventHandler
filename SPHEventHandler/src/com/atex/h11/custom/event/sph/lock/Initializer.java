package com.atex.h11.custom.event.sph.lock;

import java.io.File;
import com.unisys.media.cr.adapter.ncm.common.business.interfaces.INCMMetadataNodeManager;
import com.unisys.media.cr.adapter.ncm.common.data.datasource.NCMDataSourceDescriptor;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.common.data.interfaces.IDataSource;
import com.unisys.media.cr.common.data.values.NodeTypePK;
import com.atex.h11.custom.event.sph.InitializerAbstract;

public class Initializer extends InitializerAbstract {
	
	private NCMDataSource hermesDS = null;
	private INCMMetadataNodeManager metaMgr = null;
	
	/**
	 * Creates a new Initializer
	 */
	public Initializer(IDataSource ds) {
		setLogger(Constants.NAME_LOGGER, Constants.log4jConfig);
		m_props = loadProperties(new File(Constants.DefaultConfigurationFile));

		// establish data source
		hermesDS = (NCMDataSource) ds;		
	}
	
	public NCMDataSource getHermesDataSource() {
		return hermesDS;
	}	
	
	public INCMMetadataNodeManager getMetadataManager() {
		if (metaMgr == null) {
			NodeTypePK PK = new NodeTypePK(NCMDataSourceDescriptor.NODETYPE_NCMMETADATA);
			metaMgr = (INCMMetadataNodeManager) getHermesDataSource().getNodeManager(PK);
		}
		return metaMgr;
	}		
	
	public String getLockMetadataGroup() {
		return getProperty("LockMetadataGroup");
	}	
	
	public String getLockMetadataField() {
		return getProperty("LockMetadataField");
	}	
	
	public int getLockedMetadataValue() {
		return Integer.parseInt(getProperty("LockedValue"));
	}
	
	public int getUnlockedMetadataValue() {
		return Integer.parseInt(getProperty("UnlockedValue"));
	}
}
