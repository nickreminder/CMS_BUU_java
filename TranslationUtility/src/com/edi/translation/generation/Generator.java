package com.edi.translation.generation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.spi.DirStateFactory.Result;
import javax.rmi.CORBA.Util;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.edi.sort.bean.DataLocation;
import com.edi.translation.FlatfileToX12Translation;
import com.edi.translation.bean.GroupBean;
import com.edi.translation.bean.InterchangeBean;
import com.edi.translation.bean.TransactionBean;
import com.edi.translation.constants.ConfigurationConstants;
import com.edi.translation.constants.IndexConstants;
import com.edi.translation.constants.IndexConstantsBatching;
import com.edi.translation.reader.FlatFileLineReader;
import com.edi.translation.reader.RandomFileReader;
import com.edi.translation.reader.TemplateReader;
import com.edi.translation.reporting.FileReportingBean;
import com.edi.translation.reporting.IssuerReportingBean;
import com.edi.translation.rule.SpecialRules;
import com.edi.translation.util.XMLValidationUtil;
import com.edi.translation.writer.Writer;

public class Generator {
	/* Formats:
	 * 1) SUBSTITUTE_RULE
	 * 		## SUBSTITUTE_RULE COLUMN=<Column number> INSERT_DELIMITER=[-(3,7)+(6,9)] REMOVE_DELIMITER=- ##
	 * 		COLUMN - Mandatory
	 *		INSERT_DELIMITER - Optional
	 * 		REMOVE_DELIMITER - Optional
	 * 
	 * 2) ## DERIVE_RULE COLUMN=<Column number> INSERT_DELIMITER=[-(3,7)+(6,9)] REMOVE_DELIMITER=- ##	
	 * 		COLUMN - Mandatory
	 *		INSERT_DELIMITER - Optional
	 * 		REMOVE_DELIMITER - Optional
	 * 
	 * 3) ## SPECIAL_RULE NAME=<Rule Name to be invoked> COLUMN=<Column number> DELIMITER=<Delimier within the value> ##
	 * 
	 * 4) ## REMOVE_SEGMENT_RULE COLUMN=<Column number> [^#]* REMOVE_START=<column to start removal at> REMOVE_END=<column to end removal at> ##
	 * 
	 * 5) ## SEQUENCE_RULE NAME=<Name for the sequence> START_VALUE=<Start value for sequence> MIN_LEN=<Minimum length for sequence> MAX_LEN=<Maximum length for sequence> SCOPE=<For reset of sequence> ##
	 * 
	 * 6) ## REFERENCE_RULE NAME=<Name for the sequence/special rule to reference> SCOPE=<For reset of sequence> ##
	 */
	
	private final String SUBSTITUTE_RULE_TOKEN_PATTERN_STRING = "\\#\\# SUBSTITUTE_RULE COLUMN=[0-9]*( INSERT_DELIMITER=(\\[[^# ]{1,1}\\([0-9,]*\\)\\])*){0,1}( REMOVE_DELIMITER=[^# ]{1,1}){0,1} ##";				// Token pattern for places where the value needs to be inserted
	private final Pattern substituteRulePattern = Pattern.compile(SUBSTITUTE_RULE_TOKEN_PATTERN_STRING); 
	
	// REMOVE_DELIMITER is optional
	private final String DERIVE_RULE_TOKEN_PATTERN_STRING = "\\#\\# DERIVE_RULE COLUMN=[0-9,]* [^#]* \\#\\#";				// Token pattern for places where the value needs to be inserted
	private final Pattern deriveRulePattern = Pattern.compile(DERIVE_RULE_TOKEN_PATTERN_STRING); 
	
	// COLUMN is optional
	private final String SPECIAL_RULE_TOKEN_PATTERN_STRING = "\\#\\# SPECIAL_RULE NAME=[a-z_A-Z0-9]*[^#]* \\#\\#";				// Token pattern for places where the value needs to be inserted
	private final Pattern specialRulePattern = Pattern.compile(SPECIAL_RULE_TOKEN_PATTERN_STRING); 
	
	// REMOVE_START is optional
	// REMOVE_END is optional
	private final String REMOVE_SEGMENT_RULE_TOKEN_PATTERN_STRING = "\\#\\# REMOVE_SEGMENT_RULE COLUMN=[0-9,]* [^#]* \\#\\#";				// Token pattern for places where the value needs to be inserted
	private final Pattern removeSegmentPattern = Pattern.compile(REMOVE_SEGMENT_RULE_TOKEN_PATTERN_STRING); 
	
	private final String SEQUENCE_RULE_TOKEN_PATTERN_STRING = "\\#\\# SEQUENCE_RULE NAME=[^ ]* START_VALUE=[0-9]* MIN_LEN=[0-9]* MAX_LEN=[0-9]* SCOPE=[A-Z]* \\#\\#";				// Token pattern for places where the value needs to be inserted
	private final Pattern sequenceRulePattern = Pattern.compile(SEQUENCE_RULE_TOKEN_PATTERN_STRING); 	
	
	private final String REFERENCE_RULE_TOKEN_PATTERN_STRING = "\\#\\# REFERENCE_RULE NAME=[^ ]* SCOPE=[A-Z]* \\#\\#";				// Token pattern for places where the value needs to be inserted
	private final Pattern referenceRulePattern = Pattern.compile(REFERENCE_RULE_TOKEN_PATTERN_STRING);
	
	private final String COLUMN_PATTERN_STRING = "COLUMN=[0-9,]*";
	private final Pattern columnPattern = Pattern.compile(COLUMN_PATTERN_STRING);
	
	private final String NAME_PATTERN_STRING = "NAME=[^# ]*";
	private final Pattern namePattern = Pattern.compile(NAME_PATTERN_STRING);
	
	private final String MAP_KEY_VALUE_START_SEPERATOR = "<";
	private final String MAP_KEY_VALUE_END_SEPERATOR = ">";
	private final String MAP_STRING_START = "MAP";
	private final String MAP_PATTERN_STRING = "MAP<[^#]*>";
	private final Pattern mapPattern = Pattern.compile(MAP_PATTERN_STRING);
	
	private final String REMOVE_START_PATTERN_STRING = "REMOVE_START=[^# ]*";
	private final Pattern removeStartPattern = Pattern.compile(REMOVE_START_PATTERN_STRING);
	
	private final String REMOVE_END_PATTERN_STRING = "REMOVE_END=[^# ]*";
	private final Pattern removeEndPattern = Pattern.compile(REMOVE_END_PATTERN_STRING);	
	
	private final String REMOVE_TYPE_PATTERN_STRING = "REMOVE_TYPE=[^# ]*";
	private final Pattern removeTypePattern = Pattern.compile(REMOVE_TYPE_PATTERN_STRING);
	
	private final String MINLENGTH_PATTERN_STRING = "MIN_LEN=[^# ]*";
	private final Pattern minLengthPattern = Pattern.compile(MINLENGTH_PATTERN_STRING);
	
	private final String MAXLENGTH_PATTERN_STRING = "MAX_LEN=[^# ]*";
	private final Pattern maxLengthPattern = Pattern.compile(MAXLENGTH_PATTERN_STRING);
	
	private final String SCOPE_PATTERN_STRING = "SCOPE=[^# ]*";
	private final Pattern scopePattern = Pattern.compile(SCOPE_PATTERN_STRING);
	
	private final String START_VALUE_PATTERN_STRING = "START_VALUE=[^# ]*";
	private final Pattern StartValuePattern = Pattern.compile(START_VALUE_PATTERN_STRING);
	
	private final String DELIMITER_PATTERN_STRING = "DELIMITER=[^# ]*";
	private final Pattern delimiterPattern = Pattern.compile(DELIMITER_PATTERN_STRING);
	
	private final String REMOVE_DELIMITER = "REMOVE_DELIMITER=[^# ]*";
	private final Pattern removeDelimiterPattern = Pattern.compile(REMOVE_DELIMITER);
	
	private final String INSERT_DELIMITER = "INSERT_DELIMITER=(\\[[^# ]{1,1}\\([0-9,]*\\)\\])*";
	private final Pattern insertDelimiterPattern = Pattern.compile(INSERT_DELIMITER);
	
	private final String FORMAT_PATTERN_STRING = "FORMAT=[^# ]*";
	private final Pattern formatPattern = Pattern.compile(FORMAT_PATTERN_STRING);
	
	private final String SCOPE_ISA="ISA";
	private final String SCOPE_GS="GS";
	private final String SCOPE_ST="ST";
	private final String SCOPE_MEMBER="MEMBER";
	private final String VALUE_PRESENT="PRESENT";
	private final String VALUE_ABSENT="ABSENT";
	private final String VALUE_REMOVE="REMOVE";
	private final String VALUE_SEPERATOR=",";
	
	private final String COLUMN_VALUE="COLUMN_VALUE";
	
