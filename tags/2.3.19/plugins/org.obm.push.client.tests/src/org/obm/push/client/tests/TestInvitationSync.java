package org.obm.push.client.tests;

import java.io.InputStream;

import org.obm.push.utils.DOMUtils;
import org.obm.sync.push.client.Folder;
import org.obm.sync.push.client.FolderSyncResponse;
import org.obm.sync.push.client.FolderType;
import org.obm.sync.push.client.SyncResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestInvitationSync extends OPClientTests {

	public void testSync() throws Exception {
		testOptions();
		FolderSyncResponse fsr = testInitialFolderSync();
		Folder inbox = fsr.getFolders().get(FolderType.DEFAULT_INBOX_FOLDER);
		Folder calendarFolder = fsr.getFolders().get(
				FolderType.DEFAULT_CALENDAR_FOLDER);
		SyncResponse syncResp = testInitialSync(inbox, calendarFolder);
		InputStream in = null;
		Document doc = null;
		
		in = loadDataFile("InvitationItemEstimate.xml");
		doc = DOMUtils.parse(in);
		replace(doc, calendarFolder, inbox, syncResp);
		
		Document ret = postXml("ItemEstimate", doc, "GetItemEstimate");
		assertNotNull(ret);

		 in = loadDataFile("InvitationSync.xml");
		 doc = DOMUtils.parse(in);
		 replace(doc, calendarFolder, inbox, syncResp);
		
		testSync(doc);

	}
	
	private void replace(Document doc, Folder calendarFolder, Folder inbox,SyncResponse syncResp){
		NodeList nl = doc.getElementsByTagName("Collection");
		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			Element syncKey = DOMUtils.getUniqueElement(e, "SyncKey");
			if(syncKey == null){
				syncKey = DOMUtils.getUniqueElement(e, "AirSync:SyncKey");
			}
			if ("CALENDAR".equals(syncKey.getTextContent())) {
				syncKey.setTextContent(getSyncKey(calendarFolder.getServerId(),
						syncResp.getCollections()));
			} else if ("INBOX".equals(syncKey.getTextContent())) {
				syncKey.setTextContent(getSyncKey(inbox.getServerId(), syncResp
						.getCollections()));
			} else {
				fail(syncKey.getTextContent());
			}

			Element collectionId = DOMUtils.getUniqueElement(e, "CollectionId");
			if ("CALENDAR".equals(collectionId.getTextContent())) {
				collectionId.setTextContent(calendarFolder.getServerId());
			} else if ("INBOX".equals(collectionId.getTextContent())) {
				collectionId.setTextContent(inbox.getServerId());
			} else {
				fail(collectionId.getTextContent());
			}
		}
	}

	public void testMailSync2() throws Exception {
		InputStream in = loadDataFile("FolderSyncRequest.xml");
		Document doc = DOMUtils.parse(in);
		Document ret = postXml("FolderHierarchy", doc, "FolderSync");
		assertNotNull(ret);

		in = loadDataFile("EmailSyncRequest.xml");
		doc = DOMUtils.parse(in);
		Element synckeyElem = DOMUtils.getUniqueElement(doc
				.getDocumentElement(), "SyncKey");
		synckeyElem.setTextContent("0");
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);

		String sk = DOMUtils.getUniqueElement(ret.getDocumentElement(),
				"SyncKey").getTextContent();
		in = loadDataFile("GetItemEstimateRequestEmail.xml");
		doc = DOMUtils.parse(in);
		synckeyElem = DOMUtils.getUniqueElement(doc.getDocumentElement(),
				"AirSync:SyncKey");
		synckeyElem.setTextContent(sk);
		ret = postXml("ItemEstimate", doc, "GetItemEstimate");
		assertNotNull(ret);

		// sk = DOMUtils.getUniqueElement(ret.getDocumentElement(), "SyncKey")
		// .getTextContent();
		in = loadDataFile("EmailSyncRequest2.xml");
		doc = DOMUtils.parse(in);
		synckeyElem = DOMUtils.getUniqueElement(doc.getDocumentElement(),
				"SyncKey");
		synckeyElem.setTextContent(sk);
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);

		NodeList nl = ret.getDocumentElement().getElementsByTagName(
				"ApplicationData");
		System.out.println("received " + nl.getLength()
				+ " events from server.");
		assertTrue(nl.getLength() > 0);

		sk = DOMUtils.getUniqueElement(ret.getDocumentElement(), "SyncKey")
				.getTextContent();
		in = loadDataFile("EmailSyncRequest2.xml");
		doc = DOMUtils.parse(in);
		synckeyElem = DOMUtils.getUniqueElement(doc.getDocumentElement(),
				"SyncKey");
		synckeyElem.setTextContent(sk);
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);

		nl = ret.getDocumentElement().getElementsByTagName("ApplicationData");
		System.out.println("received " + nl.getLength()
				+ " events from server.");
		assertTrue(nl.getLength() == 0);

	}

	public void testMailSyncMultiBodyPref() throws Exception {
		InputStream in = loadDataFile("FolderSyncRequest.xml");
		Document doc = DOMUtils.parse(in);
		Document ret = postXml("FolderHierarchy", doc, "FolderSync");
		assertNotNull(ret);

		in = loadDataFile("EmailSyncRequest.xml");
		doc = DOMUtils.parse(in);
		Element synckeyElem = DOMUtils.getUniqueElement(doc
				.getDocumentElement(), "SyncKey");
		synckeyElem.setTextContent("0");
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);

		String sk = DOMUtils.getUniqueElement(ret.getDocumentElement(),
				"SyncKey").getTextContent();
		in = loadDataFile("EmailSyncRequestMultipleBodyPref.xml");
		doc = DOMUtils.parse(in);
		synckeyElem = DOMUtils.getUniqueElement(doc.getDocumentElement(),
				"SyncKey");
		synckeyElem.setTextContent(sk);
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);

	}
}
