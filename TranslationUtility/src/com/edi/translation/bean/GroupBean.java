package com.edi.translation.bean;

import java.util.ArrayList;

public class GroupBean  extends BaseBean {
	private ArrayList<String> segmentListArray = new ArrayList<String>();
	
	public ArrayList<String> getSegmentListArray() {
		return segmentListArray;
	}

	public void setSegmentListArray(String segment) {
		this.segmentListArray.add(segment);
	}
}
