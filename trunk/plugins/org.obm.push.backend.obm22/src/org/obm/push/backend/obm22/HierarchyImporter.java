package org.obm.push.backend.obm22;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.push.backend.IHierarchyImporter;
import org.obm.push.backend.ServerId;
import org.obm.push.backend.SyncFolder;
import org.obm.push.state.SyncState;


public class HierarchyImporter implements IHierarchyImporter {

	private static final Log logger = LogFactory.getLog(HierarchyImporter.class);
	
	@Override
	public void configure(SyncState state) {
		// TODO Auto-generated method stub
		logger.info("configure("+state+")");
	}

	@Override
	public ServerId importFolderChange(SyncFolder sf) {
		// TODO Auto-generated method stub
		logger.info("importFolderChange("+sf+")");
		return null;
	}

	@Override
	public ServerId importFolderDeletion(SyncFolder sf) {
		// TODO Auto-generated method stub
		logger.info("importFolderDeletion("+sf+")");
		return null;
	}

}
