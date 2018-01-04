package com.edi.translation.bean;

import java.util.ArrayList;

public class TransactionBean extends BaseBean {
	private ArrayList<String> segmentListArray = new ArrayList<String>();
	private ArrayList<String> repeatSegmentListArray = new ArrayList<String>();

	public ArrayList<String> getSegmentListArray() {
		return segmentListArray;
	}

	public void setSegmentListArray(String segment) {
		this.segmentListArray.add(segment);
	}
	
	

	public ArrayList<String> getMemberListArray() {
		return repeatSegmentListArray;
	}

	public void setMemberListArray(String segment) {
		this.repeatSegmentListArray.add(segment);
	}
}
