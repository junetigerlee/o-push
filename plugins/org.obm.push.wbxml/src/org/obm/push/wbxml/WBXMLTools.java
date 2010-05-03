package org.obm.push.wbxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.push.utils.DOMUtils;
import org.obm.push.wbxml.parsers.WbxmlEncoder;
import org.obm.push.wbxml.parsers.WbxmlParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Wbxml convertion tools
 * 
 * @author tom
 * 
 */
public class WBXMLTools {

	private static final Log logger = LogFactory.getLog(WBXMLTools.class);

	/**
	 * Transforms a wbxml byte array into the corresponding DOM representation
	 * 
	 * @param wbxml
	 * @return
	 * @throws IOException
	 */
	public static Document toXml(byte[] wbxml) throws IOException {

		// storeWbxml(wbxml);

		WbxmlParser parser = new WbxmlParser();
		parser.setTagTable(0, TagsTables.CP_0); // AirSync
		parser.setTagTable(1, TagsTables.CP_1); // Contacts
		parser.setTagTable(2, TagsTables.CP_2); // Email
		parser.setTagTable(3, TagsTables.CP_3); // AirNotify
		parser.setTagTable(4, TagsTables.CP_4); // Calendar
		parser.setTagTable(5, TagsTables.CP_5); // Move
		parser.setTagTable(6, TagsTables.CP_6); // ItemEstimate
		parser.setTagTable(7, TagsTables.CP_7); // FolderHierarchy
		parser.setTagTable(8, TagsTables.CP_8); // MeetingResponse
		parser.setTagTable(9, TagsTables.CP_9); // Tasks
		parser.setTagTable(10, TagsTables.CP_10); // ResolveRecipients
		parser.setTagTable(11, TagsTables.CP_11); // ValidateCert
		parser.setTagTable(12, TagsTables.CP_12); // Contacts2
		parser.setTagTable(13, TagsTables.CP_13); // Ping
		parser.setTagTable(14, TagsTables.CP_14); // Provision
		parser.setTagTable(15, TagsTables.CP_15); // Search
		parser.setTagTable(16, TagsTables.CP_16); // GAL
		parser.setTagTable(17, TagsTables.CP_17); // AirSyncBase
		parser.setTagTable(18, TagsTables.CP_18); // Settings
		parser.setTagTable(19, TagsTables.CP_19); // DocumentLibrary
		parser.setTagTable(20, TagsTables.CP_20); // ItemOperations
		parser.switchPage(0);
		PushDocumentHandler pdh = new PushDocumentHandler();
		parser.setDocumentHandler(pdh);
		try {
			parser.parse(new ByteArrayInputStream(wbxml));
			return pdh.getDocument();
		} catch (SAXException e) {
			storeWbxml(wbxml);
			logger.error(e.getMessage(), e);
			throw new IOException(e.getMessage());
		}

	}

	private static void storeWbxml(byte[] wbxml) {
		try {
			File tmp = File.createTempFile("debug_", ".wbxml");
			FileOutputStream fout = new FileOutputStream(tmp);
			fout.write(wbxml);
			fout.close();
			logger.error("unparsable wbxml saved in " + tmp.getAbsolutePath());
		} catch (Throwable t) {
			logger.error("error storing debug file", t);
		}
	}

	public static byte[] toWbxml(String defaultNamespace, Document doc)
			throws IOException {
		WbxmlEncoder encoder = new WbxmlEncoder(defaultNamespace);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			DOMUtils.serialise(doc, out);
			InputSource is = new InputSource(new ByteArrayInputStream(out
					.toByteArray()));
			out = new ByteArrayOutputStream();
			encoder.convert(is, out);
			byte[] ret = out.toByteArray();

			// storeWbxml(ret);
			// logger.info("reconverted version");
			// DOMUtils.logDom(toXml(ret));

			return ret;
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

}
