package org.obm.push.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.MSAttendee;
import org.obm.push.backend.MSEvent;
import org.obm.push.backend.Recurrence;
import org.obm.push.backend.SyncCollection;
import org.obm.push.data.calendarenum.CalendarMeetingStatus;
import org.obm.push.data.calendarenum.RecurrenceDayOfWeek;
import org.obm.push.data.email.Type;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Element;

public class CalendarEncoder extends Encoder implements IDataEncoder {

	private static final Pattern hexa = Pattern.compile("[0-9a-fA-F]");
	private SimpleDateFormat sdf;
	
	
	public CalendarEncoder() {
		super();
		this.sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		this.sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	// <TimeZone>xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==</TimeZone>
	// <AllDayEvent>0</AllDayEvent>
	// <BusyStatus>2</BusyStatus>
	// <DTStamp>20010101T000000Z</DTStamp>
	// <EndTime>20010101T000000Z</EndTime>
	// <Sensitivity>0</Sensitivity>
	// <StartTime>20010101T000000Z</StartTime>
	// <UID>74455CE0E49D486DBDBC7CB224C5212D00000000000000000000000000000000</UID>
	// <MeetingStatus>0</MeetingStatus>
	@Override
	public void encode(BackendSession bs, Element p, IApplicationData data,
			SyncCollection c, boolean isReponse) {

		MSEvent ev = (MSEvent) data;

		Element tz = DOMUtils.createElement(p, "Calendar:TimeZone");
		// taken from exchange 2k7 : eastern greenland, gmt+0, no dst
		tz
				.setTextContent("xP///1IAbwBtAGEAbgBjAGUAIABTAHQAYQBuAGQAYQByAGQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAFIAbwBtAGEAbgBjAGUAIABEAGEAeQBsAGkAZwBoAHQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");

		s(p, "Calendar:DTStamp", ev.getDtStamp() != null ? ev.getDtStamp()
				: new Date(),sdf);

		s(p, "Calendar:StartTime", ev.getStartTime(),sdf);
		s(p, "Calendar:Subject", ev.getSubject());

		if (hexa.matcher(ev.getUID()).matches()) {
			s(p, "Calendar:UID", ev.getUID());
		} else if (ev.getUID().hashCode() != 0) {
			s(p, "Calendar:UID", Integer.toHexString(ev.getUID().hashCode()));
		}
		if (ev.getOrganizerEmail() != null) {
			s(p, "Calendar:OrganizerName", ev.getOrganizerName());
			s(p, "Calendar:OrganizerEmail", ev.getOrganizerEmail());
		} else {
			// FIXME we need a proper name & email
			String l = bs.getLoginAtDomain();
			int idx = l.indexOf("@");
			if (idx > 0) {
				l = l.substring(0, idx);
			}
			s(p, "Calendar:OrganizerName", l);
			s(p, "Calendar:OrganizerEmail", bs.getLoginAtDomain());
		}

		if (bs.checkHint("hint.loadAttendees", true)) {
			Element at = DOMUtils.createElement(p, "Calendar:Attendees");
			if (ev.getAttendees().size() > 1) {
				for (MSAttendee ma : ev.getAttendees()) {
					Element ae = DOMUtils
							.createElement(at, "Calendar:Attendee");
					s(ae, "Calendar:AttendeeEmail", ma.getEmail());
					

					if (ma.getName() == null
							|| ma.getName().trim().length() == 0) {
						String mail = ma.getEmail();
						int idx = mail.indexOf('@');
						if (idx > 0) {
							mail = mail.substring(0, mail.indexOf('@'))
									.replace(".", " ");
						}
						ma.setName(mail);
					}
					
					s(ae, "Calendar:AttendeeName", ma.getName());

					if (bs.getProtocolVersion() >= 12) {
						s(ae, "Calendar:AttendeeStatus", ma.getAttendeeStatus()
								.asIntString());
						s(ae, "Calendar:AttendeeType", ma.getAttendeeType()
								.asIntString());
					}
				}
			}
		}

		s(p, "Calendar:Location", ev.getLocation());
		s(p, "Calendar:EndTime", ev.getEndTime(),sdf);

		encodeBody(c, bs, p, ev);

		if (ev.getRecurrence() != null) {
			encodeRecurrence(p, ev);
			encodeExceptions(c, bs, p, ev.getExceptions());
		}

		s(p, "Calendar:Sensitivity", "0");
		s(p, "Calendar:BusyStatus", ev.getBusyStatus().asIntString());

		if (ev.getAllDayEvent()) {
			s(p, "Calendar:AllDayEvent", (ev.getAllDayEvent() ? "1" : "0"));
		} else {
			s(p, "Calendar:AllDayEvent", "0");
		}

		if (bs.checkHint("hint.loadAttendees", true)
				&& ev.getAttendees().size() > 1) {
			s(p, "Calendar:MeetingStatus", CalendarMeetingStatus.IS_IN_MEETING
					.asIntString());
		} else {
			s(p, "Calendar:MeetingStatus",
					CalendarMeetingStatus.IS_NOT_IN_MEETING.asIntString());
		}

		if (isReponse && bs.getProtocolVersion() > 12) {
			s(p, "AirSyncBase:NativeBodyType", Type.PLAIN_TEXT.toString());
		}

		if (ev.getReminder() != null) {
			s(p, "Calendar:ReminderMinsBefore", ev.getReminder().toString());
		}

		// DOMUtils.createElement(p, "Calendar:Compressed_RTF");

	}

	private void encodeExceptions(SyncCollection c, BackendSession bs,
			Element p, List<MSEvent> excepts) {
		// Exceptions.Exception
		Element es = DOMUtils.createElement(p, "Calendar:Exceptions");
		for (MSEvent ex : excepts) {
			Element e = DOMUtils.createElement(es, "Calendar:Exception");
			if (ex.isDeletedException()) {

				s(e, "Calendar:ExceptionIsDeleted", "1");
				s(e, "Calendar:MeetingStatus",
						CalendarMeetingStatus.MEETING_IS_CANCELED.asIntString());

			} else {
				if (bs.checkHint("hint.loadAttendees", true)
						&& ex.getAttendees().size() > 1) {
					s(e, "Calendar:MeetingStatus",
							CalendarMeetingStatus.IS_IN_MEETING.asIntString());
				} else {
					s(e, "Calendar:MeetingStatus",
							CalendarMeetingStatus.IS_NOT_IN_MEETING
									.asIntString());
				}

				encodeBody(c, bs, e, ex);

				s(e, "Calendar:Location", ex.getLocation());
				s(e, "Calendar:Sensitivity", "0");
				s(e, "Calendar:BusyStatus", ex.getBusyStatus().asIntString());
				s(e, "Calendar:AllDayEvent", (ex.getAllDayEvent() ? "1" : "0"));
				s(e, "Calendar:ReminderMinsBefore", ex.getReminder());
				DOMUtils.createElement(e, "Calendar:Categories");
			}
			s(e, "Calendar:Subject", ex.getSubject());

			s(e, "Calendar:ExceptionStartTime", ex.getExceptionStartTime(),sdf);

			s(e, "Calendar:StartTime", ex.getStartTime(),sdf);
			s(e, "Calendar:EndTime", ex.getEndTime(),sdf);
			s(e, "Calendar:DTStamp", ex.getDtStamp(),sdf);
		}
	}

	private void encodeBody(SyncCollection c, BackendSession bs, Element p,
			MSEvent event) {
		String body = "";
		if (event.getDescription() != null) {
			body = event.getDescription().trim();
		}
		if (bs.getProtocolVersion() >= 12) {
			Element d = DOMUtils.createElement(p, "AirSyncBase:Body");
			s(d, "AirSyncBase:Type", Type.PLAIN_TEXT.toString());
			s(d, "AirSyncBase:EstimatedDataSize", "" + body.length());
			if (body.length() > 0) {
				DOMUtils.createElementAndText(d, "AirSyncBase:Data", body);
			}
		} else {
			if (event.getDescription() != null
					&& event.getDescription().length() > 0) {
				// Doesn't work with wm5
				// DOMUtils.createElementAndText(p, "Email:BodyTruncated", "0");
				// DOMUtils.createElementAndText(p, "Email:Body",
				// event.getDescription()+"\r\n");
			}
		}
	}

	private void encodeRecurrence(Element p, MSEvent ev) {
		Element r = DOMUtils.createElement(p, "Calendar:Recurrence");
		DOMUtils.createElementAndText(r, "Calendar:RecurrenceType", rec(ev)
				.getType().asIntString());
		s(r, "Calendar:RecurrenceInterval", rec(ev).getInterval());
		s(r, "Calendar:RecurrenceUntil", rec(ev).getUntil(),sdf);

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(ev.getStartTime().getTime());
		switch (rec(ev).getType()) {
		case DAILY:
			break;
		case MONTHLY:
			s(r, "Calendar:RecurrenceDayOfMonth",
					"" + cal.get(Calendar.DAY_OF_MONTH));
			break;
		case MONTHLY_NDAY:
			s(r, "Calendar:RecurrenceWeekOfMonth",
					"" + cal.get(Calendar.WEEK_OF_MONTH));
			s(r, "Calendar:RecurrenceDayOfWeek", ""
					+ RecurrenceDayOfWeek.dayOfWeekToInt(cal
							.get(Calendar.DAY_OF_WEEK)));
			break;
		case WEEKLY:
			s(r, "Calendar:RecurrenceDayOfWeek", ""
					+ RecurrenceDayOfWeek.asInt(rec(ev).getDayOfWeek()));
			break;
		case YEARLY:
			s(r, "Calendar:RecurrenceDayOfMonth",
					"" + cal.get(Calendar.DAY_OF_MONTH));
			s(r, "Calendar:RecurrenceMonthOfYear",
					"" + (cal.get(Calendar.MONTH) + 1));
			break;
		case YEARLY_NDAY:
			break;
		}
	}

	private Recurrence rec(MSEvent ev) {
		return ev.getRecurrence();
	}
}
