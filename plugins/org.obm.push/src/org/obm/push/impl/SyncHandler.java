package org.obm.push.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mortbay.util.ajax.Continuation;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.DataDelta;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContentsImporter;
import org.obm.push.backend.ItemChange;
import org.obm.push.backend.PIMDataType;
import org.obm.push.backend.SyncCollection;
import org.obm.push.data.CalendarDecoder;
import org.obm.push.data.ContactsDecoder;
import org.obm.push.data.EncoderFactory;
import org.obm.push.data.IDataDecoder;
import org.obm.push.data.IDataEncoder;
import org.obm.push.state.StateMachine;
import org.obm.push.state.SyncState;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//<?xml version="1.0" encoding="UTF-8"?>
//<Sync>
//<Collections>
//<Collection>
//<Class>Contacts</Class>
//<SyncKey>ff16677f-ee9c-42dc-a562-709f899c8d31</SyncKey>
//<CollectionId>obm://contacts/user@domain</CollectionId>
//<DeletesAsMoves/>
//<GetChanges/>
//<WindowSize>100</WindowSize>
//<Options>
//<Truncation>4</Truncation>
//<RTFTruncation>4</RTFTruncation>
//<Conflict>1</Conflict>
//</Options>
//</Collection>
//</Collections>
//</Sync>

public class SyncHandler extends WbxmlRequestHandler {

	public static final Integer SYNC_TRUNCATION_ALL = 9;

	private Map<String, IDataDecoder> decoders;

	private EncoderFactory encoders;

	public SyncHandler(IBackend backend) {
		super(backend);
		this.decoders = new HashMap<String, IDataDecoder>();
		decoders.put("Contacts", new ContactsDecoder());
		decoders.put("Calendar", new CalendarDecoder());
		this.encoders = new EncoderFactory();
	}

	@Override
	public void process(Continuation continuation, BackendSession bs,
			Document doc, Responder responder) {
		logger.info("process(" + bs.getLoginAtDomain() + "/" + bs.getDevType()
				+ ")");

		StateMachine sm = new StateMachine(backend.getStore());

		NodeList nl = doc.getDocumentElement().getElementsByTagName(
				"Collection");
		LinkedList<SyncCollection> collections = new LinkedList<SyncCollection>();
		HashSet<String> processedClientIds = new HashSet<String>();
		for (int i = 0; i < nl.getLength(); i++) {
			Element col = (Element) nl.item(i);
			collections.add(processCollection(bs, sm, col, processedClientIds));
		}

		Document reply = null;
		try {
			reply = DOMUtils.createDoc(null, "Sync");
			Element root = reply.getDocumentElement();
			Element cols = DOMUtils.createElement(root, "Collections");

			for (SyncCollection c : collections) {
				String oldSyncKey = c.getSyncKey();
				SyncState st = sm.getSyncState(oldSyncKey);
				Element ce = DOMUtils.createElement(cols, "Collection");
				if (c.getDataClass() != null) {
					DOMUtils
							.createElementAndText(ce, "Class", c.getDataClass());
				}
				Element sk = DOMUtils.createElement(ce, "SyncKey");
				DOMUtils.createElementAndText(ce, "CollectionId", c
						.getCollectionId());
				if (!st.isValid()) {
					logger.info("invalid sync key: " + st);
					DOMUtils.createElementAndText(ce, "Status",
							SyncStatus.INVALID_SYNC_KEY.asXmlValue());
				} else {
					DOMUtils.createElementAndText(ce, "Status", "1");
					if (!oldSyncKey.equals("0")) {
						int col = Integer.parseInt(c.getCollectionId());
						String colStr = backend.getStore().getCollectionString(
								col);
						IContentsExporter cex = backend.getContentsExporter(bs);
						cex.configure(bs, c.getDataClass(), c.getFilterType(),
								st, colStr);

						if (c.getFetchIds().size() == 0) {
							doUpdates(bs, c, ce, cex, processedClientIds);
						} else {
							// fetch
							doFetch(bs, c, ce, cex);
						}
					}
					sk.setTextContent(sm.allocateNewSyncKey(bs, c
							.getCollectionId(), st));
				}
			}

			responder.sendResponse("AirSync", reply);
		} catch (Exception e) {
			logger.error("Error creating Sync response", e);
		}
	}

