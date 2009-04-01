package org.obm.push.backend.obm22;

import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.MSEvent;
import org.obm.push.backend.PIMDataType;
import org.obm.push.backend.obm22.calendar.CalendarBackend;
import org.obm.push.backend.obm22.mail.MailBackend;
import org.obm.push.state.SyncState;

public class ContentsImporter implements IContentsImporter {

	private MailBackend mailBackend;
	private CalendarBackend calBackend;

	public ContentsImporter(MailBackend mailBackend, CalendarBackend calBackend) {
		this.mailBackend = mailBackend;
		this.calBackend = calBackend;
	}
	
	@Override
	public void configure(BackendSession bs, SyncState syncState, Integer conflictPolicy) {
		bs.setState(syncState);
	}

	@Override
	public SyncState getState(BackendSession bs) {
		return bs.getState();
	}

	@Override
	public String importMessageChange(BackendSession bs, String collectionId, String serverId, IApplicationData data) {
		String id = null;
		switch (data.getType()) {
			case CALENDAR:
				id = calBackend.createOrUpdate(bs, collectionId, serverId, (MSEvent) data);
				break;
			case CONTACTS:
				break;
			case EMAIL:
				break;
			case TASKS:
				break;
		}
		return id;
	}

	@Override
	public void importMessageDeletion(BackendSession bs, PIMDataType type, String serverId) {
		// TODO Auto-generated method stub
		switch (type) {
		case CALENDAR:
			calBackend.delete(bs, serverId);
			break;
		case CONTACTS:
			break;
		case EMAIL:
			mailBackend.delete(bs, serverId);
			break;
		case TASKS:
			break;
		}
	}

	@Override
	public void importMessageMove(BackendSession bs, String serverId, String trash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void importMessageReadFlag(BackendSession bs, String serverId, boolean read) {
		// TODO Auto-generated method stub

	}

}
