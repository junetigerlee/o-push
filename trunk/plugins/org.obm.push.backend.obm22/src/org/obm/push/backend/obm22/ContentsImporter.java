package org.obm.push.backend.obm22;

import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.MSContact;
import org.obm.push.backend.MSEvent;
import org.obm.push.backend.MSEmail;
import org.obm.push.backend.MSTask;
import org.obm.push.backend.PIMDataType;
import org.obm.push.backend.obm22.calendar.CalendarBackend;
import org.obm.push.backend.obm22.contacts.ContactsBackend;
import org.obm.push.backend.obm22.mail.MailBackend;
import org.obm.push.backend.obm22.tasks.TasksBackend;
import org.obm.push.data.calendarenum.AttendeeStatus;
import org.obm.push.exception.ActiveSyncException;

/**
 * 
 * @author adrienp
 * 
 */
public class ContentsImporter implements IContentsImporter {

	private MailBackend mailBackend;
	private CalendarBackend calBackend;
	private ContactsBackend contactBackend;
	private TasksBackend tasksBackend;

	public ContentsImporter(MailBackend mailBackend,
			CalendarBackend calBackend, ContactsBackend contactBackend,
			TasksBackend tasksBackend) {
		this.mailBackend = mailBackend;
		this.calBackend = calBackend;
		this.contactBackend = contactBackend;
		this.tasksBackend = tasksBackend;
	}

	@Override
	public String importMessageChange(BackendSession bs, String collectionId,
			String serverId, String clientId, IApplicationData data)
			throws ActiveSyncException {
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
			id = tasksBackend.createOrUpdate(bs, collectionId, serverId,
					clientId, (MSTask) data);
			break;
		}
		return id;
	}

	@Override
	public void importMessageDeletion(BackendSession bs, PIMDataType type,
			String collectionId, String serverId) {
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
			tasksBackend.delete(bs, serverId);
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

	public String importMoveItem(BackendSession bs, PIMDataType type,
			String srcFolder, String dstFolder, String messageId) {
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
	public void sendEmail(BackendSession bs, byte[] mailContent,
			Boolean saveInSent) {
		mailBackend.sendEmail(bs, mailContent, saveInSent);
	}

	@Override
	public void replyEmail(BackendSession bs, byte[] mailContent,
			Boolean saveInSent, String collectionId, String serverId) {
		mailBackend.replyEmail(bs, mailContent, saveInSent, collectionId,
				serverId);
	}

	@Override
	public String importCalendarUserStatus(BackendSession bs, MSEvent event,
			AttendeeStatus userResponse) {
		return calBackend.updateUserStatus(bs, event, userResponse);
	}

	@Override
	public void forwardEmail(BackendSession bs, byte[] mailContent,
			Boolean saveInSent, String collectionId, String serverId) {
		mailBackend.forwardEmail(bs, mailContent, saveInSent, collectionId,
				serverId);
	}

}
