package org.obm.push.backend.obm22;

import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.MSContact;
import org.obm.push.backend.MSEvent;
import org.obm.push.backend.MSEmail;
import org.obm.push.backend.PIMDataType;
import org.obm.push.backend.obm22.calendar.CalendarBackend;
import org.obm.push.backend.obm22.contacts.ContactsBackend;
import org.obm.push.backend.obm22.mail.MailBackend;
import org.obm.push.state.SyncState;

public class ContentsImporter implements IContentsImporter {

	private MailBackend mailBackend;
	private CalendarBackend calBackend;
	private ContactsBackend contactBackend;

	public ContentsImporter(MailBackend mailBackend,
			CalendarBackend calBackend, ContactsBackend contactBackend) {
		this.mailBackend = mailBackend;
		this.calBackend = calBackend;
		this.contactBackend = contactBackend;
	}

	@Override
	public void configure(BackendSession bs, SyncState syncState,
			Integer conflictPolicy) {
		bs.setState(syncState);
	}

	@Override
	public SyncState getState(BackendSession bs) {
		return bs.getState();
	}

	@Override
	public String importMessageChange(BackendSession bs, String collectionId,
			String serverId, String clientId, IApplicationData data) {
		String id = null;
		switch (data.getType()) {
		case CALENDAR:
			id = calBackend.createOrUpdate(bs, collectionId, serverId,
					clientId, (MSEvent) data);
			break;
		case CONTACTS:
			id = contactBackend.createOrUpdate(bs, collectionId, serverId,
					clientId, (MSContact) data);
			break;
		case EMAIL:
			id = mailBackend.createOrUpdate(bs, collectionId, serverId,
					clientId, (MSEmail) data);
			break;
		case TASKS:
			break;
		}
		return id;
	}

	@Override
	public void importMessageDeletion(BackendSession bs, PIMDataType type,
			String collectionId, String serverId) {
		// TODO Auto-generated method stub
		switch (type) {
		case CALENDAR:
			calBackend.delete(bs, collectionId, serverId);
			break;
		case CONTACTS:
			contactBackend.delete(bs, serverId);
			break;
		case EMAIL:
			mailBackend.delete(bs, serverId);
			break;
		case TASKS:
			break;
		}
	}

	@Override
	public void importMessageMove(BackendSession bs, String serverId,
			String trash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void importMessageReadFlag(BackendSession bs, String serverId,
			boolean read) {
		// TODO Auto-generated method stub

	}

	public String importMoveItem(BackendSession bs, PIMDataType type, String srcFolder, String dstFolder, String messageId) {
		switch (type) {
		case CALENDAR:
			break;
		case CONTACTS:
			break;
		case EMAIL:
			return mailBackend.move(bs, srcFolder, dstFolder, messageId);
		case TASKS:
			break;
		}
		return null;
	}
	
	@Override
	public void sendEmail(BackendSession bs, byte[] mailContent) {
		mailBackend.sendEmail(bs, mailContent);
	}

}