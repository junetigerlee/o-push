package org.obm.push.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IApplicationData;
import org.obm.push.backend.MSTask;
import org.obm.push.backend.Recurrence;
import org.obm.push.backend.SyncCollection;
import org.obm.push.data.calendarenum.RecurrenceDayOfWeek;
import org.obm.push.data.email.Type;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Element;

public class TaskEncoder extends Encoder implements IDataEncoder {
	
	private SimpleDateFormat sdf;
	public TaskEncoder() {
		super();
		//2010-07-08T22:00:00.000Z
		this.sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'");
		this.sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@Override
	public void encode(BackendSession bs, Element p, IApplicationData data,
			SyncCollection c, boolean isResponse) {

		MSTask ta = (MSTask) data;

		s(p, "Tasks:Subject", ta.getSubject());
		if (ta.getImportance() != null) {
			s(p, "Tasks:Importance", ta.getImportance().toString());
		}
		s(p, "Tasks:UTCStartDate", ta.getUtcStartDate(),sdf);
		s(p, "Tasks:StartDate", ta.getStartDate(),sdf);
		s(p, "Tasks:UTCDueDate", ta.getUtcDueDate(),sdf);
		s(p, "Tasks:DueDate", ta.getDueDate(),sdf);
//		DOMUtils.createElement(p, "Tasks:Categories");
		if (ta.getRecurrence() != null) {
			encodeRecurrence(p, ta);
		}
		s(p, "Tasks:Complete", ta.getComplete());
		s(p, "Tasks:DateCompleted", ta.getDateCompleted(),sdf);
		s(p, "Tasks:Sensitivity", ta.getSensitivity().asIntString());
		if(ta.getReminderSet()!= null && ta.getReminderSet()){
			s(p, "Tasks:ReminderTime", ta.getReminderTime(),sdf);
			s(p, "Tasks:ReminderSet", ta.getReminderSet());
		}
		encodeBody(c, bs, p, ta);
	}

	private void encodeRecurrence(Element p, MSTask task) {
		Recurrence rec = task.getRecurrence();
		Element r = DOMUtils.createElement(p, "Tasks:Recurrence");

		s(r, "Tasks:RecurrenceType", rec.getType().asIntString());
		s(r, "Tasks:RecurrenceStart", rec.getStart(),sdf);
		s(r, "Tasks:RecurrenceRegenerate", rec.getRegenerate());
		s(r, "Tasks:RecurrenceDeadOccur", rec.getDeadOccur());
		s(r, "Tasks:RecurrenceInterval", rec.getInterval());
		s(r, "Tasks:RecurrenceUntil", rec.getUntil(),sdf);

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(task.getStartDate().getTime());
		switch (rec.getType()) {
		case DAILY:
			break;
		case MONTHLY:
			DOMUtils.createElementAndText(r, "Tasks:RecurrenceDayOfMonth", ""
					+ cal.get(Calendar.DAY_OF_MONTH));
			break;
		case MONTHLY_NDAY:
			DOMUtils.createElementAndText(r, "Tasks:RecurrenceWeekOfMonth", ""
					+ cal.get(Calendar.WEEK_OF_MONTH));
			DOMUtils.createElementAndText(r, "Tasks:RecurrenceDayOfWeek", ""
					+ RecurrenceDayOfWeek.dayOfWeekToInt(cal
							.get(Calendar.DAY_OF_WEEK)));
			break;
		case WEEKLY:
			DOMUtils.createElementAndText(r, "Tasks:RecurrenceDayOfWeek", ""
					+ RecurrenceDayOfWeek.asInt(rec.getDayOfWeek()));
			break;
		case YEARLY:
			DOMUtils.createElementAndText(r, "Tasks:RecurrenceDayOfMonth", ""
					+ cal.get(Calendar.DAY_OF_MONTH));
			DOMUtils.createElementAndText(r, "Tasks:RecurrenceMonthOfYear", ""
					+ (cal.get(Calendar.MONTH) + 1));
			break;
		case YEARLY_NDAY:
			break;
		}
	}

	private void encodeBody(SyncCollection c, BackendSession bs, Element p,
			MSTask task) {
		String body = "";
		if (task.getDescription() != null) {
			body = task.getDescription().trim();
		}
		if (bs.getProtocolVersion() >= 12) {
			Element d = DOMUtils.createElement(p, "AirSyncBase:Body");
			s(d, "AirSyncBase:Type", Type.PLAIN_TEXT.toString());
			s(d, "AirSyncBase:EstimatedDataSize", "" + body.length());
			if (body.length() > 0) {
				DOMUtils.createElementAndText(d, "AirSyncBase:Data", body);
			}
		} else {
			if (task.getDescription() != null
					&& task.getDescription().length() > 0) {
				// Doesn't work with wm5
				// DOMUtils.createElementAndText(p, "Email:BodyTruncated", "0");
				// DOMUtils.createElementAndText(p, "Email:Body",
				// event.getDescription()+"\r\n");
			}
		}
	}
}
