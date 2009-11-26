package org.obm.push.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.MSAddress;
import org.obm.push.backend.MSAttachement;
import org.obm.push.backend.MSEmailBodyType;
import org.obm.push.backend.MSEvent;
import org.obm.push.backend.MSEmail;
import org.obm.push.backend.Recurrence;
import org.obm.push.backend.SyncCollection;
import org.obm.push.data.calendarenum.RecurrenceDayOfWeek;
import org.obm.push.utils.Base64;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Element;

/**
 * 
 * @author adrienp
 * 
 */
public class EmailEncoder implements IDataEncoder {

	protected Log logger = LogFactory.getLog(getClass());
	private SimpleDateFormat sdf;

	public EmailEncoder() {
		sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	// <To>"Administrator" &lt;Administrator@buffy.kvm&gt;</To>
	// <From>"Administrator" &lt;Administrator@buffy.kvm&gt;</From>
	// <Subject>mail to me</Subject>
	// <DateReceived>2009-03-17T17:59:00.000Z</DateReceived>
	// <DisplayTo>Administrator</DisplayTo>
	// <ThreadTopic>mail to me</ThreadTopic>
	// <Importance>1</Importance>
	// <Read>1</Read>
	// <BodyTruncated>0</BodyTruncated>
	// <Body>to me from otlk&#13;</Body>
	// <MessageClass>IPM.Note</MessageClass>
	// <InternetCPID>28591</InternetCPID>

	@Override
	public void encode(BackendSession bs, Element parent,
			IApplicationData data, SyncCollection c, boolean isResponse) {
		MSEmail mail = (MSEmail) data;

		DOMUtils.createElementAndText(parent, "Email:To",
				buildStringAdresses(mail.getTo()));
		if (mail.getCc().size() > 0) {
			DOMUtils.createElementAndText(parent, "Email:CC",
					buildStringAdresses(mail.getCc()));
		}
		if (mail.getFrom() != null) {
			DOMUtils.createElementAndText(parent, "Email:From", mail.getFrom()
					.getMail());
		} else {
			DOMUtils.createElementAndText(parent, "Email:From", "");
		}
		DOMUtils.createElementAndText(parent, "Email:Subject", mail
				.getSubject());
		DOMUtils.createElementAndText(parent, "Email:DateReceived", sdf
				.format(mail.getDate()));
		if (mail.getTo().size() > 0 && mail.getTo().get(0) != null
				&& mail.getTo().get(0).getDisplayName() != null
				&& !"".equals(mail.getTo().get(0).getDisplayName())) {
			DOMUtils.createElementAndText(parent, "Email:DisplayTo", mail
					.getTo().get(0).getDisplayName());
		}
//		DOMUtils.createElementAndText(parent, "Email:ThreadTopic", mail
//				.getSubject());
		DOMUtils.createElementAndText(parent, "Email:Importance", mail
				.getImportance().asIntString());
		DOMUtils.createElementAndText(parent, "Email:Read", mail.isRead() ? "1"
				: "0");

		if (bs.getProtocolVersion() == 2.5) {
			appendBody25(parent, mail, c);
		} else {
			appendBody(parent, mail, c);
		}

		if (bs.getProtocolVersion() == 2.5) {
			appendAttachments25(parent, mail);
		} else {
			appendAttachments(parent, mail);
		}
//		

		DOMUtils.createElementAndText(parent, "Email:MessageClass", mail
				.getMessageClass().toString());

		appendMeetintRequest(parent, mail);

		DOMUtils.createElementAndText(parent, "Email:InternetCPID",
				InternetCPIDMapping
						.getInternetCPID(mail.getBody().getCharset()));

	}

	private void appendBody25(Element parent, MSEmail mail, SyncCollection c) {
		if (c.getTruncation() != null && c.getTruncation().equals(1)) {
			DOMUtils.createElementAndText(parent, "Email:BodyTruncated", "1");
		} else {
			DOMUtils.createElementAndText(parent, "Email:BodyTruncated", "0");
			String mailBody = mail.getBody().getValue(MSEmailBodyType.PlainText);
			DOMUtils.createElementAndText(parent, "Email:Body", mailBody);
		}
	}

	protected void appendBody(Element parent, MSEmail mail, SyncCollection c) {
		Element elemBody = DOMUtils.createElement(parent, "AirSyncBase:Body");
		String data = "";
		MSEmailBodyType availableFormat = mail.getBody().availableFormats()
				.iterator().next();
		if (c.getMimeSupport() != null && c.getMimeSupport().equals(2)) {
			data = mail.getMimeData();
			availableFormat = MSEmailBodyType.MIME;
		} else {
			if (c.getBodyPreference() != null
					&& c.getBodyPreference().getType() != null) {
				availableFormat = c.getBodyPreference().getType();
			}

			data = mail.getBody().getValue(availableFormat);
			if (data == null) {
				availableFormat = mail.getBody().availableFormats().iterator()
						.next();
				data = mail.getBody().getValue(availableFormat);
			}
		}

		DOMUtils.createElementAndText(elemBody, "AirSyncBase:Type",
				availableFormat.asIntString());
		DOMUtils.createElementAndText(elemBody,
				"AirSyncBase:EstimatedDataSize", "" + data.getBytes().length);

		if (c.getBodyPreference() != null
				&& c.getBodyPreference().getTruncationSize() != null) {
			if (c.getBodyPreference().getTruncationSize() < data.length()) {
				data = data.substring(0, c.getBodyPreference()
						.getTruncationSize());
				DOMUtils.createElementAndText(elemBody,
						"AirSyncBase:Truncated", "1");
			}
		}

		DOMUtils.createElementAndText(elemBody, "AirSyncBase:Data", data);

		if (mail.getInvitation() != null) {
			DOMUtils.createElementAndText(parent, "Email:ContentClass",
					"urn:content-classes:calendarmessage");
		} else {
			DOMUtils.createElementAndText(parent, "Email:ContentClass",
					"urn:content-classes:message");
		}

		DOMUtils.createElementAndText(parent, "AirSyncBase:NativeBodyType",
				mail.getBody().availableFormats().iterator().next()
						.asIntString());
	}

	private void appendMeetintRequest(Element parent, MSEmail mail) {
		if (mail.getInvitation() != null) {
			Element mr = DOMUtils.createElement(parent, "Email:MeetingRequest");

			MSEvent invi = mail.getInvitation();
			DOMUtils.createElementAndText(mr, "Email:AllDayEvent", invi
					.getAllDayEvent() ? "1" : "0");
			DOMUtils.createElementAndText(mr, "Email:StartTime",
					formatDate(invi.getStartTime()));
			DOMUtils.createElementAndText(mr, "Email:DTStamp", formatDate(invi
					.getDtStamp()));
			DOMUtils.createElementAndText(mr, "Email:EndTime", formatDate(invi
					.getEndTime()));

			DOMUtils.createElementAndText(mr, "Email:InstanceType", "0");

			if (invi.getLocation() != null && !"".equals(invi.getLocation())) {
				DOMUtils.createElementAndText(mr, "Email:Location", invi
						.getLocation());
			}
			if (invi.getOrganizerEmail() != null
					&& !"".equals(invi.getOrganizerEmail())) {
				DOMUtils.createElementAndText(mr, "Email:Organizer", invi
						.getOrganizerEmail());
			} else if (invi.getOrganizerName() != null
					&& !"".equals(invi.getOrganizerName())) {
				DOMUtils.createElementAndText(mr, "Email:Organizer", invi
						.getOrganizerName());
			}
			if (invi.getReminder() != null) {
				DOMUtils.createElementAndText(mr, "Email:Reminder", invi
						.getReminder().toString());
			}

			DOMUtils.createElementAndText(mr, "Email:ResponseRequested", "1");

			if (invi.getSensitivity() != null) {
				DOMUtils.createElementAndText(mr, "Email:Sensitivity", invi
						.getSensitivity().asIntString());
			}

			if (invi.getBusyStatus() != null) {
				DOMUtils.createElementAndText(mr, "Email:IntDBusyStatus", invi
						.getBusyStatus().asIntString());
			}

			Element tz = DOMUtils.createElement(mr, "Email:TimeZone");
			// taken from exchange 2k7 : eastern greenland, gmt+0, no dst
			tz
					.setTextContent("xP///1IAbwBtAGEAbgBjAGUAIABTAHQAYQBuAGQAYQByAGQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAFIAbwBtAGEAbgBjAGUAIABEAGEAeQBsAGkAZwBoAHQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
			DOMUtils.createElementAndText(mr, "Email:GlobalObjId", new String(
					Base64.encode(invi.getUID().getBytes())));

			appendRecurence(mr, invi);
		}
	}

	private void appendRecurence(Element parent, MSEvent invi) {
		Recurrence recur = invi.getRecurrence();
		if (recur != null) {
			Element ers = DOMUtils.createElement(parent, "Email:Recurrences");
			Element r = DOMUtils.createElement(ers, "Email:Recurrence");
			if (recur.getInterval() != null) {
				DOMUtils.createElementAndText(r, "Email:Recurrence_Interval",
						recur.getInterval().toString());
			}
			if (recur.getUntil() != null) {
				DOMUtils.createElementAndText(r, "Email:Recurrence_Until",
						formatDate(recur.getUntil()));
			}
			if (recur.getOccurrences() != null) {
				DOMUtils.createElementAndText(r,
						"Email:Recurrence_Occurrences", recur.getOccurrences()
								.toString());
			}

			DOMUtils.createElementAndText(r, "Email:Recurrence_Type", recur
					.getType().asIntString());
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.setTimeInMillis(invi.getStartTime().getTime());
			switch (recur.getType()) {
			case DAILY:
				break;
			case MONTHLY:
				DOMUtils.createElementAndText(r, "Email:Recurrence_DayOfMonth",
						"" + cal.get(Calendar.DAY_OF_MONTH));
				break;
			case MONTHLY_NDAY:
				DOMUtils.createElementAndText(r,
						"Email:Recurrence_WeekOfMonth", ""
								+ cal.get(Calendar.WEEK_OF_MONTH));
				DOMUtils.createElementAndText(r, "Email:Recurrence_DayOfWeek",
						""
								+ RecurrenceDayOfWeek.dayOfWeekToInt(cal
										.get(Calendar.DAY_OF_WEEK)));
				break;
			case WEEKLY:
				DOMUtils.createElementAndText(r, "Email:Recurrence_DayOfWeek",
						"" + RecurrenceDayOfWeek.asInt(recur.getDayOfWeek()));
				break;
			case YEARLY:
				DOMUtils.createElementAndText(r, "Email:Recurrence_DayOfMonth",
						"" + cal.get(Calendar.DAY_OF_MONTH));
				DOMUtils.createElementAndText(r,
						"Email:Recurrence_MonthOfYear", ""
								+ (cal.get(Calendar.MONTH) + 1));
				break;
			case YEARLY_NDAY:
				break;

			}
		}
	}

	private void appendAttachments(Element parent, MSEmail email) {
		if (email.getAttachements().size() > 0) {
			Element atts = DOMUtils.createElement(parent,
					"AirSyncBase:Attachments");

			Set<MSAttachement> mailAtts = email.getAttachements();
			for (MSAttachement msAtt : mailAtts) {
				Element att = DOMUtils.createElement(atts,
						"AirSyncBase:Attachment");
				DOMUtils.createElementAndText(att, "AirSyncBase:DisplayName",
						msAtt.getDisplayName());
				DOMUtils.createElementAndText(att, "AirSyncBase:FileReference",
						msAtt.getFileReference());
				DOMUtils.createElementAndText(att, "AirSyncBase:Method", msAtt
						.getMethod().asIntString());
				DOMUtils.createElementAndText(att,
						"AirSyncBase:EstimatedDataSize", msAtt
								.getEstimatedDataSize().toString());
			}
		}
	}
	
	private void appendAttachments25(Element parent, MSEmail email) {
		if (email.getAttachements().size() > 0) {
			Element atts = DOMUtils.createElement(parent,
					"Email:Attachments");

			Set<MSAttachement> mailAtts = email.getAttachements();
			for (MSAttachement msAtt : mailAtts) {
				Element att = DOMUtils.createElement(atts,
						"Email:Attachment");
				DOMUtils.createElementAndText(att, "Email:DisplayName",
						msAtt.getDisplayName());
				DOMUtils.createElementAndText(att, "Email:AttName",
						msAtt.getFileReference());
				DOMUtils.createElementAndText(att, "Email:AttMethod", msAtt
						.getMethod().asIntString());
				DOMUtils.createElementAndText(att,
						"Email:AttSize", msAtt
								.getEstimatedDataSize().toString());
			}
		}
	}

	private String formatDate(Date d) {
		return sdf.format(d);
	}

	private String buildStringAdresses(List<MSAddress> addresses) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<MSAddress> it = addresses.iterator(); it.hasNext();) {
			MSAddress addr = it.next();
			sb.append(addr.getMail());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();

	}

}
