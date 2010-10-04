package org.obm.push.tnefconverter.ScheduleMeeting;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.freeutils.tnef.TNEFUtils;

/**
 * Decode a global object id
 * 
 * @author adrienp
 * 
 */
public class GlobalObjectId {

	private String uid;
	private Date recurrenceId;

	/**
	 * http://msdn.microsoft.com/en-us/library/ee160198(EXCHG.80).aspx Spec
	 * MS-OXOCAL
	 * 
	 * @param data
	 * @throws IOException
	 */
	public GlobalObjectId(InputStream obj, int recurStartTime)
			throws IOException {
		// int[] data = FileUtils.streamBytes(obj, false);
		// Byte Array ID (16 bytes): An array of 16 bytes identifying this BLOB
		// as a Global Object ID.
		// The byte array MUST be as follows:
		// int pos = 0;
		int[] goiID = { 0x04, 0x00, 0x00, 0x00, 0x82, 0x00, 0xE0, 0x00, 0x74,
				0xC5, 0xB7, 0x10, 0x1A, 0x82, 0xE0, 0x08 };
		for (int i = 0; i < goiID.length; i++) {
			if (goiID[i] != obj.read()) {
				throw new IllegalArgumentException();
			}
		}

		int[] yearBuf = new int[2];
		// YH (1 byte): The high-ordered byte of the 2-byte Year from the
		// PidLidExceptionReplaceTime
		// property if the object represents an exception; otherwise, zero.
		// YL (1 byte): The low-ordered byte of the 2-byte Year from the
		// PidLidExceptionReplaceTime
		// property if the object represents an exception; otherwise, zero.
		// System.err.println(obj.read(););
		yearBuf[0] = obj.read();
		yearBuf[1] = obj.read();
		short year = readShort(yearBuf);
		// M (1 byte): The Month from the PidLidExceptionReplaceTime property if
		// the object represents
		// an exception; otherwise, zero. If it represents an exception, the
		// value MUST be one of those
		// listed in the following table.
		int m = obj.read();
		int month = 0;
		switch (m) {
		case 1:
			month = Calendar.JANUARY;
			break;
		case 2:
			month = Calendar.FEBRUARY;
			break;
		case 3:
			month = Calendar.MARCH;
			break;
		case 4:
			month = Calendar.APRIL;
			break;
		case 5:
			month = Calendar.MAY;
			break;
		case 6:
			month = Calendar.JUNE;
			break;
		case 7:
			month = Calendar.JULY;
			break;
		case 8:
			month = Calendar.AUGUST;
			break;
		case 9:
			month = Calendar.SEPTEMBER;
			break;
		case 10:
			month = Calendar.OCTOBER;
			break;
		case 11:
			month = Calendar.NOVEMBER;
			break;
		case 12:
			month = Calendar.DECEMBER;
			break;
		}

		// D (1 byte): The Day of the month from the PidLidExceptionReplaceTime
		// property if the object
		// represents an exception; otherwise, zero.
		int dayOfMonth = obj.read();
		if (year != 0 && month != 0 && dayOfMonth != 0) {
			int h = (recurStartTime >> 12);
			int min = (recurStartTime - h * 4096) >> 6;
			Calendar cal = new GregorianCalendar(year, month, dayOfMonth, h - 2,
					min, 0);
			recurrenceId = cal.getTime();
		}
		// Creation Time (8 bytes): The date and time that this Global Object ID
		// was generated.
		// Creation Time is a FILETIME structure, as specified in [MS-DTYP].
		obj.skip(8);
		// X (8 bytes): Reserved, MUST be all zeroes.
		obj.skip(8);

		// Size (4 bytes): A LONG value that defines the size of the Data
		// component.
		int[] tsize = new int[4];
		tsize[3] = obj.read();
		tsize[2] = obj.read();
		tsize[1] = obj.read();
		tsize[0] = obj.read();
		int size = readInt(tsize);

		byte[] b = new byte[8];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) obj.read();
		}
		size = size - 8;
		String uidLabel = new String(b);
		if (!"vCal-Uid".equals(uidLabel)) {
			// TODO USE buf
			StringBuilder uidBuilder = new StringBuilder(
					"040000008200E00074C5B7101A82E00800000000");
			obj.reset();
			obj.skip(20);
			byte[] endUid = new byte[obj.available()];
			for (int i = 0; i < endUid.length; i++) {
				endUid[i] = (byte) obj.read();
			}
			uidBuilder.append(TNEFUtils.toHexString(endUid));
			this.uid = uidBuilder.toString();
		} else {
			// // skip 0x01,0x00,0x00,0x00
			obj.skip(4);
			size = size - 4;
			// skip 0x00 at the end
			size = size - 1;
			byte[] uid = new byte[size];
			for (int l = 0; l < size; l++) {
				uid[l] = (byte) obj.read();
				;
			}
			System.err.println(new String(uid));
			this.uid = TNEFUtils.toHexString(uid);
		}
	}

	public String getUid() {
		return uid;
	}

	public Date getRecurrenceId() {
		return recurrenceId;
	}

	private final short readShort(int[] t) throws IOException {
		int ch1 = t[0];
		int ch2 = t[1];
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (short) ((ch1 << 8) + (ch2 << 0));
	}

	private final static int readInt(int[] t) throws IOException {

		if ((t[0] | t[1] | t[2] | t[3]) < 0)
			throw new EOFException();
		return ((t[0] << 24) + (t[1] << 16) + (t[2] << 8) + (t[3] << 0));
	}
}
