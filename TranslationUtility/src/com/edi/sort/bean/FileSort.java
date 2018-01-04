package com.edi.sort.bean;

import java.util.Comparator;

public class FileSort {
	private long xmlCurrentTimeStamp_Date;
	private long xmlCurrentTimeStamp_Time;
	private long fileDate;
	private long fileTime;
	private long controlNumber;
	private String fileRow;

    public FileSort()
    {
    	
    }

	@Override
	public String toString() {
		return fileRow;
	}

	public FileSort(long xmlCurrentTimeStamp_Date,
			long xmlCurrentTimeStamp_Time, long fileDate, long fileTime,
			long controlNumber, String fileRow) {
		super();
		this.xmlCurrentTimeStamp_Date = xmlCurrentTimeStamp_Date;
		this.xmlCurrentTimeStamp_Time = xmlCurrentTimeStamp_Time;
		this.fileDate = fileDate;
		this.fileTime = fileTime;
		this.controlNumber = controlNumber;
		this.fileRow = fileRow;
	}

	public long getXmlCurrentTimeStamp_Date() {
		return xmlCurrentTimeStamp_Date;
	}

	public void setXmlCurrentTimeStamp_Date(long xmlCurrentTimeStamp_Date) {
		this.xmlCurrentTimeStamp_Date = xmlCurrentTimeStamp_Date;
	}

	public long getXmlCurrentTimeStamp_Time() {
		return xmlCurrentTimeStamp_Time;
	}

	public void setXmlCurrentTimeStamp_Time(long xmlCurrentTimeStamp_Time) {
		this.xmlCurrentTimeStamp_Time = xmlCurrentTimeStamp_Time;
	}

	public long getFileDate() {
		return fileDate;
	}

	public void setFileDate(long fileDate) {
		this.fileDate = fileDate;
	}

	public long getFileTime() {
		return fileTime;
	}

	public void setFileTime(long fileTime) {
		this.fileTime = fileTime;
	}

	public long getControlNumber() {
		return controlNumber;
	}

	public void setControlNumber(long controlNumber) {
		this.controlNumber = controlNumber;
	}

	public String getFileRow() {
		return fileRow;
	}

	public void setFileRow(String fileRow) {
		this.fileRow = fileRow;
	}

	public static class SortByDateTime implements Comparator<FileSort> {
		@Override
		public int compare(FileSort arg0, FileSort arg1) {
			FileSort user0 = (FileSort) arg0;
			FileSort user1 = (FileSort) arg1;

			//XML current time stamp field
			int flag = (int) (user0.getXmlCurrentTimeStamp_Date() - user1.getXmlCurrentTimeStamp_Date());
			if (flag != 0) {
				return flag;
			} 
			
			flag = (int) (user0.getXmlCurrentTimeStamp_Time() - user1.getXmlCurrentTimeStamp_Time());
			if (flag != 0) {
				return flag;
			} 
			
			//File name date and time nodes
			flag = (int) (user0.getFileDate() - user1.getFileDate());
			if (flag != 0) {
				return flag;
			}
			
			flag = (int) (user0.getFileTime() - user1.getFileTime());
			if (flag != 0) {
				return flag;
			}
			
			//Control number
			return (int) (user0.getControlNumber() - user1.getControlNumber());
		}
	}
}