	private void doUpdates(BackendSession bs, SyncCollection c, Element ce,
			IContentsExporter cex, HashSet<String> processedClientIds) {
		List<ItemChange> changed;
		String col = backend.getStore().getCollectionString(
				Integer.parseInt(c.getCollectionId()));
		DataDelta delta = cex.getChanged(bs, col);
		changed = delta.getChanges();
		logger.info("should send " + changed.size() + " change(s).");
		Element responses = DOMUtils.createElement(ce, "Responses");
		Element commands = DOMUtils.createElement(ce, "Commands");

		StringBuilder processedIds = new StringBuilder();
		for (String k : processedClientIds) {
			processedIds.append(' ');
			processedIds.append(k);
		}

		for (ItemChange ic : changed) {

			logger.info("processedIds:" + processedIds.toString()
					+ " ic.clientId: " + ic.getClientId() + " ic.serverId: "
					+ ic.getServerId());

			if (ic.getClientId() != null
					&& processedClientIds.contains(ic.getClientId())) {
				// Acks Add done by pda
				Element add = DOMUtils.createElement(responses, "Add");
				DOMUtils
						.createElementAndText(add, "ClientId", ic.getClientId());
				DOMUtils
						.createElementAndText(add, "ServerId", ic.getServerId());
				DOMUtils.createElementAndText(add, "Status", "1");
			} else if (processedClientIds.contains(ic.getServerId())) {
				// Change asked by device
				Element add = DOMUtils.createElement(responses, "Change");
				DOMUtils
						.createElementAndText(add, "ServerId", ic.getServerId());
				DOMUtils.createElementAndText(add, "Status", "1");
			} else {
				// New change done on server
				Element add = DOMUtils.createElement(commands, "Add");
				DOMUtils
						.createElementAndText(add, "ServerId", ic.getServerId());
				serializeChange(bs, add, c, ic);
			}
		}

		if (responses.getChildNodes().getLength() == 0) {
			responses.getParentNode().removeChild(responses);
		}
		if (commands.getChildNodes().getLength() == 0) {
			commands.getParentNode().removeChild(commands);
		}

		List<ItemChange> del = delta.getDeletions();
		for (ItemChange ic : del) {
			serializeDeletion(ce, ic);
		}
	}

	private void doFetch(BackendSession bs, SyncCollection c, Element ce,
			IContentsExporter cex) {
		List<ItemChange> changed;
		changed = cex.fetch(bs, c.getFetchIds());
		Element commands = DOMUtils.createElement(ce, "Responses");
		for (ItemChange ic : changed) {
			Element add = DOMUtils.createElement(commands, "Fetch");
			DOMUtils.createElementAndText(add, "ServerId", ic.getServerId());
			if (ic.getClientId() != null) {
				DOMUtils
						.createElementAndText(add, "ClientId", ic.getClientId());
			}
			DOMUtils.createElementAndText(add, "Status", "1");
			serializeChange(bs, add, c, ic);
		}
	}

	private void serializeDeletion(Element commands, ItemChange ic) {
		// TODO Auto-generated method stub

	}

	private void serializeChange(BackendSession bs, Element col,
			SyncCollection c, ItemChange ic) {
		IApplicationData data = ic.getData();
		IDataEncoder encoder = encoders.getEncoder(data);
		Element apData = DOMUtils.createElement(col, "ApplicationData");
		encoder.encode(bs, apData, data);
	}