	private final int INTERCHANGE_ISA=0, INTERCHANGE_IEA=1, GROUP_GS=2, GROUP_GE=3, TRANSACTION_ST=4,TRANSACTION=5, TRANSACTION_SE=6;  
	private int transactionSegmentCount = 0;
	private int transactionCount = 0;
	private int groupCount = 0;
	protected int memberCountWithinST = 0;
	protected int dependentCountWithinST = 0;	
	
	private Map<String, String> interchangeSequenceMap = new HashMap<String, String>();
	private Map<String, String> groupSequenceMap = new HashMap<String, String>();
	private Map<String, String> transactionSequenceMap = new HashMap<String, String>();
	private Map<String, String> memberSequenceMap = new HashMap<String, String>();
	
	private Map<String, String> interchangeControlNumber = new HashMap<String, String>();
	private Map<String, String> groupControlNumber = new HashMap<String, String>();

	private int segCountToBeCleaned = 0;
	protected String templateFileNameWithPath;
	//private String outputFileLocation;
	private String reportingFileLocation;
	
	public static final String FILE_TYPE_XML = "XML";
	public static final String FILE_TYPE_BATCH = "BATCH";
	public static final String FILE_TYPE_X12 = "X12";
	
	private final int REPORT_LINE_SIZE = 150;
	private final int POLICY_UNITS_TO_BUNDLE_FOR_XML = 1000;
	
	protected Writer writer = new Writer();
	protected String isaControlNumberFile = new String();
	protected String gsControlNumberFile = new String();
	String hostName;
	
	public static void main(String args[]) {
		long start = Calendar.getInstance().getTimeInMillis();
		//System.out.println("Starting the tranlation process");	
		
		//String templateFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Template/834X12Template.ver5.tem";
		//String outputFileLocation = "C:/Parimanshu/FlatFileTranslation/Output/";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/AllEnrolledMembers-2014-03-26.psv";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/TEST DATA.dat";
		
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/FlatFile_UnitTesting_4242014.txt";
		
		String templateFileNameWithPath = null, outputFileLocation = null, flatFileNameWithPath = null, 
				reportingFileLocation = null, fileTypeToGenerate = null, envVariable= null, xsdLocation = null, errorFolder = null;
		
		String receiverOfTranslatedFile = null, functionCodeForTranslatedFile = null,  receiverOfReport = null, functionCodeForReport = null; 
				
		if(args.length < 5) {
			System.out.println("Need to specify the following files in the sequence: ");
			System.out.println("1. Template file name with path");
			System.out.println("2. X12 Output folder path");
			System.out.println("3. Flat file name with path");
			System.out.println("4. Reporting file location");
			System.out.println("5. File Type to generate( Avaliable options \"XML\", \"X12\", \"BATCH\".)");	
			System.out.println("6. Environment variable( Avaliable options \"T\", \"P\".)");
			System.out.println("7. XSD for the XML(This is required if generating XML) [or] Host id for BATCH mode.");
			System.out.println("8. Error folder for the files failing XSD validation(This is required if generating XML) [or] Error folder location for BATCH mode.");
			System.out.println("9. Receiver ID for translated file(This is required if generating XML) [or] ISA Control number property file with location for BATCH mode.");
			System.out.println("10. Translated file Function Code(This is required if generating XML) [or] GS Control number property file with location for BATCH mode." );
			System.out.println("11. Receiver ID for the report(This is required if generating XML)");
			System.out.println("12. Report Function Code(This is required if generating XML)");
		} else {
			templateFileNameWithPath = args[0];
			outputFileLocation = args[1];
			flatFileNameWithPath = args[2].trim();
			reportingFileLocation = args[3];
			fileTypeToGenerate = args[4];
			envVariable = args[5];
			
			if(fileTypeToGenerate.equals(FILE_TYPE_XML)) {
				xsdLocation = args[6];
				errorFolder = args[7];
				receiverOfTranslatedFile = args[8];
				functionCodeForTranslatedFile = args[9];
				receiverOfReport = args[10];
				functionCodeForReport = args[11];
			}				
		}	
		
		boolean fullyTranslated = true;
		Generator generator = null;
		String logFolder="";
		if(fileTypeToGenerate.equals(FILE_TYPE_BATCH)) {
			xsdLocation = args[6]; // Host name for batch
			errorFolder = args[7]; // Error location for batch
			receiverOfTranslatedFile = args[8]; //ISA  property file with location
			functionCodeForTranslatedFile = args[9];//GS  property file with location
			logFolder = args[10]; //Log file location
			generator = new BatchGenerator(templateFileNameWithPath, reportingFileLocation, logFolder);
		} else {
			generator = new Generator(templateFileNameWithPath, reportingFileLocation);
		}
				
		try {
			fullyTranslated = generator.generateHipaaDataGroupedByGS(flatFileNameWithPath, fileTypeToGenerate, envVariable, xsdLocation, errorFolder, outputFileLocation, receiverOfTranslatedFile,
			functionCodeForTranslatedFile, receiverOfReport, functionCodeForReport);			
			
		}  catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(3);
		}
		catch (Exception e) {			
			e.printStackTrace();
			System.exit(3);
		}
		System.out.println("Time Taken(s): " + (Calendar.getInstance().getTimeInMillis() - start));	
		
