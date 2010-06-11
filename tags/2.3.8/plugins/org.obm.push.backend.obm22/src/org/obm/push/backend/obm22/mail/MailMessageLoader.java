/* ***** BEGIN LICENSE BLOCK *****
 * Version: GPL 2.0
 *
 * The contents of this file are subject to the GNU General Public
 * License Version 2 or later (the "GPL").
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Initial Developer of the Original Code is
 *   MiniG.org project members
 *
 * ***** END LICENSE BLOCK ***** */

package org.obm.push.backend.obm22.mail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.minig.imap.Flag;
import org.minig.imap.FlagsList;
import org.minig.imap.IMAPException;
import org.minig.imap.IMAPHeaders;
import org.minig.imap.StoreClient;
import org.minig.imap.mime.MimePart;
import org.minig.imap.mime.MimeTree;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.MSAttachement;
import org.obm.push.backend.MSEmail;
import org.obm.push.backend.MSEmailBody;
import org.obm.push.backend.MSEmailBodyType;
import org.obm.push.backend.MSEvent;
import org.obm.push.backend.MethodAttachment;
import org.obm.push.backend.obm22.calendar.EventConverter;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.calendar.Event;
import org.obm.sync.client.calendar.CalendarClient;

import fr.aliasource.utils.DOMUtils;
import fr.aliasource.utils.FileUtils;

/**
 * Creates a {@link MailMessage} from a {@link MessageId}.
 * 
 * @author tom, adrienp
 * 
 */
public class MailMessageLoader {

	private static final String[] HEADS_LOAD = new String[] { "Subject",
			"From", "Date", "To", "Cc", "Bcc", "X-Mailer", "User-Agent",
			"Message-ID" };

	private CalendarClient calendarClient;
	private BackendSession bs;
	private Integer collectionId;
	private long messageId;

	private MimeTree tree;
	private InputStream invitation;

	private static final Log logger = LogFactory
			.getLog(MailMessageLoader.class);

	/**
	 * Creates a message loader with the given {@link AttachmentManager} for the
	 * given {@link IFolder}
	 * 
	 * @param atMgr
	 * @param f
	 */
	public MailMessageLoader() {
		this.tree = null;
	}

	public MSEmail fetch(Integer collectionId, long messageId,
			StoreClient store, BackendSession bs, CalendarClient calendarClient)
			throws IOException, IMAPException {
		long[] set = new long[] { messageId };
		this.calendarClient = calendarClient;
		this.bs = bs;
		this.collectionId = collectionId;
		this.messageId = messageId;

		IMAPHeaders[] hs = store.uidFetchHeaders(set,
				MailMessageLoader.HEADS_LOAD);
		if (hs.length != 1 || hs[0] == null) {
			return null;
		}
		MimeTree[] mts = null;
		mts = store.uidFetchBodyStructure(set);
		tree = mts[0];

		MSEmail mm = fetchOneMessage(tree, hs[0], store);

		// do load messages forwarded as attachments into the indexers, as it
		// ignores them
		fetchQuotedText(tree, mm, store);
		fetchForwardMessages(tree, mm, store);

		FlagsList[] fl = store.uidFetchFlags(new long[] { messageId });
		if (fl.length > 0) {
			mm.setRead(fl[0].contains(Flag.SEEN));
			mm.setStarred(fl[0].contains(Flag.FLAGGED));
			mm.setAnswered(fl[0].contains(Flag.ANSWERED));
		}

		fetchMimeData(store, mm);

		return mm;
	}

