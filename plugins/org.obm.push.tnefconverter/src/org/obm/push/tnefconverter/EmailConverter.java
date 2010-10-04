package org.obm.push.tnefconverter;

import java.io.InputStream;
import java.nio.charset.Charset;

import net.freeutils.tnef.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.columba.ristretto.composer.MimeTreeRenderer;
import org.columba.ristretto.io.CharSequenceSource;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.message.BasicHeader;
import org.columba.ristretto.message.Header;
import org.columba.ristretto.message.LocalMimePart;
import org.columba.ristretto.message.MimeHeader;
import org.columba.ristretto.message.MimeType;
import org.obm.push.tnefconverter.ScheduleMeeting.PidTagMessageClass;
import org.obm.push.tnefconverter.ScheduleMeeting.ScheduleMeeting;
import org.obm.push.tnefconverter.ScheduleMeeting.ScheduleMeetingEncoder;
import org.obm.push.tnefconverter.ScheduleMeeting.TNEFExtractorUtils;
import org.obm.push.utils.Base64;
import org.obm.push.utils.FileUtils;

/**
 * 
 * @author adrienp
 * 
 */
public class EmailConverter {

	private static Log logger = LogFactory.getLog(EmailConverter.class);

	public InputStream convert(InputStream email) throws TNEFConverterException {
		try {
			MimeStreamParser parser = new MimeStreamParser();
			EmailTnefHandler handler = new EmailTnefHandler();
			parser.setContentHandler(handler);
			parser.parse(email);

			Message message = handler.getTNEFMsg();
			if (message != null) {
				if (logger.isDebugEnabled()) {
					logger.debug(message);
				}
				// FIXME DISABLED TNEF DECODER
				if (TNEFExtractorUtils.isScheduleMeetingRequest(message)) {
					ScheduleMeeting meeting = new ScheduleMeeting(message);
					ScheduleMeetingEncoder encoder = new ScheduleMeetingEncoder(
							meeting, handler.getSubject(), handler.getFrom(),
							handler.getTo(), handler.getCc());
					String ics = encoder.encodeToIcs();
					if (logger.isDebugEnabled()) {
						logger.debug("ICS from tnef: " + ics);
					}

					Header rootHeader = new Header();

					BasicHeader basicHeader = new BasicHeader(rootHeader);
					basicHeader.setTo(handler.getTo().toArray(new Address[0]));
					basicHeader.setSubject(handler.getSubject(), Charset
							.defaultCharset());
					basicHeader.set("Thread-Topic", handler.getSubject());
					basicHeader.setFrom(handler.getFrom());
					if (handler.getCc().size() > 0) {
						basicHeader.setCc(handler.getCc().toArray(
								new Address[0]));
					}

					MimeHeader mh = new MimeHeader(rootHeader);
					mh.set("MIME-Version", "1.0");
					mh.setMimeType(new MimeType("multipart", "mixed"));
					LocalMimePart root = new LocalMimePart(mh);

					MimeHeader textMimeHeader = new MimeHeader(new Header());
					textMimeHeader.set("Content-Type",
							"text/plain; charset=\"UTF-8\"");
					textMimeHeader.setContentTransferEncoding("8bit");
					LocalMimePart textPart = new LocalMimePart(textMimeHeader);
					textPart.setBody(new CharSequenceSource(meeting
							.getDescription()));
					root.addChild(textPart);

					MimeHeader requestMimeHeader = new MimeHeader(new Header());
					String method = "REQUEST";
					if (PidTagMessageClass.ScheduleMeetingCanceled
							.equals(meeting.getMethod())) {
						method = "CANCEL";
					}
					requestMimeHeader.set("Content-Type",
							"text/calendar; charset=\"UTF-8\"; method="
									+ method + "; charset=\"UTF-8\"");
					requestMimeHeader.setContentTransferEncoding("8bit");
					LocalMimePart requestPart = new LocalMimePart(
							requestMimeHeader);
					requestPart.setBody(new CharSequenceSource(ics.replace(
							"\0", "")
							+ "\n"));
					root.addChild(requestPart);

					MimeHeader tnefMimeHeader = new MimeHeader(new Header());
					tnefMimeHeader.set("Content-Type", "application/ms-tnef");
					tnefMimeHeader.set("X-MS-Has-Attach", "");
					tnefMimeHeader.setContentTransferEncoding("base64");
					tnefMimeHeader.setContentDisposition("attachment");
					LocalMimePart tnefPart = new LocalMimePart(tnefMimeHeader);
					byte[] b = FileUtils
							.streamBytes(handler.getTnefDoc(), true);
					tnefPart.setBody(new CharSequenceSource(new String(Base64
							.encode(b))));
					root.addChild(tnefPart);
					InputStream in = MimeTreeRenderer.getInstance()
							.renderMimePart(root);
					return in;
				}
			}
			return null;
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new TNEFConverterException(e);
		}
	}
}
