package org.obm.push.backend.obm22;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.IHierarchyExporter;
import org.obm.push.backend.IHierarchyImporter;
import org.obm.push.backend.SyncCollection;
import org.obm.push.backend.obm22.calendar.CalendarBackend;
import org.obm.push.backend.obm22.mail.MailExporter;
import org.obm.push.provisioning.Policy;

public class OBMBackend implements IBackend {

	private IHierarchyImporter hImporter;
	private IContentsImporter cImporter;
	private IHierarchyExporter exporter;
	private IContentsExporter contentsExporter;

	private static final Log logger = LogFactory.getLog(OBMBackend.class);

	public OBMBackend() {
		MailExporter mailExporter = new MailExporter();
		CalendarBackend calendarExporter = new CalendarBackend();

		hImporter = new HierarchyImporter();
		exporter = new HierarchyExporter(mailExporter, calendarExporter);
		cImporter = new ContentsImporter(mailExporter, calendarExporter);
		contentsExporter = new ContentsExporter(mailExporter, calendarExporter);
	}

	@Override
	public IHierarchyImporter getHierarchyImporter(BackendSession bs) {
		return hImporter;
	}

	@Override
	public IHierarchyExporter getHierarchyExporter(BackendSession bs) {
		return exporter;
	}

	@Override
	public IContentsImporter getContentsImporter(String collectionId,
			BackendSession bs) {
		return cImporter;
	}

	@Override
	public String getWasteBasket() {
		return "Trash";
	}

	@Override
	public IContentsExporter getContentsExporter(BackendSession bs) {
		return contentsExporter;
	}

	@Override
	public Policy getDevicePolicy(BackendSession bs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SyncCollection> waitForChanges(BackendSession bs,
			Set<SyncCollection> toMonitor, long interval) {
		try {
			logger.info("sleeping " + (interval / 1000) + "sec");
			Thread.sleep(interval);
		} catch (Throwable t) {
		}

		// TODO Auto-generated method stub
		return null;
	}

}
