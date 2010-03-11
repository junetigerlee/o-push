package org.obm.push.backend.obm22;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IHierarchyExporter;
import org.obm.push.backend.ItemChange;
import org.obm.push.backend.PIMDataType;
import org.obm.push.backend.obm22.calendar.CalendarBackend;
import org.obm.push.backend.obm22.contacts.ContactsBackend;
import org.obm.push.backend.obm22.mail.MailBackend;
import org.obm.push.backend.obm22.tasks.TasksBackend;
import org.obm.push.exception.ActiveSyncException;
import org.obm.push.state.SyncState;

public class HierarchyExporter implements IHierarchyExporter {

	private static final Log logger = LogFactory
			.getLog(HierarchyExporter.class);

	private FolderBackend folderExporter;
	private MailBackend mailExporter;
	private CalendarBackend calendarExporter;
	private ContactsBackend contactsBackend;
	private TasksBackend tasksBackend;

	public HierarchyExporter(FolderBackend folderExporter,
			MailBackend mailExporter, CalendarBackend calendarExporter,
			ContactsBackend contactsBackend, TasksBackend tasksBackend) {
		this.folderExporter = folderExporter;
		this.mailExporter = mailExporter;
		this.calendarExporter = calendarExporter;
		this.contactsBackend = contactsBackend;
		this.tasksBackend = tasksBackend;
	}

	@Override
	public void configure(BackendSession bs, String dataClass,
			Integer filterType, SyncState state, int i, int j) {
		logger.info("configure(bs, " + dataClass + ", " + filterType + ", "
				+ state + ", " + i + ", " + j + ")");
		bs.setState(state);
		if (dataClass != null) {
			bs.setDataType(PIMDataType.valueOf(dataClass.toUpperCase()));
		} else {
			bs.setDataType(null);
		}
	}

	@Override
	public SyncState getState(BackendSession bs) {
		return bs.getState();
	}

	private List<ItemChange> getContactsChanges(BackendSession bs) {
		return contactsBackend.getHierarchyChanges(bs);
	}

	private List<ItemChange> getTasksChanges(BackendSession bs) {
		return tasksBackend.getHierarchyChanges(bs);
	}

	private List<ItemChange> getCalendarChanges(BackendSession bs) {
		return calendarExporter.getHierarchyChanges(bs);
	}

	private List<ItemChange> getMailChanges(BackendSession bs) {
		return mailExporter.getHierarchyChanges(bs);
	}

	@Override
	public List<ItemChange> getChanged(BackendSession bs) {
		folderExporter.synchronize(bs);
		LinkedList<ItemChange> changes = new LinkedList<ItemChange>();
		changes.addAll(getCalendarChanges(bs));
		changes.addAll(getMailChanges(bs));
		changes.addAll(getContactsChanges(bs));
		changes.addAll(getTasksChanges(bs));
		return changes;
	}

	@Override
	public int getCount(BackendSession bs) {
		return getChanged(bs).size();
	}

	@Override
	public List<ItemChange> getDeleted(BackendSession bs) {
		return new LinkedList<ItemChange>();
	}

	@Override
	public int getRootFolderId(BackendSession bs) throws ActiveSyncException {
		return folderExporter.getServerIdFor(bs);
	}

}
