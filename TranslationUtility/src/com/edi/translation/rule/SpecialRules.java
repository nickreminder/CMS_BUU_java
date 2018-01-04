package com.edi.translation.rule;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.edi.translation.constants.IndexConstantsBatching;

public class SpecialRules {
	public static final String ISA06_RECEIVER_ID="ISA06_RECEIVER_ID";
	public static final String ISA09_CURRENT_DATE = "ISA09_CURRENT_DATE";
	public static final String ISA10_CURRENT_TIME = "ISA10_CURRENT_TIME";
	public static final String GS04_CURRENT_DATE = "GS04_CURRENT_DATE";
	public static final String GS05_CURRENT_TIME = "GS05_CURRENT_TIME";
	public static final String INS02_DERIVE_RELATIONSHIP_CODE = "INS02_DERIVE_RELATIONSHIP_CODE";
	public static final String SE01_NUMBER_OF_TOTAL_SEGMENTS_IN_TRANSACTION = "ST01_NUMBER_OF_TOTAL_SEGMENTS_IN_TRANSACTION";
	public static final String GE01_NUMBER_OF_TOTAL_TRANSACTIONS_IN_GROUP = "GS01_NUMBER_OF_TOTAL_TRANSACTIONS_IN_GROUP";
	public static final String GS02_TRIM_QHP_14_LENGTH = "GS02_TRIM_QHP_14_LENGTH";
	public static final String IEA01_NUMBER_OF_TOTAL_GROUPS_IN_INTERCHANGE = "IEA01_NUMBER_OF_TOTAL_GROUPS_IN_INTERCHANGE";
	public static final String REMOVE_DELIMITER_FROM_VALUE = "REMOVE_DELIMITER_FROM_VALUE";
	public static final String QTY02_MEMBER_TOTAL = "QTY02_MEMBER_TOTAL";
	public static final String QTY02_DEPENDENT_TOTAL = "QTY02_DEPENDENT_TOTAL";
	public static final String CURRENT_DATE_WITH_PROVIDED_FORMAT = "CURRENT_DATE_WITH_PROVIDED_FORMAT";
	public static final String CURRENT_TIME_WITH_PROVIDED_FORMAT = "CURRENT_TIME_WITH_PROVIDED_FORMAT";
	public static final String ISA_CONTROL_NUMBER_GENERATION = "ISA_CONTROL_NUMBER_GENERATION";
	public static final String GS_CONTROL_NUMBER_GENERATION = "GS_CONTROL_NUMBER_GENERATION";
	public static final String INTERCHANGE_CONTROL_NUMBER_INCREMENT = "INTERCHANGE_CONTROL_NUMBER_INCREMENT";
	public static final String GROUP_CONTROL_NUMBER_INCREMENT = "GROUP_CONTROL_NUMBER_INCREMENT";
	public static final String BATCH_NUMBER_OF_TOTAL_TRANSACTIONS_IN_GROUP ="BATCH_NUMBER_OF_TOTAL_TRANSACTIONS_IN_GROUP";
	public static final String DERIVE_CONFIRMATION_CODE ="DERIVE_CONFIRMATION_CODE";

	public String generateISA06ReceiverID(String value) {
		String receiverID = value;
		
		if(receiverID != null ) {
			while(receiverID.length() < 15) {
				receiverID += " ";
			}
		}
		
		return receiverID;
	}
	
	public String generateDate(String format) {
		String date = new SimpleDateFormat(format).format(new Date());
		
		return date;
	}
	
	public String generateTime(String format) {
		String time = new SimpleDateFormat(format).format(new Date());
		
		return time;
	}
	
	// YYMMDD
	public String generateISA09CurrentDate() {
		String date = new SimpleDateFormat("yyMMdd").format(new Date());
		
		return date;
	}
	
	// HHMM
	public String generateISA10CurrentTime() {
		String time = new SimpleDateFormat("HHmm").format(new Date());
		
		return time;
	}
	
	// CCYYMMDD
	public String generateGS04CurrentDate() {
		String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
		
		return date;
	}
	
	// HHMM, or HHMMSS, or HHMMSSD, or HHMMSSDD, where H = hours (00-23), M = minutes (00-59), S = integer seconds (00-59) and DD = decimal seconds; decimal seconds are
	// expressed as follows: D = tenths (0-9) and DD = hundredths (00-99)
	public String generateGS05CurrentTime() {
		String time = generateISA10CurrentTime();	 
		
		return time;
	}
	
	public String deriveRelationshipCode(String val, boolean isSubscriber) {
		String relationshipValue = "";
		
		if(val != null && !val.trim().isEmpty()) {
			
			if(":::".equals(val) || ": : :".equals(val))
			{
				val += " "; 
				System.out.println("Appending space");
			}
			String[] valueArray = val.split(":");
				if(valueArray.length == 4 && valueArray[3] != null && valueArray[3].trim().length() > 0) {
					relationshipValue = valueArray[3];
				} else if(valueArray.length > 1 && valueArray[1] != null && valueArray[1].trim().length() > 0) {
					relationshipValue = valueArray[1];
				} else {
					if(isSubscriber)
						relationshipValue = "18";
					else 
						relationshipValue = "D2";
				}
		}	
		
		return relationshipValue;
	}
	
	public String removeDelimiterFromValue(String value, String delimiter) {
		String cleanedValue = "";
		int delimiterLen = delimiter.length();
		
		while(value != null && value.contains(delimiter)) {
			cleanedValue += value.substring(0, value.indexOf(delimiter));
			value = value.substring(value.indexOf(delimiter)+delimiterLen);
		}
		
		if(value != null) {
			cleanedValue += value; 
		}
		
		return cleanedValue;
	}
	
