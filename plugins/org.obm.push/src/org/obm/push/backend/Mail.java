package org.obm.push.backend;


public class Mail implements IApplicationData {

	@Override
	public PIMDataType getType() {
		return PIMDataType.EMAIL;
	}

	@Override
	public boolean isRead() {
		// TODO Auto-generated method stub
		return false;
	}

}