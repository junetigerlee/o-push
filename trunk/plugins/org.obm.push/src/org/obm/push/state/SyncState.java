package org.obm.push.state;

import java.util.Calendar;
import java.util.Date;

import org.obm.push.backend.PIMDataType;

/**
 * Stores the last sync date for a given sync key & collection
 * 
 * @author tom
 * 
 */
public class SyncState {

	private Date lastSync;
	private Boolean lastSyncFiltred;
	private String key;
	private PIMDataType dataType;

	public SyncState(String path) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 1970);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		lastSync = cal.getTime();
		lastSyncFiltred = false;
		if (path.contains("\\calendar\\")) {
			this.dataType = PIMDataType.CALENDAR;
		} else if (path.endsWith("\\contacts")) {
			this.dataType = PIMDataType.CONTACTS;
		} else if (path.contains("\\tasks")) {
			this.dataType = PIMDataType.TASKS;
		} else if (path.contains("\\email")) {
			this.dataType = PIMDataType.EMAIL;
		}else {
			this.dataType = PIMDataType.FOLDER;
		}
	}

	public Date getLastSync() {
		return lastSync;
	}

	public void setLastSync(Date lastSync) {
		this.lastSync = lastSync;
	}

	/**
	 * @return true if we matched the SyncKey to a sync date
	 */
	public boolean isValid() {
		return lastSync != null;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Boolean isLastSyncFiltred() {
		return lastSyncFiltred;
	}

	public void setLastSyncFiltred(Boolean lastSyncFiltred) {
		this.lastSyncFiltred = lastSyncFiltred;
	}

	public PIMDataType getDataType() {
		return dataType;
	}

	public void setDataType(PIMDataType dataType) {
		this.dataType = dataType;
	}

}
