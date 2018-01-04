package com.edi.translation.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IssuerReportingBean {
	private String fileGenerated;
	private String IssuerHIOSID;
	private String fileLocation;
	private String fileGeneratedType;
	private int memberCount = 0;
	private int subscriberCount = 0;
	private int dependentCount = 0;	
	private int numberOfHouseHolds = 0;
	
	private Map<String, ArrayList<Exception>> exceptionMap = new HashMap<String, ArrayList<Exception>>();
	
	private String policyNumber;
	private String batchID;
	private String recordID;
	
	public String getPolicyNumber() {
		return policyNumber;
	}
	public void setPolicyNumber(String policyNumber) {
		this.policyNumber = policyNumber;
	}
	public String getBatchID() {
		return batchID;
	}
	public void setBatchID(String batchID) {
		this.batchID = batchID;
	}
	public String getRecordID() {
		return recordID;
	}
	public void setRecordID(String recordID) {
		this.recordID = recordID;
	}
	public Map<String, ArrayList<Exception>> getException() {
		return exceptionMap;
	}
	public void setException(String key, Exception exception) {
		ArrayList<Exception> errorArrayList = exceptionMap.get(key);
		if(errorArrayList == null) {
			errorArrayList = new ArrayList<Exception>();
			exceptionMap.put(key, errorArrayList);
		}
		
		errorArrayList.add(exception);
	}	
	public int getSubscriberCount() {
		return subscriberCount;
	}
	public void setSubscriberCount(int subscriberCount) {
		this.subscriberCount += subscriberCount;
	}
	public int getNumberOfHouseHolds() {
		return numberOfHouseHolds;
	}
	public void setNumberOfHouseHolds(int numberOfHouseHolds) {
		this.numberOfHouseHolds += numberOfHouseHolds;
	}
	public String getHIOSID() {
		return IssuerHIOSID;
	}
	public void setHIOSID(String issuerHIOSID) {
		IssuerHIOSID = issuerHIOSID;
	}
	public int getMemberCount() {
		return memberCount;
	}
	public void setMemberCount(int memberCount) {
		this.memberCount += memberCount;
	}
	public int getDependentCount() {
		return dependentCount;
	}
	public void setDependentCount(int dependentCount) {
		this.dependentCount += dependentCount;
	}
	public String getFileGeneratedLocation() {
		return fileLocation;
	}
	public void setFileGeneratedLocation(String filelocation) {
		this.fileLocation = filelocation;
	}
	
	public String getFileGenerated() {
		return fileGenerated;
	}
	public void setFileGenerated(String fileGenerated) {
		this.fileGenerated = fileGenerated;
	}
	
	public String getFileGeneratedType() {
		return fileGeneratedType;
	}
	public void setFileGeneratedType(String fileGeneratedType) {
		this.fileGeneratedType = fileGeneratedType;
	}
}
