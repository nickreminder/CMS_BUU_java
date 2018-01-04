package com.edi.translation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;

import com.edi.translation.constants.ConfigurationConstants;
import com.edi.translation.constants.IndexConstants;
import com.edi.translation.reader.FlatFileLineReader;



public class FlatfileToX12Translation {
	
	private String LENGTH_PATTERN = "LENGTH=[^,)]*";
	private Pattern lengthPattern = Pattern.compile(LENGTH_PATTERN);
	private boolean hiosIDChanging = false;
	
		
	public static void main(String args[]) {
		//String fileNameWithPath = "C:/Parimanshu/FlatFileTranslation/AllEnrolledMembers-2014-03-26.psv";
		String fileNameWithPath = "C:/Parimanshu/FlatFileTranslation/TestFile.txt";
		//String fileNameWithPath = "C:/Parimanshu/poi/Output/family/initial/Set1/5.1.D20140410.T16414031.out";
		
		long start = Calendar.getInstance().getTimeInMillis();
		FlatFileLineReader reader = new FlatFileLineReader();
		
		FlatfileToX12Translation translator = new FlatfileToX12Translation();
		try {
			reader.setFileToRead(fileNameWithPath);
			
			int count = 0;
			while(reader.isDataAvaliable()) {	
				// Lines per house hold
				ArrayList<String[]> lineArrayList = translator.getPolicyUnitData(reader);		
				
				
				count += lineArrayList.size();
				System.out.println("-------------------------------------------------" + count + "|" + lineArrayList.get(0)[IndexConstants.SUBSCRIBER_INDICATOR_INDEX]);
			}
			System.out.println("Count:" + count);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//reader.search();
				
		System.out.println("Time Taken(s): " + (Calendar.getInstance().getTimeInMillis() - start));		
	}
	
	/*
	 * Gives household data with first being the subscriber.
	 */
	public ArrayList<String[]> getPolicyUnitData(FlatFileLineReader reader) throws IOException {
		
		ArrayList<String[]> transactionArrayList = new ArrayList<String[]>();
		
		String prevMemberID = null;
		String prevHIOSID = null;
		String prevInsuranceLineOfBusiness = null;
		
		while(reader.isDataAvaliable() && true) {
			String[] lineArray = reader.getCurrentLine();
			String memberID = "", memberIndicator = "", HIOSID = "", insuranceLineOfBusiness = "";
			
			if(lineArray.length >= IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX)
				memberID = lineArray[IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1];
			
			if(lineArray.length >= IndexConstants.SUBSCRIBER_INDICATOR_INDEX)
				memberIndicator = lineArray[IndexConstants.SUBSCRIBER_INDICATOR_INDEX-1];	
			
			if(lineArray.length >= IndexConstants.HIOS_ID_INDEX)
				HIOSID = lineArray[IndexConstants.HIOS_ID_INDEX-1];	
			
			if(lineArray.length >= IndexConstants.INSURANCE_LINE_OF_BUSINESS_INDEX)
				insuranceLineOfBusiness = lineArray[IndexConstants.INSURANCE_LINE_OF_BUSINESS_INDEX-1];	
			
			
			if((HIOSID.equals(prevHIOSID) || prevHIOSID == null) && (memberID.equals(prevMemberID) || prevMemberID == null) && (insuranceLineOfBusiness.equals(prevInsuranceLineOfBusiness) || prevInsuranceLineOfBusiness == null)) {
				hiosIDChanging = false;
				if(memberIndicator.equals(ConfigurationConstants.SUBSCRIBER_INDICATOR_Y)) {
					transactionArrayList.add(0, lineArray);
				}
				else
					transactionArrayList.add(lineArray);
				
				reader.linePointerIncrement();
			}
			else {
				if(!HIOSID.equals(prevHIOSID) && prevHIOSID != null) {
					hiosIDChanging = true;
				}
				break;
			}
			
			prevMemberID = memberID;
			prevHIOSID = HIOSID;
			prevInsuranceLineOfBusiness = insuranceLineOfBusiness;
		}
		
		return transactionArrayList;
	}
	
	public boolean isHiosIDChanging() {
		return hiosIDChanging;
	}
}
