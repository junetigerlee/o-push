package org.obm.push.backend;

public interface IContinuation {

	Boolean isPending();
	Boolean isResumed();
	
	void suspend(long msTimeout);

	void resume();
	
	void error(String status);

	Boolean isError();

	String getErrorStatus();

	BackendSession getBackendSession();
	void setBackendSession(BackendSession bs);
	
	IListenerRegistration getListenerRegistration();
	void setListenerRegistration(IListenerRegistration reg);

	CollectionChangeListener getCollectionChangeListener();
	void setCollectionChangeListener(CollectionChangeListener l);
	
	int getReqId();
}
