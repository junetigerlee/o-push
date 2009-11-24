package org.obm.push.data;

public enum AirSyncBaseType {

	PLAIN_TEXT, // 1
	HTML, // 2
	RTF, // 3
	MIME; // 4

	@Override
	public String toString() {
		switch (this) {
		case HTML:
			return "2";
		case RTF:
			return "3";
		case MIME:
			return "4";

		default:
		case PLAIN_TEXT:
			return "1";
		}
	}

}