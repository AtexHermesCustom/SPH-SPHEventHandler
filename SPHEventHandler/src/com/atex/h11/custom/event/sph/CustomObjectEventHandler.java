package com.atex.h11.custom.event.sph;

import com.atex.h11.custom.event.sph.Constants;
import com.unisys.media.cr.adapter.ncm.common.event.config.ObjectEventHandler;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectEvent;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectFullEvent;
import com.unisys.media.cr.adapter.ncm.common.event.interfaces.IObjectMultilinkEvent;
import com.unisys.media.cr.common.data.interfaces.IDataSource;
import com.unisys.media.cr.adapter.ncm.common.data.types.NCMObjectNodeType;
import com.unisys.media.extension.common.constants.ApplicationConstants;

public class CustomObjectEventHandler extends ObjectEventHandler {
	
	private static boolean isInitialized = false;
	private static Initializer m_init = null;	
		
	private void initialize() {
		System.out.println("Initializing " + this.getClass().getName() + "...");
		m_init = new Initializer();
		System.out.println("Initialized " + this.getClass().getName() + " OK");
		isInitialized = true;
	}
	
	@Override
	public void handleEvent(IDataSource ds, IObjectFullEvent event) {
		if (!isInitialized) {
			initialize();
		}
		processEvent(ds, event);
	}
	
	@Override
	public void handleEvent(IDataSource ds, IObjectMultilinkEvent event) {
		if (!isInitialized) {
			initialize();
		}
		processEvent(ds, event);
	}

	@Override
	public void handleEvent(IDataSource ds, IObjectEvent event) { 
		if (!isInitialized) {
			initialize();
		}
		processEvent(ds, event);
	}
	
	private void processEvent(IDataSource ds, IObjectEvent event) {
		// events for story packages
		if (event.getObjectType() == NCMObjectNodeType.OBJ_STORY_PACKAGE) {
			// trigger the handler only for:
			// 1. object save (in any application)
			// 2. object move/transfer (only if done in Media Desktop).  move/transfer in Newsroom triggers a Save event
			if (event.getJEvent().EventId == Constants.SAVE_OBJ ||
				(event.getJEvent().EventId == Constants.MOVE_OBJ && 
				 event.getJEvent().AppId == Integer.parseInt(ApplicationConstants.APP_MD_PRODUCTION_ID))) {
				new SPEventHandler(m_init, ds).handleSPEvent(event);
			}
		}
		// events for images
		else if (event.getObjectType() == NCMObjectNodeType.OBJ_PHOTO) {
			// trigger handler only for:
			// 1. object save
			if (event.getJEvent().EventId == Constants.SAVE_OBJ) {
				new ObjectEventDumper(m_init, ds).handleObjectEvent(event);
			}
		}
		// events for ads
		else if (event.getObjectType() == NCMObjectNodeType.OBJ_PUB) {
			// trigger handler only for:
			// 1. layout save
			// 2. unlink
			if (event.getJEvent().EventId == Constants.LAYOUTSAVE_OBJ || event.getJEvent().EventId == Constants.UNLINK_OBJ) {
				new ObjectEventDumper(m_init, ds).handleObjectEvent(event);
			}
		}
	}
}
