package org.obm.sync.push.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestFullSync extends AbstractPushTest {

	public void testFullSync() throws Exception {
		InputStream in = loadDataFile("FolderSyncRequest.xml");
		Document doc = DOMUtils.parse(in);
		Document ret = postXml("FolderHierarchy", doc, "FolderSync");
		assertNotNull(ret);
		
		in = loadDataFile("FullSyncRequest.xml");
		doc = DOMUtils.parse(in);
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);
		
		Map<String,String> sks = processCollection(ret.getDocumentElement());
		in = loadDataFile("FullGetEstimateRequest.xml");
		doc = DOMUtils.parse(in);
		fillSyncKey(doc.getDocumentElement(), sks);
		DOMUtils.logDom(doc);
		ret = postXml("ItemEstimate", doc, "GetItemEstimate");
		assertNotNull(ret);

		in = loadDataFile("FullSyncRequest2.xml");
		doc = DOMUtils.parse(in);
		fillSyncKey(doc.getDocumentElement(), sks);
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);

		SyncPush push1 = new SyncPush(1, ret);
		SyncPush push2 = new SyncPush(2, ret);
		PingPush push3 = new PingPush(3);
		PingPush push4 = new PingPush(4);
		
		push1.start();
		push2.start();
		push3.start();
		push4.start();
		System.out.println("Avant wait");
		Thread.sleep(5000);
		System.out.println("Apprès wait");
		 sks = processCollection(ret.getDocumentElement());
		in = loadDataFile("FullSyncCalAdd.xml");
		doc = DOMUtils.parse(in);
		fillSyncKey(doc.getDocumentElement(), sks);
		Element cliidElem = DOMUtils.getUniqueElement(doc.getDocumentElement(),
				"ClientId");
		
		cliidElem.setTextContent("" + new Random().nextInt(999999999));
		DOMUtils.logDom(doc);
		ret = postXml("AirSync", doc, "Sync");
		assertNotNull(ret);
		
		while(!push1.hasResp() || !push2.hasResp() ||!push3.hasResp() || !push4.hasResp() ){
		}
	}
	
	private void fillSyncKey(Element root, Map<String, String> sks) {
		NodeList nl = root.getElementsByTagName("Collection");

		for (int i = 0; i < nl.getLength(); i++) {
			Element col = (Element) nl.item(i);
			String collectionId = DOMUtils.getElementText(col, "CollectionId");
			String syncKey = sks.get(collectionId);
			Element synckeyElem = DOMUtils.getUniqueElement(col,
			"SyncKey");
			if(synckeyElem == null){
				synckeyElem = DOMUtils.getUniqueElement(col,
				"AirSync:SyncKey");
			}
			synckeyElem.setTextContent(syncKey);
		}
		
	}

	private Map<String, String> processCollection(Element root) {
		Map<String,String> ret = new HashMap<String, String>();
		NodeList nl = root.getElementsByTagName("Collection");

		for (int i = 0; i < nl.getLength(); i++) {	
			Element col = (Element) nl.item(i);
			String collectionId = DOMUtils.getElementText(col, "CollectionId");
			String syncKey  = DOMUtils.getElementText(col, "SyncKey");
			ret.put(collectionId, syncKey);
		}
		return ret;
	}
	
	private class SyncPush extends Thread{
		
		private int num;
		private Document lastResponse;
		private Boolean resp;
		
		public SyncPush(int num, Document lastResponse){
			this.num = num;
			this.lastResponse = lastResponse;
			this.resp = false;
		}

		@Override
		public void run() {
			super.run();
			System.out.println("RUN "+num);
			Map<String,String> sks = processCollection(lastResponse.getDocumentElement());
			InputStream in = loadDataFile("FullSyncRequest3.xml");
			Document doc;
			try {
				doc = DOMUtils.parse(in);
				fillSyncKey(doc.getDocumentElement(), sks);
				DOMUtils.logDom(doc);
				postXml("AirSync", doc, "Sync");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				resp = true;
			}
		}
		
		public synchronized Boolean hasResp() {
			return resp;
		}
		
	}
	
	private class PingPush extends Thread{
		
		private int num;
		private Boolean resp;
		
		public PingPush(int num){
			this.num = num;
			this.resp = false;
		}

		@Override
		public void run() {
			super.run();
			System.out.println("RUN "+num);
			InputStream in = loadDataFile("FullPingRequest.xml");
			Document doc;
			try {
				doc = DOMUtils.parse(in);
				DOMUtils.logDom(doc);
				postXml("Ping", doc, "Ping");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				resp = true;
			}
		}
		
		public synchronized Boolean hasResp() {
			return resp;
		}
		
	}
}
