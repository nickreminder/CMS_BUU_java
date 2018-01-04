package com.edi.translation.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FileReportingBean {
	private String fileName;
	private int totalMemberCount = 0;
	private int totalDependentCount = 0;
	private int totalSubscriberCount = 0;
	private int totalHouseHoldCount = 0;
	private int totalIssuerCount = 0;
	
	private int translatedMemberCount = 0;
	private int translatedSubscriberCount = 0;
	private int translatedDependentCount = 0;
	private int translatedHouseHoldCount = 0;
	private int translatedIssuerCount = 0;
	private Map<String, ArrayList<Exception>> exceptionMap = new HashMap<String, ArrayList<Exception>>();
	
	public int getTotalSubscriberCount() {
		return totalSubscriberCount;
	}
	public void setTotalSubscriberCount(int totalSubscriberCount) {
		this.totalSubscriberCount += totalSubscriberCount;
	}
	public int getIssuerCount() {
		return totalIssuerCount;
	}
	public void setIssuerCount(int totalIssuerCount) {
		this.totalIssuerCount += totalIssuerCount;
	}
	public int getTranslatedSubscriberCount() {
		return translatedSubscriberCount;
	}
	public void setTranslatedSubscriberCount(int translatedSubscriberCount) {
		this.translatedSubscriberCount += translatedSubscriberCount;
	}
	
	public int getTranslatedMemberCount() {
		return translatedMemberCount;
	}
	public void setTranslatedMemberCount(int translatedMemberCount) {
		this.translatedMemberCount += translatedMemberCount;
	}
	public int getTranslatedDependentCount() {
		return translatedDependentCount;
	}
	public void setTranslatedDependentCount(int translatedDependentCount) {
		this.translatedDependentCount += translatedDependentCount;
	}
	public int getTranslatedHouseHoldCount() {
		return translatedHouseHoldCount;
	}
	public void setTranslatedHouseHoldCount(int translatedHouseHoldCount) {
		this.translatedHouseHoldCount += translatedHouseHoldCount;
	}
	public int getTranslatedIssuerCount() {
		return translatedIssuerCount;
	}
	public void setTranslatedIssuerCount(int translatedIssuerCount) {
		this.translatedIssuerCount += translatedIssuerCount;
	} 
	
	private ArrayList<IssuerReportingBean> issuerArrayList = new ArrayList<IssuerReportingBean>();
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int getTotalMemberCount() {
		return totalMemberCount;
	}
	public void setTotalMemberCount(int totalMemberCount) {
		this.totalMemberCount += totalMemberCount;
	}
	public int getTotalDependentCount() {
		return totalDependentCount;
	}
	public void setTotalDependentCount(int totalMemberCount) {
		this.totalDependentCount += totalMemberCount;
	}
	public int getTotalIssuerCount() {
		return issuerArrayList.size();
	}	
	public ArrayList<IssuerReportingBean> getIssuerReportList() {
		return issuerArrayList;
	}
	public void setIssuerReport(IssuerReportingBean issuerReportBean) {
		this.issuerArrayList.add(issuerReportBean);
	}
	public int getNumberOfHouseHolds() {
		return totalHouseHoldCount;
	}
	public void setNumberOfHouseHolds(int numberOfHouseHolds) {
		totalHouseHoldCount += numberOfHouseHolds;
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
}