	private void fetchMimeData(StoreClient store, MSEmail mm) {
		try {
			InputStream mimeData = store.uidFetchMessage(messageId);

			SendEmailHandler handler = new SendEmailHandler("");
			MimeEntityConfig config = new MimeEntityConfig();
			config.setMaxContentLen(Integer.MAX_VALUE);
			config.setMaxLineLen(Integer.MAX_VALUE);
			MimeStreamParser parser = new MimeStreamParser(config);
			parser.setContentHandler(handler);
			parser.parse(mimeData);

			mm.setMimeData(handler.getMessage());

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void fetchQuotedText(MimeTree tree, MSEmail mailMessage,
			StoreClient protocol) throws IOException, IMAPException {
		Iterator<MimePart> it = tree.getChildren().iterator();
		while (it.hasNext()) {
			MimePart m = it.next();
			if (m.getMimeType() != null) {
				fetchFlowed(mailMessage, protocol, m);
			} else {
				Iterator<MimePart> mIt = m.getChildren().iterator();
				while (mIt.hasNext()) {
					MimePart mp = mIt.next();
					if (mp.getMimeType() != null) {
						fetchFlowed(mailMessage, protocol, mp);
					}
				}
			}
		}

	}

	private void fetchFlowed(MSEmail mailMessage, StoreClient protocol,
			MimePart m) throws IOException, IMAPException {
		if ("flowed".equalsIgnoreCase(m.getBodyParams().get("format"))) {
			MSEmail mm = fetchOneMessage(m, null, protocol);
			if (!mailMessage.getBody().equals(mm.getBody())) {
				for (MSEmailBodyType format : mm.getBody().availableFormats()) {
					mailMessage.getBody().addMailPart(format,
							mm.getBody().getValue(format));
				}
			}
		}
	}

	private void fetchForwardMessages(MimePart t, MSEmail mailMessage,
			StoreClient protocol) throws IOException, IMAPException {

		Iterator<MimePart> it = t.getChildren().iterator();
		while (it.hasNext()) {
			MimePart m = it.next();
			if (m.getMimeType() != null) {
				fetchNested(mailMessage, protocol, m);
			} else {
				Iterator<MimePart> mIt = m.getChildren().iterator();
				while (mIt.hasNext()) {
					MimePart mp = mIt.next();
					if (mp.getMimeType() != null) {
						fetchNested(mailMessage, protocol, mp);
					}
				}
			}
		}
	}

	private void fetchNested(MSEmail mailMessage, StoreClient protocol,
			MimePart m) throws IOException, IMAPException {
		if (m.getFullMimeType().equalsIgnoreCase("message/rfc822")) {
			MSEmail mm = fetchOneMessage(m, null, protocol);
			mailMessage.addForwardMessage(mm);
			fetchForwardMessages(m, mm, protocol);
		}
	}

	private MSEmail fetchOneMessage(MimePart mimePart, IMAPHeaders h,
			StoreClient protocol) throws IOException, IMAPException {
		Set<MimePart> chosenParts = null;
		if (mimePart.getMimeType() == null
				|| mimePart.getFullMimeType().equals("message/rfc822")) {
			chosenParts = findBodyTextPart(mimePart, mimePart.getAddress());
		}
		if (h == null) {
			InputStream is = protocol.uidFetchPart(tree.getUid(), mimePart
					.getAddress()
					+ ".HEADER");

			Map<String, String> rawHeaders = new HashMap<String, String>();
			parseRawHeaders(is, rawHeaders, getHeaderCharsetDecoder(mimePart));
			h = new IMAPHeaders();
			h.setRawHeaders(rawHeaders);
		}

		MSEmailBody body = getMailBody(chosenParts, protocol);
		Set<MSAttachement> attach = new HashSet<MSAttachement>();
//		if (chosenParts != null && chosenParts.size() > 0) {
//			for (MimePart mp : chosenParts) {
//				attach.addAll(extractAttachments(mp, protocol,  mp
//						.isInvitation()));
//			}
//		} else {
			attach = extractAttachments(mimePart, protocol);
//		}
		MSEmail mm = new MSEmail();
		mm.setBody(body);
		mm.setFrom(AddressConverter.convertAddress(h.getFrom()));
		mm.setDate(h.getDate());
		mm.setSubject(h.getSubject());
		mm.setHeaders(h.getRawHeaders());
		mm.setCc(AddressConverter.convertAddresses(h.getCc()));
		mm.setTo(AddressConverter.convertAddresses(h.getTo()));
		mm.setBcc(AddressConverter.convertAddresses(h.getBcc()));
		mm.setUid(tree.getUid());
		if (this.calendarClient != null && invitation != null) {
			MSEvent event = getInvitation();
			mm.setInvitation(event);
		}

		mm.setAttachements(attach);
		mm.setSmtpId(h.getRawHeader("Message-ID"));
		return mm;
	}

	private MSEvent getInvitation() throws IOException {
		String ics = FileUtils.streamString(invitation, true);
		if (ics != null && !"".equals(ics)) {
			AccessToken at = calendarClient.login(bs.getLoginAtDomain(), bs
					.getPassword(), "o-push");
			try {
				List<Event> obmEvents = calendarClient.parseICS(at, ics);
				if (obmEvents.size() > 0) {
					Event icsEvent = obmEvents.get(0);

					int ar = bs.getLoginAtDomain().lastIndexOf("@");
					String calendar = bs.getLoginAtDomain().substring(0, ar);

					Event event = calendarClient.getEventFromExtId(at,
							calendar, icsEvent.getExtId());
					if (event == null) {
						String uid = calendarClient.createEvent(at, calendar,
								icsEvent);
						icsEvent.setUid(uid);
						event = icsEvent;
					}

					EventConverter ec = new EventConverter();
					return ec.convertEvent(event);
				}
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			} finally {
				calendarClient.logout(at);
			}
		}
		return null;
	}

	private boolean isSupportedCharset(String charset) {
		if (charset == null || charset.length() == 0) {
			return false;
		}
		try {
			return Charset.isSupported(charset);
		} catch (Throwable t) {
			return false;
		}
	}

	private MSEmailBody getMailBody(Set<MimePart> chosenParts,
			StoreClient protocol) throws IOException, IMAPException {

		MSEmailBody mb = new MSEmailBody();

		if (chosenParts == null || chosenParts.size() == 0) {
			mb.addConverted(MSEmailBodyType.PlainText, "");
			mb.setCharset("utf-8");
		} else {
			for (MimePart mp : chosenParts) {
				InputStream bodyText = protocol.uidFetchPart(tree.getUid(), mp
						.getAddress());
				String charsetName = mp.getBodyParams().get("charset");
				if (!isSupportedCharset(charsetName)) {
					charsetName = "utf-8";
				}
				mb.setCharset(charsetName);
				byte[] rawData = extractPartData(mp, bodyText);
				String partText = new String(rawData, charsetName);

				mb.addConverted(MSEmailBodyType
						.getValueOf(mp.getFullMimeType()), partText);
				if (logger.isDebugEnabled()) {
					logger.debug("Added part " + mp.getFullMimeType() + "\n"
							+ partText + "\n------");
				}
			}
		}
		return mb;
	}

	private boolean inEml(MimePart mp) {
		return mp.getParent() != null
				&& "rfc822".equalsIgnoreCase(mp.getParent().getMimeSubtype());
	}

	private Set<MimePart> findBodyTextPart(MimePart mimePart, String a) {
		Set<MimePart> ret = new HashSet<MimePart>();
		if (a.equals(mimePart.getAddress())) {
			for (MimePart mp : mimePart) {
				if (mp.getMimeType() != null) {
					if (mp.getMimeType().equalsIgnoreCase("text")) {
						if (mp.getMimeSubtype().equalsIgnoreCase("html")
								&& (!inEml(mp))) {
							ret.add(mp);
							break;
						} else if (mp.getMimeSubtype()
								.equalsIgnoreCase("plain")) {
							ret.add(mp);
						}
					}
				} else {
					MimePart mpChild = mp.getChildren().get(0);
					if (mpChild.getMimeType() == null) {
						ret.addAll(findBodyTextPart(mp, mp.getAddress()));
					} else if (!mpChild.getFullMimeType().equalsIgnoreCase(
							"message/rfc822")) {
						ret.addAll(findBodyTextPart(mp, mp.getAddress()));
					}
				}
			}
			return ret;
		}
		return null;
	}

	private byte[] extractPartData(MimePart chosenPart, InputStream bodyText)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		FileUtils.transfer(bodyText, out, true);
		byte[] rawData = out.toByteArray();

		if ("QUOTED-PRINTABLE".equals(chosenPart.getContentTransfertEncoding())) {
			out = new ByteArrayOutputStream();
			InputStream in = new QuotedPrintableDecoderInputStream(
					new ByteArrayInputStream(rawData));
			FileUtils.transfer(in, out, true);
			rawData = out.toByteArray();
		} else if ("BASE64".equals(chosenPart.getContentTransfertEncoding())) {
			rawData = new Base64().decode(rawData);
		}
		return rawData;
	}

	// private Set<MSAttachement> extractAttachments(MimePart mimePart,
	// StoreClient protocol, boolean isInvit) throws IOException,
	// IMAPException {
	// Set<MSAttachement> attach = new HashSet<MSAttachement>();
	// if (mimePart != null) {
	// MimePart parent = mimePart.getParent();
	// if (parent != null) {
	// boolean inv = false;
	// for (MimePart mp : parent.getChildren()) {
	// inv = mp.isInvitation();
	// MSAttachement att = extractAttachmentData(mp, protocol,
	// isInvit || inv);
	// if (att != null) {
	// attach.add(att);
	// }
	// }
	// if (parent.getMimeType() == null
	// && parent.getMimeSubtype() == null) {
	// extractAttachments(parent, protocol, inv);
	// }
	// }
	// }
	// return attach;
	// }

	private Set<MSAttachement> extractAttachments(MimePart mimePart,
			StoreClient protocol) throws IOException,
			IMAPException {
		Set<MSAttachement> attach = new HashSet<MSAttachement>();
		if (mimePart != null) {
			for (MimePart mp : mimePart.getChildren()) {
				MSAttachement msAtt = extractAttachmentData(mp,
						protocol, mp.isInvitation());
				if (msAtt != null) {
					attach.add(msAtt);
				}
			}
		}
		return attach;
	}

	private MSAttachement extractAttachmentData(MimePart mp,
			StoreClient protocol, boolean isInvitation) throws IOException {

		long uid = tree.getUid();
		String id = AttachmentHelper.getAttachmentId(collectionId.toString(),
				"" + messageId, mp.getAddress(), mp.getFullMimeType(), mp
						.getContentTransfertEncoding());
		byte[] data = null;
		InputStream part = protocol.uidFetchPart(uid, mp.getAddress());
		data = extractPartData(mp, part);
		try {
			Map<String, String> bp = mp.getBodyParams();
			if (bp != null) {
				if (bp.containsKey("name") && bp.get("name") != null) {
					if (isInvitation && bp.get("name").contains(".ics")
							 && data != null) {
						invitation = new ByteArrayInputStream(data);
					}
					MSAttachement att = new MSAttachement();
					att.setDisplayName(bp.get("name"));
					att.setFileReference(id);
					att.setMethod(MethodAttachment.NormalAttachment);
					att.setEstimatedDataSize(data.length);
					return att;
				} else if (mp.getContentId() != null
						&& !mp.getContentId().equalsIgnoreCase("nil")) {
					MSAttachement att = new MSAttachement();
					att.setDisplayName(mp.getContentId());
					att.setFileReference(id);
					att.setMethod(MethodAttachment.NormalAttachment);
					att.setEstimatedDataSize(data.length);
					return att;
				} else if (isInvitation) {
					invitation = protocol.uidFetchPart(tree.getUid(), mp
							.getAddress());
				}
			}
		} catch (Exception e) {
			logger.error("Error storing metadata for " + id, e);
		}
		return null;
	}

	// public void setPickupPlain(boolean pickupPlain) {
	// this.pickupPlain = pickupPlain;
	// }

	private void parseRawHeaders(InputStream inputStream,
			Map<String, String> rawHeaders, Charset charset) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				inputStream, charset));
		String line = null;
		StringBuilder curHead = null;
		String lastKey = null;
		while ((line = br.readLine()) != null) {
			// collapse rfc822 headers into one line
			if (!(line.length() > 1)) {
				continue;
			}
			char first = line.charAt(0);
			if (Character.isWhitespace(first)) {
				curHead.append(line.substring(1));
			} else {
				if (lastKey != null) {
					rawHeaders.put(lastKey, DOMUtils
							.stripNonValidXMLCharacters(curHead.toString()));
				}
				curHead = new StringBuilder();
				lastKey = null;

				int split = line.indexOf(':');
				if (split > 0) {
					lastKey = line.substring(0, split).toLowerCase();
					String value = line.substring(split + 1).trim();
					curHead.append(value);
				}
			}
		}
		if (lastKey != null) {
			rawHeaders.put(lastKey, DOMUtils.stripNonValidXMLCharacters(curHead
					.toString()));
		}
	}

	/**
	 * Tries to return a suitable {@link Charset} to decode the headers
	 * 
	 * @param part
	 * @return
	 */
	private Charset getHeaderCharsetDecoder(MimePart part) {
		String encoding = part.getContentTransfertEncoding();
		if (encoding == null) {
			return Charset.forName("utf-8");
		} else if (encoding.equalsIgnoreCase("8bit")) {
			return Charset.forName("iso-8859-1");
		} else {
			try {
				return Charset.forName(encoding);
			} catch (UnsupportedCharsetException uee) {
				if (logger.isDebugEnabled()) {
					logger.debug("illegal charset: " + encoding
							+ ", defaulting to utf-8");
				}
				return Charset.forName("utf-8");
			}
		}
	}
}