	private SyncCollection processCollection(BackendSession bs,
			StateMachine sm, Element col, HashSet<String> processedClientIds) {
		SyncCollection collection = new SyncCollection();
		collection.setDataClass(DOMUtils.getElementText(col, "Class"));
		collection.setSyncKey(DOMUtils.getElementText(col, "SyncKey"));
		Element fid = DOMUtils.getUniqueElement(col, "CollectionId");
		if (fid != null) {
			collection.setCollectionId(fid.getTextContent());
		}
		// TODO sync supported
		// TODO sync <deletesasmoves/>
		// TODO sync <getchanges/>
		// TODO sync max items
		// TODO sync options

		if (collection.getCollectionId() == null) {
			collection.setCollectionId(Utils.getFolderId(bs.getDevId(),
					collection.getDataClass()));
		}

		collection.setSyncState(sm.getSyncState(collection.getSyncKey()));

		// Element perform = DOMUtils.getUniqueElement(col, "Perform");
		Element perform = DOMUtils.getUniqueElement(col, "Commands");

		if (perform != null) {
			// get our sync state for this collection
			IContentsImporter importer = backend.getContentsImporter(collection
					.getCollectionId(), bs);
			importer.configure(bs, collection.getSyncState(), collection
					.getConflict());
			NodeList mod = perform.getChildNodes();
			for (int j = 0; j < mod.getLength(); j++) {
				Element modification = (Element) mod.item(j);
				processModification(bs, collection, importer, modification,
						processedClientIds);
			}
		}
		return collection;
	}

	/**
	 * Handles modifications requested by mobile device
	 * 
	 * @param bs
	 * @param collection
	 * @param importer
	 * @param modification
	 * @param processedClientIds
	 */
	private void processModification(BackendSession bs,
			SyncCollection collection, IContentsImporter importer,
			Element modification, HashSet<String> processedClientIds) {
		int col = Integer.parseInt(collection.getCollectionId());
		String collectionId = backend.getStore().getCollectionString(col);
		String modType = modification.getNodeName();
		logger.info("modType: " + modType);
		String serverId = DOMUtils.getElementText(modification, "ServerId");
		String clientId = DOMUtils.getElementText(modification, "ClientId");
		if (clientId != null) {
			processedClientIds.add(clientId);
		}
		if (serverId != null) {
			processedClientIds.add(serverId);
		}
		Element syncData = DOMUtils.getUniqueElement(modification,
				"ApplicationData");
		String dataClass = backend.getStore().getDataClass(collectionId);
		IDataDecoder dd = getDecoder(dataClass);
		IApplicationData data = null;
		if (dd != null) {
			if (syncData != null) {
				data = dd.decode(syncData);
			}
			if (modType.equals("Modify")) {
				if (data.isRead()) {
					importer.importMessageReadFlag(bs, serverId, data.isRead());
				} else {
					importer.importMessageChange(bs, collectionId, serverId,
							clientId, data);
				}
			} else if (modType.equals("Add") || modType.equals("Change")) {
				logger.info("processing Add/Change (srv: " + serverId
						+ ", cli:" + clientId + ")");
				importer.importMessageChange(bs, collectionId, serverId,
						clientId, data);
			} else if (modType.equals("Delete")) {
				if (collection.isDeletesAsMoves()) {
					String trash = backend.getWasteBasket();
					if (trash != null) {
						importer.importMessageMove(bs, serverId, trash);
					}
				} else {
					importer.importMessageDeletion(bs, PIMDataType
							.valueOf(dataClass.toUpperCase()), collectionId,
							serverId);
				}
			}
		} else {
			logger.error("no decoder for " + dataClass);
			if (modType.equals("Fetch")) {
				logger.info("adding id to fetch " + serverId);
				collection.getFetchIds().add(serverId);
			}
		}
		collection.setSyncState(importer.getState(bs));
	}

	private IDataDecoder getDecoder(String dataClass) {
		return decoders.get(dataClass);
	}
}
