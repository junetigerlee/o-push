package org.obm.push.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import org.obm.push.data.calendarenum.CalendarBusyStatus;
import org.obm.push.data.calendarenum.CalendarMeetingStatus;
import org.obm.push.data.calendarenum.CalendarSensitivity;

public class Calendar implements IEventCalendar {
	private String organizerName;
	private String organizerEmail;
	private String location;
	private String subject;
	private String uID;
	private SimpleDateFormat dtStamp;
	private SimpleDateFormat endTime;
	private SimpleDateFormat startTime;
	private Boolean allDayEvent;
	private CalendarBusyStatus busyStatus;
	private CalendarSensitivity sensitivity;
	private CalendarMeetingStatus meetingStatus;
	private Integer reminder;
	private ArrayList<Attendee> attendees;
	private ArrayList<String> categories;
	private Recurrence recurrence;
	private ArrayList<Exception> exceptions;
	private TimeZone timeZone;
	
	public TimeZone getTimeZone() {
		return timeZone;
	}
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}
	public String getOrganizerName() {
		return organizerName;
	}
	public void setOrganizerName(String organizerName) {
		this.organizerName = organizerName;
	}
	public String getOrganizerEmail() {
		return organizerEmail;
	}
	public void setOrganizerEmail(String organizerEmail) {
		this.organizerEmail = organizerEmail;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getUID() {
		return uID;
	}
	public void setUID(String uid) {
		uID = uid;
	}
	public SimpleDateFormat getDtStamp() {
		return dtStamp;
	}
	public void setDtStamp(SimpleDateFormat dtStamp) {
		this.dtStamp = dtStamp;
	}
	public SimpleDateFormat getEndTime() {
		return endTime;
	}
	public void setEndTime(SimpleDateFormat endTime) {
		this.endTime = endTime;
	}
	public SimpleDateFormat getStartTime() {
		return startTime;
	}
	public void setStartTime(SimpleDateFormat startTime) {
		this.startTime = startTime;
	}
	public Boolean getAllDayEvent() {
		return allDayEvent;
	}
	public void setAllDayEvent(Boolean allDayEvent) {
		this.allDayEvent = allDayEvent;
	}
	public CalendarBusyStatus getBusyStatus() {
		return busyStatus;
	}
	public void setBusyStatus(CalendarBusyStatus busyStatus) {
		this.busyStatus = busyStatus;
	}
	public CalendarSensitivity getSensitivity() {
		return sensitivity;
	}
	public void setSensitivity(CalendarSensitivity sensitivity) {
		this.sensitivity = sensitivity;
	}
	public CalendarMeetingStatus getMeetingStatus() {
		return meetingStatus;
	}
	public void setMeetingStatus(CalendarMeetingStatus meetingStatus) {
		this.meetingStatus = meetingStatus;
	}
	public Integer getReminder() {
		return reminder;
	}
	public void setReminder(Integer reminder) {
		this.reminder = reminder;
	}
	public ArrayList<Attendee> getAttendees() {
		return attendees;
	}
	public void setAttendees(ArrayList<Attendee> attendees) {
		this.attendees = attendees;
	}
	public ArrayList<String> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}
	public Recurrence getRecurrence() {
		return recurrence;
	}
	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = recurrence;
	}
	public ArrayList<Exception> getExceptions() {
		return exceptions;
	}
	public void setExceptions(ArrayList<Exception> exceptions) {
		this.exceptions = exceptions;
	}
}
