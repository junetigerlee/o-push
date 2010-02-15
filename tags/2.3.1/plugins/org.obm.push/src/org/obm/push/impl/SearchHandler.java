package org.obm.push.impl;

import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContinuation;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Handles the search cmd
 * 
 * @author tom
 * 
 */
public class SearchHandler extends WbxmlRequestHandler {

	public SearchHandler(IBackend backend) {
		super(backend);
	}

	@Override
	public void process(IContinuation continuation, BackendSession bs,
			Document doc, Responder responder) {
		logger.info("process(" + bs.getLoginAtDomain() + "/" + bs.getDevType()
				+ ")");
		
		

		try {
			Document search = DOMUtils.createDoc(null, "Search");
			Element r = search.getDocumentElement();
			DOMUtils.createElementAndText(r, "Status", "1");
			Element resp = DOMUtils.createElement(r, "Response");
			Element store = DOMUtils.createElement(resp, "Store");
			DOMUtils.createElementAndText(store, "Status", "1");

			// TODO results go here
			//DOMUtils.createElementAndText(store, "Range", "0-0");
			
			DOMUtils.createElementAndText(store, "Total", "0");
			
			
			responder.sendResponse("Search", search);

		} catch (Exception e) {
			logger.error("Error creating search response");
		}

	}

}
