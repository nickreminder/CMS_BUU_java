package com.edi.sort.bean;

import java.util.Comparator;

public class DataLocation {
	private long date;
	private long time;
	
	
	private long startLocation;
	private int numberOfLines;
	
	public long getStartLocation() {
		return startLocation;
	}
	public int getNumberOfLines() {
		return numberOfLines;
	}
	public void setNumberOfLines(int numberOfLines) {
		this.numberOfLines = numberOfLines;
	}
	@Override
	public String toString() {
		return "DataLocation [date=" + date + ", time=" + time
				+ ", startLocation=" + startLocation + ", numberOfLines="
				+ numberOfLines + "]";
	}
	public void setStartLocation(long startLocation) {
		this.startLocation = startLocation;
	}
		
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}	
	
    public static class SortByDateTime implements Comparator<DataLocation> {
        @Override
        public int compare(DataLocation arg0, DataLocation arg1) {
        	DataLocation user0 = (DataLocation) arg0;
        	DataLocation user1 = (DataLocation) arg1;

             int flag = (int) (user0.getDate() - user1.getDate());
             if (flag == 0) {
                 return (int) (user0.getTime()-user1.getTime());
             } else {
                 return flag;
             }
        }
    }
   
}