	public String trimQHPTo14Length(String value) {
		if(value != null && value.length() > 14) {
			value = value.substring(0, 14);
		}
		
		return value;
	}
	
	public String insertDelimitersToValue(String value, char delimiter, String[] indexArray) {
		String delimitedValue = value;
		
		int start = 0;
		int end = 0;
		if(value != null && !value.isEmpty()) {
		
			for(String index: indexArray) {
				//System.out.println(index);
				end = Integer.parseInt(index);
				
				delimitedValue = delimitedValue.substring(0, end-1) + delimiter + delimitedValue.substring(end-1);
				start = end;
			}
		}
		return delimitedValue;
	}

	public String generateISAControlNumber( String issuerId, String isaControlNumberFile) {
		String isaControlNumber = null;
		InputStream functionalCodePropInStream;
		Properties isaControlNumbers = new Properties();
		try {
			functionalCodePropInStream = new FileInputStream(isaControlNumberFile);
			isaControlNumbers.load(functionalCodePropInStream);
		
			if(isaControlNumbers.containsKey(issuerId))
			{
				isaControlNumber = (String) isaControlNumbers.get(issuerId);
				int isaNumber = Integer.parseInt(isaControlNumber);
				isaNumber = isaNumber +1;
				isaControlNumber = intToStringWithNineDigit(isaNumber,9);
			}
			else
			{
				int isaNumber = 000000001;
				isaControlNumber = intToStringWithNineDigit(isaNumber,9);
			}
			isaControlNumbers.setProperty(issuerId, isaControlNumber);
			OutputStream output = new FileOutputStream(isaControlNumberFile);
			isaControlNumbers.store(output, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isaControlNumber;
	}

	public String generateGSControlNumber(String issuerId, String gsControlNumberFile) {
		String gsControlNumber = null;

		InputStream functionalCodePropInStream;
		Properties gsControlNumbers = new Properties();
		try {
			functionalCodePropInStream = new FileInputStream(gsControlNumberFile);
			gsControlNumbers.load(functionalCodePropInStream);
		
			if(gsControlNumbers.containsKey(issuerId))
			{
				gsControlNumber = (String) gsControlNumbers.get(issuerId);
				int gsNumber = Integer.parseInt(gsControlNumber);
				gsNumber = gsNumber +1;
				gsControlNumber = String.valueOf(gsNumber);
			}
			else
			{
				int gsNumber = 1;
				gsControlNumber = String.valueOf(gsNumber);
			}
			gsControlNumbers.setProperty(issuerId, gsControlNumber);
			OutputStream output = new FileOutputStream(gsControlNumberFile);
			gsControlNumbers.store(output, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return gsControlNumber;
	}
	
	private String intToStringWithNineDigit(int num, int digits) {
        String output = Integer.toString(num);
        while (output.length() < digits) output = "0" + output;
        return output;
    }

	public String populateConfirmationCode(String[] dataSet) {
		
		//System.out.println("[populateConfirmationCode] 11th element:" + dataSet[IndexConstantsBatching.TRANS_TYPE_CODE_INDEX]);
		//System.out.println("[populateConfirmationCode] 99th element:" + dataSet[IndexConstantsBatching.CONFIRMATION_INDICATOR_INDEX]);
		String policyStatusCode = dataSet[IndexConstantsBatching.TRANS_TYPE_CODE_INDEX];
		String confirmationIndicator = dataSet[IndexConstantsBatching.CONFIRMATION_INDICATOR_INDEX];
		String superdededIndicator = dataSet[IndexConstantsBatching.SUPERSEDED_INDICATOR_INDEX];
		superdededIndicator = (null!=superdededIndicator)?superdededIndicator.trim():superdededIndicator;
		//System.out.println("superdededIndicator:" + superdededIndicator);
		policyStatusCode = (null!=policyStatusCode)?policyStatusCode.trim():policyStatusCode;
		confirmationIndicator = (null!=confirmationIndicator)?confirmationIndicator.trim():confirmationIndicator;
		String confimrationCode = confirmationIndicator;
		
		if("y".equalsIgnoreCase(superdededIndicator))
		{
			confimrationCode = "";
		}
		else
		{
			if("1".equalsIgnoreCase(policyStatusCode) && "N".equalsIgnoreCase(confirmationIndicator))
			{
				confimrationCode = "OVERRIDE INITIAL";
			}
			else if("1".equalsIgnoreCase(policyStatusCode) && "Y".equalsIgnoreCase(confirmationIndicator))
			{
				confimrationCode = "OVERRIDE CONFIRM";
			}
			else if("3".equalsIgnoreCase(policyStatusCode) && "Y".equalsIgnoreCase(confirmationIndicator))
			{
				confimrationCode = "OVERRIDE CANCEL ERROR";
			}
			else if("3".equalsIgnoreCase(policyStatusCode) && "N".equalsIgnoreCase(confirmationIndicator))
			{
				confimrationCode = "OVERRIDE CANCEL";
			}
			else if("4".equalsIgnoreCase(policyStatusCode) && "N".equalsIgnoreCase(confirmationIndicator))
			{
				confimrationCode = "OVERRIDE TERM";
			}
			else if("4".equalsIgnoreCase(policyStatusCode) && "Y".equalsIgnoreCase(confirmationIndicator))
			{
				confimrationCode = "OVERRIDE CONFIRM AND TERM";
			}
		}
		
		
		//System.out.println("[confimrationCode]:" + confimrationCode);
		return confimrationCode;
	}
}