		if(!fullyTranslated) {
			System.exit(2);
		} else
			System.exit(0);
	}
	
	
	public Map<String, Map<String, Map<String, ArrayList<DataLocation>>>> sort(RandomFileReader flatFileReader, boolean isXMLFileGenerated) throws Exception {
		long start = Calendar.getInstance().getTimeInMillis();
		Map<String, Map<String, Map<String, ArrayList<DataLocation>>>> fileSortMap = new HashMap<String, Map<String, Map<String, ArrayList<DataLocation>>>>();
		
		System.out.println("Start Sorting...");
		
		boolean firstLineSkipped = false;
		String prevHiosID = null;
		String prevQhpID = null;
		String prevPolicyID = null;
		
		int count = 1;
		
		while(flatFileReader.hasMoreData()) {	
			String[] lineArray = flatFileReader.getCurrentElementArray();
			
			if(lineArray == null) {
				flatFileReader.linePointerIncrement();	
				continue;
			}				
			
			if(!firstLineSkipped) {	// First line skipped as it contains the difinitions.
				firstLineSkipped = true;
				flatFileReader.linePointerIncrement();	
				continue;
			}
			
			String currentHiosID = lineArray[IndexConstants.HIOS_ID_INDEX-1].trim();			
			// Grouping changed from Member ID to Exchange assigned policy ID
			//String currentMemberID = lineArray[IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1].trim();
			String currentPolicyID = lineArray[IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1].trim();
			String currentQhpID = "";
			
			if(!isXMLFileGenerated) {
				currentQhpID = lineArray[IndexConstants.QHP_IENTIFIER_INDEX-1].trim().substring(0,14);
			}			
			
			Map<String, Map<String, ArrayList<DataLocation>>> hiosMap = fileSortMap.get(currentHiosID);		
			if(hiosMap == null) {
				hiosMap = new HashMap<String, Map<String, ArrayList<DataLocation>>>();
				fileSortMap.put(currentHiosID, hiosMap);	
			}
			
			Map<String, ArrayList<DataLocation>> qhpMap = hiosMap.get(currentQhpID);
			if(qhpMap == null) {
				qhpMap = new HashMap<String, ArrayList<DataLocation>>();
				hiosMap.put(currentQhpID, qhpMap);
			}
			
			ArrayList<DataLocation> memberIndexArrayList = qhpMap.get(currentPolicyID);
			if(memberIndexArrayList == null) {
				memberIndexArrayList = new ArrayList<DataLocation>();
				qhpMap.put(currentPolicyID, memberIndexArrayList);
			}			
			
			//long currentLine = flatFileReader.getCurrentRowCount();
						
			if(currentHiosID.equals(prevHiosID) && currentQhpID.equals(prevQhpID) && currentPolicyID.equals(prevPolicyID)) {
				DataLocation loc = memberIndexArrayList.get(memberIndexArrayList.size() - 1);				
				loc.setNumberOfLines(loc.getNumberOfLines() + 1);		
				
				++count;
			} else {
				++count;
				//System.out.println(count + ":" + currentPolicyID + ":" + flatFileReader.getCurrentSegmentLocation());
				
				DataLocation loc = new DataLocation();
				loc.setStartLocation(flatFileReader.getCurrentSegmentLocation());
				loc.setNumberOfLines(1);
				
				memberIndexArrayList.add(loc);
			}
			
			prevHiosID = currentHiosID;
			prevQhpID = currentQhpID;
			prevPolicyID = currentPolicyID;
			
			flatFileReader.linePointerIncrement();	
		}
		System.out.println("Time Taken(s) for sorting: " + (Calendar.getInstance().getTimeInMillis() - start));
		
		return fileSortMap;
	}	
	
	public boolean generateHipaaDataGroupedByGS(String flatFileNameWithPath, String fileTypeToGenerate, String envVariable, String xsdLocation, 
			String errorFolder, String outputFileLocation, 
			String receiverOfTranslatedFile, String functionCodeForTranslatedFile, String receiverOfReport, String functionCodeForReport) throws IOException, SAXException, Exception {		
		
		boolean isXMLFileGenerated = (fileTypeToGenerate.equals(FILE_TYPE_XML)) ? true:false;
		
		//Generator generator = new Generator();	
		FlatfileToX12Translation translator = new FlatfileToX12Translation();
		RandomFileReader flatFileReader = new RandomFileReader(flatFileNameWithPath, isXMLFileGenerated);
		
		TemplateReader template = new TemplateReader(templateFileNameWithPath);
		//flatFileReader.setFileToRead(flatFileNameWithPath);
		
		Map<String, Map<String, Map<String, ArrayList<DataLocation>>>> fileSortMap = sort(flatFileReader, isXMLFileGenerated);
				
		FileReportingBean reportingBean = new FileReportingBean();
		reportingBean.setFileName(flatFileNameWithPath.substring(flatFileNameWithPath.lastIndexOf("/")+1));		

		boolean fullyTranslated = true;
		
		Iterator<String> itr = fileSortMap.keySet().iterator();	
		boolean writerInitialized = false;
		

		while(itr.hasNext()) {// New file			
			IssuerReportingBean issuerReportingBean = new IssuerReportingBean();
			String hiosID = "";
			String policyNumber = "";
			String batchID = "";
			String recordID = "";
			
			int stCount = 0;
			int policyUnitCounter = 0;
			boolean fileInitilazied = false;			
			
			boolean isaGenareted = false;
			
			String ge = null, iea= null;
			ArrayList<String> gsArrayList = null;
			ArrayList<String> isaArrayList = null;
			
			boolean gsGenerated = false;
			
			try{
			// Lines per house hold
				hiosID = itr.next();
				issuerReportingBean.setHIOSID(hiosID);
				reportingBean.setIssuerReport(issuerReportingBean);	
				Map<String, Map<String, ArrayList<DataLocation>>> qhpMap = fileSortMap.get(hiosID);
				Iterator<String> itrQhp = qhpMap.keySet().iterator();
				
				int hiosSplitCounter = 1;
				while(itrQhp.hasNext()) {		// START QHP 
					String qhpID = itrQhp.next();
										
					Map<String, ArrayList<DataLocation>> memberMap = qhpMap.get(qhpID);	
					
					Iterator<String> itrMemberID = memberMap.keySet().iterator();
					if(!isXMLFileGenerated)		// New GS for X12
						gsGenerated = false;
										
					while(itrMemberID.hasNext()) {		// START Member ID						
						String memberID = itrMemberID.next();
						
						ArrayList<DataLocation> lineLocationArrayList = memberMap.get(memberID);
						
						
						
						ArrayList<ArrayList<String[]>> policyUnitsArrayListForSubscriber = identifyPolicies(lineLocationArrayList, flatFileReader, reportingBean);
						
						for(ArrayList<String[]> singlePUArrayList:policyUnitsArrayListForSubscriber) {
							for(String[] memberArray:singlePUArrayList) {
								String subscriberIndicator = "";
								if(memberArray.length >= IndexConstants.SUBSCRIBER_INDICATOR_INDEX) {
									subscriberIndicator = memberArray[IndexConstants.SUBSCRIBER_INDICATOR_INDEX-1].trim();
								}
								
								if(ConfigurationConstants.SUBSCRIBER_INDICATOR_Y.equalsIgnoreCase(subscriberIndicator)){
									reportingBean.setTotalSubscriberCount(1);
								} else if(ConfigurationConstants.SUBSCRIBER_INDICATOR_N.equalsIgnoreCase(subscriberIndicator)){
									reportingBean.setTotalDependentCount(1);
								}
								reportingBean.setTotalMemberCount(1);								
							}
						}
						
						reportingBean.setNumberOfHouseHolds(policyUnitsArrayListForSubscriber.size());					
						
						for(ArrayList<String[]> singlePUArrayList:policyUnitsArrayListForSubscriber) {
							//for(String[] memberArray:singlePUArrayList) {
							String[] memberArray = singlePUArrayList.get(0);
								try{
									if(memberArray.length >= IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX)
										policyNumber = memberArray[IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1];
									
									if(memberArray.length >= IndexConstants.BATCH_ID_INDEX)
										batchID = memberArray[IndexConstants.BATCH_ID_INDEX-1];
									
									if(memberArray.length >= IndexConstants.RECORD_ID_INDEX)
										recordID = memberArray[IndexConstants.RECORD_ID_INDEX-1];
									//System.out.println( "Processing policy: " + policyNumber + " batch id" + batchID + "recordId: " + recordID );
									if(!isaGenareted) {
										isaArrayList = generateISA((InterchangeBean)template.getDataBean(template.INTERCHANGE_BEAN), memberArray);
										
										isaGenareted = true;
									}
									
									if(!gsGenerated) {
										gsArrayList = generateGS((GroupBean)template.getDataBean(template.GROUP_BEAN), memberArray);
										
										gsGenerated = true;
									}
									
									stCount++;
									policyUnitCounter++;
									
									fileInitilazied = writeSTData(isaArrayList, gsArrayList, fileInitilazied, singlePUArrayList, template, flatFileNameWithPath, fileTypeToGenerate,
											reportingBean, issuerReportingBean, hiosSplitCounter, envVariable, outputFileLocation, 
											receiverOfTranslatedFile, functionCodeForTranslatedFile);
																			
									isaArrayList = null;
									gsArrayList = null;									
					
									writerInitialized = true;
									
									/*if(isXMLFileGenerated && policyUnitCounter >= POLICY_UNITS_TO_BUNDLE_FOR_XML) {
										fileInitilazied = false;
										isaGenareted = false;
										gsGenerated = false;
										policyUnitCounter = 0;
										
										hiosSplitCounter++;
									}*/
								} catch(Exception e) {
									e.printStackTrace();
									issuerReportingBean.setException(policyNumber+ "_" + batchID + "_" + recordID, e);

									fullyTranslated = false;
								} finally {
									//singlePUArrayList.clear();
								}
							//}
						}
												
						lineLocationArrayList = null;
					}
					
					if(fileInitilazied) {
						ge = generateGE((GroupBean)template.getDataBean(template.GROUP_BEAN), null);
						write(ge);
					}
					
					ge = null;
					
					qhpMap.put(qhpID, null);
					
				}// end QHP
			
			}catch(Exception e) {
				e.printStackTrace();
				
				issuerReportingBean.setException(policyNumber+ "_" + batchID + "_" + recordID, e);
				fullyTranslated = false;
			} finally {
				if(fileInitilazied) {							
					iea = generateIEA((InterchangeBean)template.getDataBean(template.INTERCHANGE_BEAN), null);
					write(iea);			
					reportingBean.setTranslatedIssuerCount(1);
				} 
			}
		}
		
		if(writerInitialized)
			writer.close();
		
		System.out.println("Generating report");
		boolean xsdValidationSuccessful = generateReport(reportingBean, envVariable, isXMLFileGenerated, xsdLocation, errorFolder, receiverOfReport, functionCodeForReport, flatFileNameWithPath);
		
		if(fullyTranslated)
			fullyTranslated = xsdValidationSuccessful;
		
		return fullyTranslated;
		//System.out.println("-------------------------------------------------" + count + "|" + dataSetArrayList.get(0)[IndexConstants.SUBSCRIBER_INDICATOR_INDEX]);
	}
	
	public boolean writeSTData(ArrayList<String> isaArrayList, ArrayList<String> gsArrayList, boolean fileInitilazied, ArrayList<String[]> dataSetArrayList, 
			TemplateReader template, String flatFileNameWithPath, String fileTypeToGenerate, FileReportingBean reportingBean, 
			IssuerReportingBean issuerReportingBean, int hiosSplitCounter, String envVariable, String outputFileLocation, String receiverOfTranslatedFile,
			String functionCodeForTranslatedFile) throws IOException, Exception {
		
		//reportingBean.setNumberOfHouseHolds(1);
		try
		{
		memberCountWithinST = dependentCountWithinST = 0;
		int subscriberCount = 0;				
		
		for(String[] data:dataSetArrayList) {
			String subscriberIndicator = "";
			if(data.length >= IndexConstants.SUBSCRIBER_INDICATOR_INDEX) {
				subscriberIndicator = data[IndexConstants.SUBSCRIBER_INDICATOR_INDEX-1].trim();
			}
			
			if(ConfigurationConstants.SUBSCRIBER_INDICATOR_Y.equalsIgnoreCase(subscriberIndicator)){
				++memberCountWithinST;
				//reportingBean.setTotalSubscriberCount(1);
				++subscriberCount;				
			} else if(ConfigurationConstants.SUBSCRIBER_INDICATOR_N.equalsIgnoreCase(subscriberIndicator)){
				++dependentCountWithinST;
				//reportingBean.setTotalDependentCount(1);
				++memberCountWithinST;
			}
			//reportingBean.setTotalMemberCount(1);
		}
	
		String exchangeAssignedPolicyNumber = dataSetArrayList.get(0)[IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1].trim();
		
		
		ArrayList<String> transactionSegmentData = generateTransaction((TransactionBean)template.getDataBean(template.TRANSACTION_BEAN), dataSetArrayList.get(0));
				
		ArrayList<String> transactionLoopData = new ArrayList<String>();
		for(String[] segmentArrayList:dataSetArrayList) {
			transactionLoopData.addAll(generateRepeatSegment((TransactionBean)template.getDataBean(template.TRANSACTION_BEAN), segmentArrayList));
		}
		
		String se = generateSE((TransactionBean)template.getDataBean(template.TRANSACTION_BEAN), dataSetArrayList.get(0));
		
		issuerReportingBean.setMemberCount(memberCountWithinST);
		issuerReportingBean.setDependentCount(dependentCountWithinST);
		issuerReportingBean.setSubscriberCount(subscriberCount);
		issuerReportingBean.setNumberOfHouseHolds(1);
		
		
		if(!fileInitilazied) {
			String fileName = null;
			String fileExtension = null;
			String receiver = dataSetArrayList.get(0)[3].trim();
			String functionCode = "";
			if(fileTypeToGenerate.equals(FILE_TYPE_XML)) {
				//fileExtension = "xml";
				//receiver = receiverOfTranslatedFile;
				//functionCode = functionCodeForTranslatedFile;
				
				int index = flatFileNameWithPath.lastIndexOf("/");
				if(index!=-1)
				{
					flatFileNameWithPath = flatFileNameWithPath.substring(index+1, flatFileNameWithPath.length());
				}
				System.out.println("Flatfile In File Name:" + flatFileNameWithPath);
				flatFileNameWithPath = writer.generateFileName(flatFileNameWithPath, false);
				fileName = setOuputFileLocation(true, null, outputFileLocation, flatFileNameWithPath, fileExtension, envVariable);
			} else {
				functionCode = "834X12";
				fileName = setOuputFileLocation(true, null, outputFileLocation, receiver + "." + functionCode, fileExtension, envVariable);
			}
			
			System.out.println("XML Out File Name:" + fileName);
			issuerReportingBean.setFileGenerated(fileName);
			issuerReportingBean.setFileGeneratedType(fileTypeToGenerate);
			issuerReportingBean.setFileGeneratedLocation(outputFileLocation);
		}	
		
		String segData = "";
		for(String segment:transactionSegmentData) {
			segData += segment;
		}
		
		if(isaArrayList != null) {
			String isa = "";
			
			for(String segment:isaArrayList) {
				isa += segment;			
			}
			
			write(isa);
		}
		
		if(gsArrayList != null) { 
			String gs = "";
			
			for(String segment:gsArrayList) {
				gs += segment;			
			}
			
			write(gs);
		}
			
		write(segData);
			
		segData = "";
		
		int repeatCount = 0;
		for(String segment:transactionLoopData) {
			segData += segment;	
			repeatCount++;
			
			if(repeatCount > 100) {
				repeatCount = 0;
				write(segData);
				segData = "";
			}
		}
		write(segData);
		write(se);
		
		//issuerReportingBean.setNumberOfHouseHolds(1);
		fileInitilazied = true;
		
		reportingBean.setTranslatedHouseHoldCount(1);
		reportingBean.setTranslatedSubscriberCount(subscriberCount);
		reportingBean.setTranslatedDependentCount(dependentCountWithinST);
		reportingBean.setTranslatedMemberCount(memberCountWithinST);
		}
		catch(Exception e)
		{
			throw e;
		}
		return fileInitilazied;
	}
	
	public ArrayList<ArrayList<String[]>> identifyPolicies(ArrayList<DataLocation> lineLocationArrayList, RandomFileReader flatFileReader, 
			FileReportingBean reportingBean) throws Exception {
		String[] currentElementArray = null;
		String[] previousElementArray = null;
		
		ArrayList<ArrayList<String[]>> policyArrayList = new ArrayList<ArrayList<String[]>>();
		
		for(DataLocation seekLocation: lineLocationArrayList) {
			long numberOfLines = seekLocation.getNumberOfLines();
			
			flatFileReader.moveFilePointer(seekLocation.getStartLocation());	
											
			for(long i=0; i < numberOfLines;++i) {
				currentElementArray = flatFileReader.getCurrentElementArray();		
				
				getPolicyUnitData(previousElementArray, currentElementArray, policyArrayList, reportingBean);				
				
				previousElementArray = currentElementArray;
				flatFileReader.linePointerIncrement();
			}
		}
		
		return policyArrayList;
	}
	
	public boolean getPolicyUnitData(String[] prevElementDataArray, String[] currentElementDataArray, ArrayList<ArrayList<String[]>> policyArrayList, FileReportingBean reportingBean) {
		boolean policyChanged = false;
		
		if(policyArrayList.isEmpty())
			policyArrayList.add(new ArrayList<String[]>());
			
		ArrayList<String[]> transactionArrayList = policyArrayList.get(policyArrayList.size() -1);
				
		if(prevElementDataArray != null) {		
			String[] lineArray = currentElementDataArray;
			String memberID = "", memberIndicator = "", HIOSID = "", insuranceLineOfBusiness = "";
			String previousMemberID = "", previousMemberIndicator = "", previousHIOSID = "", previousInsuranceLineOfBusiness = "";
			
			//if(lineArray.length >= IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX)
			//	memberID = lineArray[IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1];
			if(lineArray.length >= IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX)
				memberID = lineArray[IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1];
			
			
			if(lineArray.length >= IndexConstants.SUBSCRIBER_INDICATOR_INDEX)
				memberIndicator = lineArray[IndexConstants.SUBSCRIBER_INDICATOR_INDEX-1];	
				
			if(lineArray.length >= IndexConstants.HIOS_ID_INDEX)
				HIOSID = lineArray[IndexConstants.HIOS_ID_INDEX-1];	
				
			if(lineArray.length >= IndexConstants.INSURANCE_LINE_OF_BUSINESS_INDEX)
				insuranceLineOfBusiness = lineArray[IndexConstants.INSURANCE_LINE_OF_BUSINESS_INDEX-1];	
			
			
			//if(prevElementDataArray.length >= IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX)
			//	previousMemberID = prevElementDataArray[IndexConstants.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1];
			if(prevElementDataArray.length >= IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX)
				previousMemberID = prevElementDataArray[IndexConstants.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1];
			
			if(prevElementDataArray.length >= IndexConstants.SUBSCRIBER_INDICATOR_INDEX)
				previousMemberIndicator = prevElementDataArray[IndexConstants.SUBSCRIBER_INDICATOR_INDEX-1];	
				
			if(prevElementDataArray.length >= IndexConstants.HIOS_ID_INDEX)
				previousHIOSID = prevElementDataArray[IndexConstants.HIOS_ID_INDEX-1];	
				
			if(prevElementDataArray.length >= IndexConstants.INSURANCE_LINE_OF_BUSINESS_INDEX)
				previousInsuranceLineOfBusiness = prevElementDataArray[IndexConstants.INSURANCE_LINE_OF_BUSINESS_INDEX-1];	
				
				
			if((memberID.equals(previousMemberID)) 
					&& (insuranceLineOfBusiness.equals(previousInsuranceLineOfBusiness))) {
				if(memberIndicator.equals(ConfigurationConstants.SUBSCRIBER_INDICATOR_Y)) {
					transactionArrayList.add(0, lineArray);
				}
				else
					transactionArrayList.add(lineArray);				
			} else {
				policyArrayList.add(new ArrayList<String[]>());
				
				transactionArrayList = policyArrayList.get(policyArrayList.size() -1);
				transactionArrayList.add(lineArray);
				
				policyChanged = true;
			}
		} else {
			transactionArrayList.add(currentElementDataArray);
		}
		
		return policyChanged;
	}

	protected boolean generateReport(FileReportingBean reportingBean, String envVariable, 
			boolean xmlGenerated, String xsdLocation, String errorFolder, String receiver, String functionCode,
			String flatFileNameWithPath) throws IOException {
		boolean xsdValidationSucessful = true;
		
		XMLValidationUtil val = new XMLValidationUtil();
		
		Writer writer = new Writer();
		
		if(xmlGenerated)
		{
			// EXISTING CODE writer.initialize(true, null, reportingFileLocation, receiver + "." + functionCode, null, envVariable);
			int index = flatFileNameWithPath.lastIndexOf("/");
			if(index!=-1)
			{
				flatFileNameWithPath = flatFileNameWithPath.substring(index+1, flatFileNameWithPath.length());
			}
			flatFileNameWithPath = writer.generateFileName(flatFileNameWithPath, true);
			flatFileNameWithPath = writer.initialize(true, null, reportingFileLocation, flatFileNameWithPath, null, envVariable);
			System.out.println("Report File Name:" + flatFileNameWithPath);
		}
		else
		{
			writer.initialize(false, null, reportingFileLocation, reportingBean.getFileName(), null, envVariable);
		}
		
		ArrayList<IssuerReportingBean> issuerReportingBeanArrayList = reportingBean.getIssuerReportList();
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.write("<Report>");
		writer.write("<FlatFileSummary>");
		writer.write("<FlatFile>" + reportingBean.getFileName() + "</FlatFile>");
		writer.write("<NumberOfIssuers>" + reportingBean.getTotalIssuerCount() + "</NumberOfIssuers>");
		writer.write("<NumberOfHouseHold>" + reportingBean.getNumberOfHouseHolds() + "</NumberOfHouseHold>");
		writer.write("<NumberOfMembers>" + reportingBean.getTotalMemberCount() + "</NumberOfMembers>");
		writer.write("<NumberOfDependents>" + reportingBean.getTotalDependentCount() + "</NumberOfDependents>");
		writer.write("<NumberOfSubscriber>" + reportingBean.getTotalSubscriberCount() + "</NumberOfSubscriber>");
		writer.write("</FlatFileSummary>");
		writer.write("<TranslationSummary>");
		writer.write("<NumberOfIssuers>" + reportingBean.getTranslatedIssuerCount() + "</NumberOfIssuers>");
		writer.write("<NumberOfHouseHold>" + reportingBean.getTranslatedHouseHoldCount() + "</NumberOfHouseHold>");
		writer.write("<NumberOfMembers>" + reportingBean.getTranslatedMemberCount() + "</NumberOfMembers>");
		writer.write("<NumberOfDependents>" + reportingBean.getTranslatedDependentCount() + "</NumberOfDependents>");
		writer.write("<NumberOfSubscriber>" + reportingBean.getTranslatedSubscriberCount() + "</NumberOfSubscriber>");
		writer.write("</TranslationSummary>");
		writer.write("<IssuersReport>");
		
		Map<String, ArrayList<Exception>> errorMap = reportingBean.getException();
		if(!errorMap.isEmpty()) {
			writer.write("<FileLevelError>");
				Iterator<String> itr = errorMap.keySet().iterator();
				while(itr.hasNext()) {
					String key = itr.next();
					ArrayList<Exception> errorArrayList = errorMap.get(key);
					writer.write("<ErrorIdentifier HIOSID=\"" + key + "\">");		
					for(Exception error:errorArrayList) {
						writer.write("<ErrorMessage>" + error.toString() + "</ErrorMessage>");	
					}
					writer.write("</ErrorIdentifier>");
				}
				
			writer.write("</FileLevelError>");
		}
		
		int counter = 0;
		for(IssuerReportingBean issuerBean:issuerReportingBeanArrayList) {
			writer.write("<IssuerSummary>");
			writer.write("<Sequence>"+ (++counter) + "</Sequence>");
			writer.write("<HiosID>"+ issuerBean.getHIOSID() + "</HiosID>");
			
			
			writer.write("<TranslationFile>"+ issuerBean.getFileGenerated() + "</TranslationFile>");
			writer.write("<TranslationFileType>"+ issuerBean.getFileGeneratedType() + "</TranslationFileType>");
			writer.write("<NumberOfHouseHold>"+ issuerBean.getNumberOfHouseHolds() + "</NumberOfHouseHold>");
			writer.write("<NumberOfMembers>"+ issuerBean.getMemberCount() + "</NumberOfMembers>"); 
			writer.write("<NumberOfDependents>"+ issuerBean.getDependentCount() + "</NumberOfDependents>");	
			writer.write("<NumberOfSubscriber>"+ issuerBean.getSubscriberCount() + "</NumberOfSubscriber>");	
			
			Map<String, ArrayList<Exception>> errMap = issuerBean.getException();
			if(!errMap.isEmpty()) {
				writer.write("<IssuerLevelError>");
					Iterator<String> itr = errMap.keySet().iterator();
					while(itr.hasNext()) {
						String key = itr.next();
						ArrayList<Exception> errorArrayList = errMap.get(key);
						String[] keyArray = key.split("_");
						
						String policyID = "", batchID ="", recordID="";
						
						if(keyArray != null && keyArray.length >= 1) {
							policyID = keyArray[0];
							
							if( keyArray.length >= 2) {
								batchID = keyArray[1];
							}
							
							if( keyArray.length >= 3) {
								recordID = keyArray[2];
							}
						}
						
						writer.write("<ErrorIdentifier PolicyID=\"" + policyID + "\" BatchID=\""+ batchID + "\" RecordID=\"" + recordID + "\">");		
						for(Exception error:errorArrayList) {
							writer.write("<ErrorMessage>" + error.toString() + "</ErrorMessage>");	
						}
						writer.write("</ErrorIdentifier>");
					}
					
				writer.write("</IssuerLevelError>");
			}
			
			
			if(xmlGenerated) {
				/*File file = new File(issuerBean.getFileGeneratedLocation()+ "/" + issuerBean.getFileGenerated());
				writer.write("<XSDValidation>");
				boolean isValid = false;
				try {
					if(file.exists()) {
						isValid = val.validate(xsdLocation, issuerBean.getFileGeneratedLocation()+ "/" + issuerBean.getFileGenerated());
						writer.write("<Results>VALID</Results>");
					}
				} catch (IOException | SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					xsdValidationSucessful = false;
					
					writer.write("<Results>INVALID</Results>");
					writer.write("<XSDValidationError>");
					writer.write(e.getMessage());
					writer.write("</XSDValidationError>");
					
					
					File newFile = new File(errorFolder+"/" + issuerBean.getFileGenerated());
					
					int count = 0;
					
					boolean renamed = false;
					if(file.exists()) {
						System.out.println("Moving the file " + file.getAbsolutePath() + " to " +  newFile.getAbsolutePath());
						while(count < 100 && !(renamed = file.renameTo(newFile))) {
							System.gc();
							count++;
							try {
								Thread.sleep(50);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
							
						if(!renamed) {
							System.out.println("File " + file.getAbsolutePath() + " could not be moved to error folder.");
						}
					}
				}
				writer.write("</XSDValidation>");*/				
				
				boolean moveToError = false;
				try {
					xsdValidation(xsdLocation, issuerBean.getFileGenerated(), issuerBean.getFileGeneratedLocation(), writer, errorFolder);
				} catch(SAXException e) {
					moveToError = true;
				} catch(IOException e) {
					moveToError = true;
				}				
				
					
				if(moveToError) {
					boolean renamed = false;
					int count = 0;
					
					File file = new File(issuerBean.getFileGeneratedLocation() + issuerBean.getFileGenerated());	
					File newFile = new File(errorFolder +"/" + issuerBean.getFileGenerated());
					
					if(file.exists()) {
						System.out.println("Moving the file " + file.getAbsolutePath() + " to " +  newFile.getAbsolutePath());
						while(count < 100 && !(renamed = file.renameTo(newFile))) {
							System.gc();
							count++;
							try {
								Thread.sleep(50);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
			
						if(!renamed) {
							System.out.println("File " + file.getAbsolutePath() + " could not be moved to error folder.");
						}
					}
				}
			}				
			
			writer.write("</IssuerSummary>");
		}	
		writer.write("</IssuersReport>");
		writer.write("</Report>");
		
		writer.close();
		return xsdValidationSucessful;
	}
	
	public boolean  xsdValidation (String xsdForValidarion, String xmlFileToValidate, String xmlFileLocation, Writer writer, String errorFileLocation) throws IOException, SAXException {
		boolean xsdValidationSucessful = true;
		
		File file = new File(xmlFileLocation + xmlFileToValidate);		
		
		if(file.exists()) {
			writer.write("<XSDValidation>");
			
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	        Schema schema = factory.newSchema(new StreamSource(xsdForValidarion));
	        Validator validator = schema.newValidator();		        
	        final List<SAXParseException> exceptions = new LinkedList<SAXParseException>();
	        validator.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					exceptions.add(exception);
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					exceptions.add(exception);
				}
				
				@Override
				public void error(SAXParseException exception) throws SAXException {
					exceptions.add(exception);
				}
			});

			StreamSource xmlFile = new StreamSource(xmlFileLocation + xmlFileToValidate);
			
			try {
				validator.validate(xmlFile);
			} catch (SAXParseException e) {
				exceptions.add(e);
			} catch (SAXException e) {
				e.printStackTrace();
			}
			
  
			if(!exceptions.isEmpty()) {
				xsdValidationSucessful = false;
	  
			System.out.println("Validation failures..");
			writer.write("<Results>INVALID</Results>");
			  
			for(SAXParseException saxException:exceptions) {
				writer.write("<XSDValidationError>");
				writer.write("<Line>" + saxException.getLineNumber() + "</Line>");
				writer.write("<Column>" + saxException.getColumnNumber() + "</Column>");
				writer.write("<Error>" + saxException.getMessage() + "</Error>");
				writer.write("</XSDValidationError>");
				System.out.println(saxException.getMessage() + " at Line" + saxException.getLineNumber() + " column " + saxException.getColumnNumber());
			}
  
			File newFile = new File(errorFileLocation +"/" + xmlFileToValidate);
			
			int count = 0;
			
			boolean renamed = false;
			if(file.exists()) {
				System.out.println("Moving the file " + file.getAbsolutePath() + " to " +  newFile.getAbsolutePath());
				while(count < 100 && !(renamed = file.renameTo(newFile))) {
					System.gc();
					count++;
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
	
				if(!renamed) {
					System.out.println("File " + file.getAbsolutePath() + " could not be moved to error folder.");
				}
			}
		} else {
				  writer.write("<Results>VALID</Results>");
			}
			
			writer.write("</XSDValidation>");
		}
				
		return xsdValidationSucessful;
	}
	
	public Generator(String templateFileNameWithPath, String reportingFileLocation) {	
		this.templateFileNameWithPath = templateFileNameWithPath;
		this.reportingFileLocation = reportingFileLocation;
	}
	
	public Generator(String templateFileNameWithPath, String outputFileLocation, String reportingFileLocation) {	
		this.templateFileNameWithPath = templateFileNameWithPath;
		//this.outputFileLocation = outputFileLocation;
		this.reportingFileLocation = reportingFileLocation;
	}
	
	public String applyRules(String segment, String[] dataSet, int segmentType) {
		segment = clearSegmentsRule(segment, dataSet);
		if(segCountToBeCleaned == 0) {
			try{
				segment = specialRule(segment, dataSet);
				if(segment == null || segment.isEmpty()) {
					return null;		// wont be added to file
				}
				
				maintainSegmentCount(segmentType);
				
				segment = substituteRule(segment, dataSet);			
				segment = deriveRule(segment, dataSet);				
							
				segment = referenceRule(segment, dataSet);
				segment = sequenceRule(segment, dataSet);
				
				segment = cleanupTrailingElements(segment);
			} catch(RuntimeException e) {
				System.out.println("Error at Segment " + segment);
				throw e;
			}
			
			//write(segment);	
		} else {
			segCountToBeCleaned--;
			segment = null;
		}
		
		return segment;
	}
	
	private String cleanupTrailingElements(String segment) {
		int pos = segment.indexOf(":");
		
		if(pos == -1) {
			System.out.println("Template Segment " + segment + " doesn't have \":\" a expected location");
		}
		
		String elementUsageString = segment.substring(0, pos);
		segment = segment.substring(pos+1,segment.length());	
		
		String[] segArray = segment.split("\\*");
		boolean cleanUpDone = false;
		
		for(int i = segArray.length -1; i > 0;--i) {
			String val = segArray[i];
			//boolean required = (elementUsageString.charAt(i-1) == 'R')?true:false;
						
			if("~".equals(val)) {	// Last element empty
				//if(!required) {
					segArray[i-1] += "~";
					cleanUpDone = true;
				//}
			} else if(val.endsWith("~")) {// Last element has a value
				break;
			}
		}
		
		String returnSegment = segment;
		if(cleanUpDone) {
			returnSegment = "";
			
			for(String seg:segArray) {			
				if(!seg.contains("~")) {
					returnSegment += seg + "*";
				}
				else {
					returnSegment += seg;
					break;
				}
			}	
		} 
		
		return returnSegment;
	}
		
	
	private void maintainSegmentCount(int segmentType) {
		Integer val = null;
		switch(segmentType){
			case INTERCHANGE_ISA:
				groupCount = 0;
				break;
			case GROUP_GS:
				transactionCount = 0;
				transactionSegmentCount = 0;
				groupCount++;				
				break;
			case TRANSACTION_ST:			// Number of transaction	
				transactionSegmentCount = 0;
				transactionCount++;			// Gets added to segment count too
			case TRANSACTION:
			case TRANSACTION_SE:
				transactionSegmentCount++;
				break;				
		}
	}
	
	public void generate(InterchangeBean bean, String[] dataSet) {
		
	}
	
	public ArrayList<String> generateISA(InterchangeBean bean, String[] dataSet) {
		String data = applyRules(bean.getStartTag(), dataSet, INTERCHANGE_ISA);
		
		ArrayList<String> groupArrayList = bean.getSegmentListArray();
		ArrayList<String> segmentArraList = new ArrayList<String>();
		segmentArraList.add(data);
		for(String segment:groupArrayList) {
			int identifier = INTERCHANGE_ISA;
			segment = applyRules(segment, dataSet, INTERCHANGE_ISA);
			
			if(segment != null && segment.trim().length() > 0)
				segmentArraList.add(segment);			
		}	
		return segmentArraList;	
	}	
	
	public String generateIEA(InterchangeBean bean, String[] dataSet) {
		return applyRules(bean.getEndTag(), dataSet, INTERCHANGE_IEA);	
	}
	
	public ArrayList<String> generateGS(GroupBean bean, String[] dataSet) {
		String data = applyRules(bean.getStartTag(), dataSet, GROUP_GS);
		
		ArrayList<String> groupArrayList = bean.getSegmentListArray();
		ArrayList<String> segmentArraList = new ArrayList<String>();
		segmentArraList.add(data);
		for(String segment:groupArrayList) {
			int identifier = GROUP_GS;
			segment = applyRules(segment, dataSet, GROUP_GS);
			
			if(segment != null && segment.trim().length() > 0)
				segmentArraList.add(segment);			
		}	
		return segmentArraList;
		
		//return applyRules(bean.getStartTag(), dataSet, GROUP_GS);
	}
	
	public String generateGE(GroupBean bean, String[] dataSet) {
		return applyRules(bean.getEndTag(), dataSet, GROUP_GE);
	}
	
	public ArrayList<String> generateTransaction(TransactionBean bean, String[] dataSet) {
		String data = applyRules(bean.getStartTag(), dataSet, TRANSACTION_ST);
		
		ArrayList<String> transactionArrayList = bean.getSegmentListArray();
		ArrayList<String> segmentArraList = new ArrayList<String>();
		segmentArraList.add(data);
		for(String segment:transactionArrayList) {
			int identifier = TRANSACTION;
			segment = applyRules(segment, dataSet, TRANSACTION);
			
			if(segment != null && segment.trim().length() > 0)
				segmentArraList.add(segment);			
		}	
		return segmentArraList;
	}
	
	public String  generateSE(TransactionBean bean, String[] dataSet) {
		return applyRules(bean.getEndTag(), dataSet,TRANSACTION_SE);		
	}
	
	public ArrayList<String> generateRepeatSegment(TransactionBean bean, String[] dataSet) {
		ArrayList<String> memberArrayList = bean.getMemberListArray();		
		ArrayList<String> segmentArraList = new ArrayList<String>();
		for(String segment:memberArrayList) {
			segment = applyRules(segment, dataSet, TRANSACTION);
			
			if(segment != null && segment.trim().length() > 0)
				segmentArraList.add(segment);
		}
		
		resetMemberCounter();
		return segmentArraList;
	}
	
	
	/*
	 * Replaces the values in the file
	 */
	public String substituteRule(String segment, String[] dataSet) {
		Matcher ruleMatcher = substituteRulePattern.matcher(segment);
		String returnString = segment;
		
		

		while (ruleMatcher.find()) {
			String ruleString = segment.substring(ruleMatcher.start(),
					ruleMatcher.end());
			String removeDelimiter = getParameterValue(removeDelimiterPattern, ruleString);
			String insertDelimiter = getParameterValue(insertDelimiterPattern, ruleString);

			String column = getParameterValue(columnPattern, ruleString);
						
			String colValue = dataSet[Integer.parseInt(column)-1].trim();
			
			if(colValue != null) {
				//derivedVal = derivedVal.trim();
				
				if(removeDelimiter != null) {
					SpecialRules rules = new SpecialRules();
					colValue = rules.removeDelimiterFromValue(colValue, removeDelimiter);					
				}
				
				if(insertDelimiter != null) {
					colValue = insertDelimiter(colValue, insertDelimiter);
				}
			}			
			
			returnString = returnString.replace(ruleString, colValue);
		}	

		return returnString;		
	}
	
	public String deriveRule(String segment, String[] dataSet) {		
		String returnString = segment;
		Matcher ruleMatcher = deriveRulePattern.matcher(segment);
		while (ruleMatcher.find()) {
			String ruleString = segment.substring(ruleMatcher.start(),
					ruleMatcher.end());

			String column = getParameterValue(columnPattern, ruleString);
			String map = getParameterValue(mapPattern, ruleString);
			String removeDelimiter = getParameterValue(removeDelimiterPattern, ruleString);
			String insertDelimiter = getParameterValue(insertDelimiterPattern, ruleString);
			
			String[] colArray = column.split(VALUE_SEPERATOR);
			String derivedVal = "";			
			
			//System.out.println(segment);
			String keys = map.substring(map.indexOf(MAP_KEY_VALUE_START_SEPERATOR)+1, map.indexOf(MAP_KEY_VALUE_END_SEPERATOR));
			String values = map.substring(map.indexOf(MAP_KEY_VALUE_END_SEPERATOR)+2, map.lastIndexOf(MAP_KEY_VALUE_END_SEPERATOR));
			for(String colIndex:colArray) {				
				String[] keyArray = keys.split(VALUE_SEPERATOR);
				String[] valueArray = splitString(values, keyArray.length);
				
				boolean match = false;
				for(int i= 0; i < keyArray.length;++i) {
					String colValue = dataSet[Integer.parseInt(colIndex)-1];
					if(colValue != null) {
						colValue = colValue.trim();
					}
					
					String key = keyArray[i];
					String value = valueArray[i];
					
					// Ignores the case for derivaion
					if(key.equalsIgnoreCase(colValue)) {
						derivedVal = valueArray[i];
						match = true;
						break;
					} else if(VALUE_PRESENT.equals(key)) {	
						if((colValue != null && !colValue.isEmpty())) {
							derivedVal = value;
							match = true;
							break;
						}									
					} else if(VALUE_ABSENT.equals(key)) {
						if((colValue == null || colValue.isEmpty())) {
							derivedVal = value;	
							match = true;
							break;
						}  		
					}
				}
				
				if(match) {
					if(derivedVal.contains(MAP_STRING_START)) {
						map = getParameterValue(mapPattern, derivedVal);
						keys = map.substring(map.indexOf(MAP_KEY_VALUE_START_SEPERATOR)+1, map.indexOf(MAP_KEY_VALUE_END_SEPERATOR));
						values = map.substring(map.indexOf(MAP_KEY_VALUE_END_SEPERATOR)+2, map.lastIndexOf(MAP_KEY_VALUE_END_SEPERATOR));
						derivedVal = "";
					}else if(derivedVal.contains(COLUMN_VALUE)) {
						derivedVal = dataSet[Integer.parseInt(colIndex)-1].trim();
						break;
					}else if(derivedVal.length() > 0) {
						break;
					}
				} else {
					break;
				}
			}		
			
			
			if(derivedVal != null) {
				//derivedVal = derivedVal.trim();
				
				if(removeDelimiter != null) {
					SpecialRules rules = new SpecialRules();
					derivedVal = rules.removeDelimiterFromValue(derivedVal, removeDelimiter);					
				}
				
				if(insertDelimiter != null) {
					derivedVal = insertDelimiter(derivedVal, insertDelimiter);
				}
			}			
			
			returnString = returnString.replace(ruleString, derivedVal);	
		}

		return returnString;		
	}	
	
	public String specialRule(String segment, String[] dataSet) {
		Matcher ruleMatcher = specialRulePattern.matcher(segment);
		String returnString = segment;

		while (ruleMatcher.find()) {
			String ruleString = segment.substring(ruleMatcher.start(),
					ruleMatcher.end());

			String column = getParameterValue(columnPattern, ruleString);
			String name = getParameterValue(namePattern, ruleString);
			String delimiter = getParameterValue(delimiterPattern, ruleString);
			String format = getParameterValue(formatPattern, ruleString);
			
			String generatedValue = "";
			String colValue = null;
			if(column != null) {
				colValue = dataSet[Integer.parseInt(column)-1];
				
				if(colValue != null)
					colValue = colValue.trim();
			}			
			
			if(SpecialRules.ISA06_RECEIVER_ID.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateISA06ReceiverID(colValue);
			} 
			else if(SpecialRules.ISA09_CURRENT_DATE.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateISA09CurrentDate();
			} else if(SpecialRules.ISA10_CURRENT_TIME.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateISA10CurrentTime();
			} else if(SpecialRules.GS02_TRIM_QHP_14_LENGTH.equals(name)) {
				
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.trimQHPTo14Length(colValue);		
			
			} else if(SpecialRules.GS04_CURRENT_DATE.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateGS04CurrentDate();
			} else if(SpecialRules.GS05_CURRENT_TIME.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateGS05CurrentTime();
			} else if(SpecialRules.INS02_DERIVE_RELATIONSHIP_CODE.equals(name)) {
				SpecialRules rules = new SpecialRules();
				String subscriberIndicator = dataSet[IndexConstants.SUBSCRIBER_INDICATOR_INDEX-1].trim();	
				boolean isSubscriber = (ConfigurationConstants.SUBSCRIBER_INDICATOR_Y.equalsIgnoreCase(subscriberIndicator)) ? true: false;
					
				generatedValue = rules.deriveRelationshipCode(colValue, isSubscriber);
			} else if(SpecialRules.SE01_NUMBER_OF_TOTAL_SEGMENTS_IN_TRANSACTION.equals(name)) {
				generatedValue = Integer.toString(transactionSegmentCount + 1);		// Count will be one less for transaction as special rule now rule before the count
				//transactionSegmentCount = 0;
			} else if(SpecialRules.GE01_NUMBER_OF_TOTAL_TRANSACTIONS_IN_GROUP.equals(name)) {
				generatedValue = Integer.toString(transactionCount);
			} else if(SpecialRules.IEA01_NUMBER_OF_TOTAL_GROUPS_IN_INTERCHANGE.equals(name)) {
				generatedValue = Integer.toString(groupCount);
			} else if(SpecialRules.REMOVE_DELIMITER_FROM_VALUE.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.removeDelimiterFromValue(colValue, delimiter);
			} else if(SpecialRules.QTY02_MEMBER_TOTAL.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = Integer.toString(memberCountWithinST);
			} else if(SpecialRules.QTY02_DEPENDENT_TOTAL.equals(name)) {
				SpecialRules rules = new SpecialRules();
				
				if(dependentCountWithinST > 0) {
					generatedValue = Integer.toString(dependentCountWithinST);
				} else {
					return "";		// would be removed.
				}				
			} else if(SpecialRules.CURRENT_DATE_WITH_PROVIDED_FORMAT.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateDate(format);
			} else if(SpecialRules.CURRENT_TIME_WITH_PROVIDED_FORMAT.equals(name)) {
				SpecialRules rules = new SpecialRules();
				generatedValue = rules.generateTime(format);
			} else if(SpecialRules.INTERCHANGE_CONTROL_NUMBER_INCREMENT.equals(name)) {
				SpecialRules rules = new SpecialRules();
				if(null!=dataSet){
					generatedValue = rules.generateISAControlNumber(dataSet[IndexConstantsBatching.ISSUER_ID_INDEX], isaControlNumberFile);
					interchangeControlNumber.put(SpecialRules.INTERCHANGE_CONTROL_NUMBER_INCREMENT, generatedValue);
				}
			}
			else if(SpecialRules.GROUP_CONTROL_NUMBER_INCREMENT.equals(name)) {
				SpecialRules rules = new SpecialRules();
				if(null!=dataSet){
					generatedValue = rules.generateGSControlNumber(dataSet[IndexConstantsBatching.ISSUER_ID_INDEX], gsControlNumberFile);
					groupControlNumber.put(SpecialRules.GROUP_CONTROL_NUMBER_INCREMENT, generatedValue);
				}
			}
			else if(SpecialRules.BATCH_NUMBER_OF_TOTAL_TRANSACTIONS_IN_GROUP.equals(name)) {
				generatedValue = String.valueOf(BatchGenerator.transactionCount);
				//generatedValue = Integer.toString(transactionCount);
			 }
			else if(SpecialRules.DERIVE_CONFIRMATION_CODE.equals(name)) {
				SpecialRules rules = new SpecialRules();
				if(null!=dataSet){
					generatedValue = rules.populateConfirmationCode(dataSet);
				if(null!=generatedValue && "".equals(generatedValue))
				{
					return "";		// would be removed.
				} 
					
				}
			 }
			returnString = returnString.replace(ruleString, generatedValue);	
		}

		return returnString;		
	}
	
	
	
	public String clearSegmentsRule(String segment, String[] dataSet) {
		Matcher ruleMatcher = removeSegmentPattern.matcher(segment);
		String returnString = segment;
		
		while (ruleMatcher.find()) {
			String ruleString = segment.substring(ruleMatcher.start(),
					ruleMatcher.end());

			String column = getParameterValue(columnPattern, ruleString);		// Could be multiple columns
			String removeStart = getParameterValue(removeStartPattern, ruleString);
			String removeEnd = getParameterValue(removeEndPattern, ruleString);
			String map = getParameterValue(mapPattern, ruleString);
			
			if(removeStart == null) {
				removeStart = "0";
			}
			
			if(removeEnd == null) {
				removeEnd = "0";
			}
			
			boolean isToBeRemoved = false;
			if(map != null) {
				String keys = map.substring(map.indexOf(MAP_KEY_VALUE_START_SEPERATOR)+1, map.indexOf(MAP_KEY_VALUE_END_SEPERATOR));
				String values = map.substring(map.indexOf(MAP_KEY_VALUE_END_SEPERATOR)+2, map.lastIndexOf(MAP_KEY_VALUE_END_SEPERATOR));
				
				String[] colArray = column.split(VALUE_SEPERATOR);
				String derivedVal = "";
				
				for(String colIndex:colArray) {				
					String[] keyArray = keys.split(VALUE_SEPERATOR);
					String[] valueArray = splitString(values, keyArray.length);
						
					boolean match = false;
					for(int i= 0; i < keyArray.length;++i) {
						String colValue = dataSet[Integer.parseInt(colIndex)-1];
						if(colValue != null) {
							colValue = colValue.trim();
						}
						
						String key = keyArray[i];
						String value = valueArray[i];
											
						if(key.equals(colValue)) {
							derivedVal = valueArray[i];
							match = true;
							break;
						} else if(VALUE_PRESENT.equals(key)) {	
							if((colValue != null && !colValue.isEmpty())) {
								derivedVal = value;
								match = true;
								break;
							}									
						} else if(VALUE_ABSENT.equals(key)) {
							if((colValue == null || colValue.isEmpty())) {
								derivedVal = value;	
								match = true;
								break;
							}  		
						}
					}
					
					if(match) {
						if(derivedVal.contains(MAP_STRING_START)) {
							map = getParameterValue(mapPattern, derivedVal);
							keys = map.substring(map.indexOf(MAP_KEY_VALUE_START_SEPERATOR)+1, map.indexOf(MAP_KEY_VALUE_END_SEPERATOR));
							values = map.substring(map.indexOf(MAP_KEY_VALUE_END_SEPERATOR)+2, map.lastIndexOf(MAP_KEY_VALUE_END_SEPERATOR));
							derivedVal = "";						
						}else if(derivedVal.contains(VALUE_REMOVE)) {
							isToBeRemoved = true;
							break;
						}else if(derivedVal.length() > 0) {
							break;
						}
					} else {
						break;
					}
				}		
			} else {
				
			}
			
			if(isToBeRemoved)
				segCountToBeCleaned = (Integer.parseInt(removeEnd) - Integer.parseInt(removeStart)) +1;
			else {
				segCountToBeCleaned = 0;
				returnString = returnString.replace(ruleString, "");
			}
		}		
		
		return returnString;
	}	
	
	public String referenceRule(String segment, String[] dataSet) {
		Matcher ruleMatcher = referenceRulePattern.matcher(segment);
		String returnString = segment;

		while (ruleMatcher.find()) {
			String ruleString = segment.substring(ruleMatcher.start(),
					ruleMatcher.end());

			String scope = getParameterValue(scopePattern, ruleString);
			String name = getParameterValue(namePattern, ruleString);
			
			String referncedValue = null;			
			if(SCOPE_ISA.equals(scope)) {
				if(SpecialRules.INTERCHANGE_CONTROL_NUMBER_INCREMENT.equals(name)){
					referncedValue = interchangeControlNumber.get(name);
				}
				else
				{
					referncedValue = interchangeSequenceMap.get(name);
				}
			} else if(SCOPE_GS.equals(scope)) {
				if(SpecialRules.GROUP_CONTROL_NUMBER_INCREMENT.equals(name)){
					referncedValue = groupControlNumber.get(name);
				}
				else
				{
					referncedValue = groupSequenceMap.get(name);
				}
			} else if(SCOPE_ST.equals(scope)) {
				referncedValue = transactionSequenceMap.get(name);
			} else if(SCOPE_MEMBER.equals(scope)) {
				referncedValue = memberSequenceMap.get(name);
			}
				
			if(referncedValue == null)
				referncedValue = "";
			
			returnString = returnString.replace(ruleString, referncedValue.trim());	
		}
		
		return returnString;	
	}
	
	public String sequenceRule(String segment, String[] dataSet) {
		Matcher ruleMatcher = sequenceRulePattern.matcher(segment);
		String returnString = segment;

		while (ruleMatcher.find()) {
			String ruleString = segment.substring(ruleMatcher.start(),
					ruleMatcher.end());

			String minLen = getParameterValue(minLengthPattern, ruleString);
			String maxLen = getParameterValue(maxLengthPattern, ruleString);
			String scope = getParameterValue(scopePattern, ruleString);
			String startValue = getParameterValue(StartValuePattern, ruleString);
			String name = getParameterValue(namePattern, ruleString);
			
			String generatedSequence = null;
			
			Map<String, String> map = null;
			if(SCOPE_ISA.equals(scope)) {
				generatedSequence = interchangeSequenceMap.get(name);
				map = interchangeSequenceMap;
			} else if(SCOPE_GS.equals(scope)) {
				generatedSequence = groupSequenceMap.get(name);
				map = groupSequenceMap;
			} else if(SCOPE_ST.equals(scope)) {
				generatedSequence = transactionSequenceMap.get(name);
				map = transactionSequenceMap;
			} else if(SCOPE_MEMBER.equals(scope)) {
				generatedSequence = memberSequenceMap.get(name);
				map = memberSequenceMap;
			}
			
			if(generatedSequence == null) {
				generatedSequence = startValue;
			} else {
				long seq = Long.parseLong(generatedSequence) + 1;
				generatedSequence = Long.toString(seq);
			}
			
			int len = generatedSequence.length();
			int min = Integer.parseInt(minLen);
			int max = Integer.parseInt(maxLen);
			
			if(len < min) {
				for(int i =0; i < min-len; ++i) {
					generatedSequence = "0"+generatedSequence;
				}
			} else if(len > max) {
				generatedSequence = generatedSequence.substring(len-max, len);
			}					
			
			map.put(name, generatedSequence);
			
			returnString = returnString.replace(ruleString, generatedSequence);	
		}
		
		return returnString;	
	}	
			
	public String setOuputFileLocation(boolean generateDateTime, String outputFileName, String outputFileLocation, String fileIdentifier, String fileExtension, String envVariable) throws IOException {
		return writer.initialize(generateDateTime, outputFileName, outputFileLocation, fileIdentifier, fileExtension, envVariable);
	}
	
	protected void write(String segment) {
		writer.write(segment);		
	}
	
	private String getParameterValue(Pattern pattern, String inputString) {
		Matcher match = pattern.matcher(inputString);
		
		String value = null;
		
		if(match.find()) {
			String matchString = inputString.substring(match.start(), match.end());
			
			value = matchString.substring(matchString.indexOf("=")+1);
		}
		
		return value;
	}
	
	private String[] splitString(String str, int count) {
		String arr[] = new String[count];
		
		for(int i = 0; i < count; ++i) {
			if(str.startsWith(MAP_STRING_START+MAP_KEY_VALUE_START_SEPERATOR)) {
				int depth = 0;
				boolean keyFound = false;
				for(int j = 0; j < str.length(); ++j) {
					char ch = str.charAt(j);
					
					if(ch == '>' && !keyFound) {
						keyFound = true;
					} else if(keyFound && ch == '<') {
						depth++;
					} else if(keyFound && ch == '>') {
						depth--;
						
						if(depth == 0){
							arr[i] = str.substring(0,j+1);
							str = str.substring(j+1, str.length());
							
							if(str.startsWith(",")) {
								str = str.substring(1, str.length());
							}
							
							keyFound = false;
							break;
						}
					}
				}
			} else {
				int pos = str.indexOf(",");
				
				if(pos != -1) {
					arr[i] = str.substring(0, pos);
					str = str.substring(pos+1, str.length());
				} else {
					arr[i] = str;
					str = "";
				}
			}
		}
		
		return arr;
	}
	
	public String insertDelimiter(String value, String parameters) {
		String derivedVal = value;
		
		parameters = parameters.replace("[", "");
		String[] ruleParameterArray = parameters.split("]");
		
		SpecialRules rules = new SpecialRules();
		for(String ruleParam: ruleParameterArray) {
			char delimiter = ruleParam.charAt(0);
			String positions = ruleParam.substring(1, ruleParam.length());
			positions = positions.replace("(", "");
			positions = positions.replace(")", "");
			
			String[] indexArray = positions.split(",");
		
			derivedVal = rules.insertDelimitersToValue(derivedVal, delimiter, indexArray);			
		}
		
		return derivedVal;
	}
	
	public void resetInterchangeCounter() {
		interchangeSequenceMap.clear();
	}
	
	public void resetGroupCounter() {
		groupSequenceMap.clear();
	}
	
	public void resetTransactionCounter() {
		transactionSequenceMap.clear();
	}
	
	public void resetMemberCounter() {
		memberSequenceMap.clear();
	}
}
