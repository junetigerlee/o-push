package org.obm.push.backend.obm22.mail;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MailChanges {
	private Set<Long> removed;
	private Set<Long> updated;
	private Date lastSync;
	
	public MailChanges(){
		this.removed = new HashSet<Long>();
		this.updated = new HashSet<Long>();
	}

	public Set<Long> getRemoved() {
		return removed;
	}

	public void setRemoved(Set<Long> removed) {
		this.removed = removed;
	}

	public Set<Long> getUpdated() {
		return updated;
	}

	public void setUpdated(Set<Long> updated) {
		this.updated = updated;
	}

	public Date getLastSync() {
		return lastSync;
	}

	public void setLastSync(Date lastSync) {
		this.lastSync = lastSync;
	}
	
	public void addUpdated(Long uid){
		this.updated.add(uid);
	}
	
	public void addRemoved(Long uid){
		this.removed.add(uid);
	}

}
