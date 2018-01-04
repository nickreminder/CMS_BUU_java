package com.edi.translation.generation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edi.sort.bean.DataLocation;
import com.edi.sort.bean.FileSort;
import com.edi.translation.bean.GroupBean;
import com.edi.translation.bean.InterchangeBean;
import com.edi.translation.bean.TransactionBean;
import com.edi.translation.constants.ConfigurationConstants;
import com.edi.translation.constants.IndexConstantsBatching;
import com.edi.translation.reader.RandomFileReader;
import com.edi.translation.reader.TemplateReader;
import com.edi.translation.reporting.FileReportingBean;
import com.edi.translation.reporting.IssuerReportingBean;

public class BatchGenerator extends Generator{
	
	private final static Logger LOGGER = Logger.getLogger("BatchGenerator");
	private Map<String, String> xmlErrorMessage = new HashMap<String, String>();
	static int  transactionCount = 0;
	String hostName;
	String logFolder;
	String x12FileName;
	public BatchGenerator(String templateFileNameWithPath, String reportingFileLocation, String logFolder) {	
		super(templateFileNameWithPath, reportingFileLocation);
		this.logFolder = logFolder;
		try{
		File outputDirectory = new File(logFolder);
		LOGGER.setLevel(Level.ALL);
		Handler handler = new FileHandler(outputDirectory+"/Aggragator.log",10000000,100,true);//10MB file up to 20 files and append the log.
		LOGGER.setLevel(Level.ALL);
		LOGGER.addHandler(handler);
		handler.setFormatter(new Formatter() {
		      public String format(LogRecord record) {
		        return record.getLevel() + "  :  "
		            + record.getSourceClassName() + " -:- "
		            + record.getSourceMethodName() + " -:- "
		            + record.getMessage() + "\n";
		      }
		    });
		}
		catch(Exception ex)
		{
			System.err.print("Aggregator log folder error" + ex);
		}
	}
	
	public BatchGenerator(String templateFileNameWithPath, String outputFileLocation, String reportingFileLocation, String logFolder) {	
		super(templateFileNameWithPath, outputFileLocation, reportingFileLocation);
		this.logFolder = logFolder;
	}
		
	public static void main(String args[]) {
		long start = Calendar.getInstance().getTimeInMillis();
		
		String templateFileNameWithPath = null, outputFileLocation = null, flatFileNameWithPath = null, 
				reportingFileLocation = null, fileTypeToGenerate = null, envVariable= null, xsdLocation = null, errorFolder = null;
		
		String receiverOfTranslatedFile = null, functionCodeForTranslatedFile = null,  receiverOfReport = null, functionCodeForReport = null; 
				
		if(args.length < 5) {
			System.out.println("Need to specify the following files in the sequence: ");
			System.out.println("1. Template file name with path");
			System.out.println("2. X12 Output folder path");
			System.out.println("3. Flat file name with path");
			System.out.println("4. Reporting file location");
			System.out.println("5. File Type to generate( Avaliable options \"BATCH\", \"XML\", \"X12\".)");	
			System.out.println("6. Environment variable( Avaliable options \"T\", \"P\".)");
			System.out.println("7. XSD for the XML(This is required if generating XML)");
			System.out.println("8. Error folder for the files failing XSD validation(This is required if generating XML)");
			System.out.println("9. Receiver ID for translated file(This is required if generating XML)");
			System.out.println("10. Translated file Function Code(This is required if generating XML)");
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
			if(fileTypeToGenerate.equals(FILE_TYPE_BATCH)) {
				xsdLocation = args[6]; // Host name for batch
				errorFolder = args[7]; // Error location for batch
				receiverOfTranslatedFile = args[8]; //ISA  property file with location
				functionCodeForTranslatedFile = args[9];//GS  property file with location
			}
		}
		
		
		boolean fullyTranslated = true;
		BatchGenerator generator = new BatchGenerator(templateFileNameWithPath, reportingFileLocation,"");
		try {
			fullyTranslated = generator.generateHipaaDataGroupedByGS(flatFileNameWithPath, fileTypeToGenerate, envVariable, xsdLocation, errorFolder, outputFileLocation, receiverOfTranslatedFile,
			functionCodeForTranslatedFile, receiverOfReport, functionCodeForReport);			
			
		}  catch (Exception e) {			
			e.printStackTrace();
			System.exit(3);
		}
		
		//System.out.println("Time Taken(s): " + (Calendar.getInstance().getTimeInMillis() - start));	
		
		if(!fullyTranslated) {
			System.exit(2);
		} else
			System.exit(0);
	}
	
	
	public Map<String, Map<String, Map<String, ArrayList<DataLocation>>>> sort(RandomFileReader flatFileReader, boolean isXMLFileGenerated) throws Exception {
		long start = Calendar.getInstance().getTimeInMillis();
		Map<String, Map<String, Map<String, ArrayList<DataLocation>>>> fileSortMap = new LinkedHashMap<String, Map<String, Map<String, ArrayList<DataLocation>>>>();
		
		//System.out.println("Start Sorting...");
		
		boolean firstLineSkipped = false;
		String prevHiosID = null;
		String prevQhpID = null;
		String prevMemberID = null;
		
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
			
			String currentHiosID = lineArray[IndexConstantsBatching.HIOS_ID_INDEX-1].trim();			
			String currentMemberID = lineArray[IndexConstantsBatching.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1].trim();		
			String currentQhpID = "";
			
			if(!isXMLFileGenerated) {
				System.out.println("RAJJJJJ:"+lineArray[IndexConstantsBatching.QHP_IENTIFIER_INDEX-1]);
				currentQhpID = lineArray[IndexConstantsBatching.QHP_IENTIFIER_INDEX-1].trim().substring(0,14);
			}			
			
			Map<String, Map<String, ArrayList<DataLocation>>> hiosMap = fileSortMap.get(currentHiosID);		
			if(hiosMap == null) {
				hiosMap = new LinkedHashMap<String, Map<String, ArrayList<DataLocation>>>();
				fileSortMap.put(currentHiosID, hiosMap);	
			}
			
			Map<String, ArrayList<DataLocation>> qhpMap = hiosMap.get(currentQhpID);
			if(qhpMap == null) {
				qhpMap = new LinkedHashMap<String, ArrayList<DataLocation>>();
				hiosMap.put(currentQhpID, qhpMap);
			}
			
			ArrayList<DataLocation> memberIndexArrayList = qhpMap.get(currentMemberID);
			
			DataLocation loc = new DataLocation();
			loc.setStartLocation(flatFileReader.getCurrentSegmentLocation());
			loc.setNumberOfLines(1);
							
			loc.setDate(Long.parseLong(lineArray[IndexConstantsBatching.XML_DATE_INDEX-1]));
			String time = lineArray[IndexConstantsBatching.XML_TIME_INDEX-1];
			if(time!=null && !time.matches("\\d+"))
	    	{
	    		time = time.substring(0, 6);
	    	}
			loc.setTime(Long.parseLong(time));
			
			if(memberIndexArrayList == null) {
				memberIndexArrayList = new ArrayList<DataLocation>();
				memberIndexArrayList.add(loc);
				qhpMap.put(currentMemberID, memberIndexArrayList);
			}			
			else
			{
				memberIndexArrayList.add(loc);
			//	Collections.sort(memberIndexArrayList, new DataLocation.SortByDateTime());
			}
				
				
			//	System.out.println("Member id: " + currentMemberID + "  Value: " + memberIndexArrayList);
				
				
//				boolean added = false;
				
//				for (int i = memberIndexArrayList.size() - 1; i >= 0; i--) {
//			          DataLocation prevLoc = (DataLocation)memberIndexArrayList.get(i);
//			          if (prevLoc.getDate() <= loc.getDate())
//			          {
//			            if (prevLoc.getDate() < loc.getDate()) {
//			              memberIndexArrayList.add(i + 1, loc);
//			              added = true;
//			              break;
//			            }
//			            if (prevLoc.getTime() <= loc.getTime())
//			            {
//			              memberIndexArrayList.add(i + 1, loc);
//			              added = true;
//			              break;
//			            }
//			          }
//			        }
//				
//				if(!added) {
//					memberIndexArrayList.add(loc);
//					added =true;
//				}
			
			prevHiosID = currentHiosID;
			prevQhpID = currentQhpID;
			prevMemberID = currentMemberID;
			
			flatFileReader.linePointerIncrement();	
		}
	//	System.out.println("Time Taken(s) for sorting: " + (Calendar.getInstance().getTimeInMillis() - start));
	//	System.out.println(fileSortMap);
//		Iterator<String> itr = fileSortMap.keySet().iterator();	
//		while(itr.hasNext()) {
//			String hiosID = itr.next();
//			System.out.println("-" + hiosID);
//			Map<String, Map<String, ArrayList<DataLocation>>> qhpMap = fileSortMap.get(hiosID);
//			Iterator<String> itrQhp = qhpMap.keySet().iterator();
//			
//			while(itrQhp.hasNext()) {		// START QHP 
//				String qhpID = itrQhp.next();
//				System.out.println("--" + qhpID);
//				Map<String, ArrayList<DataLocation>> memberMap = qhpMap.get(qhpID);	
//				SortedSet<ArrayList<DataLocation>> values = new TreeSet<ArrayList<DataLocation>>(memberMap.values());
//				System.out.println("VALUES:" + values);
//				Iterator<String> itrMemberID = memberMap.keySet().iterator();
//									
//				while(itrMemberID.hasNext()) {		// START Member ID						
//					String memberID = itrMemberID.next();
//					System.out.println("---" + memberID);
//					ArrayList<DataLocation> lineLocationArrayList = memberMap.get(memberID);
//					System.out.println("----" + lineLocationArrayList);
//					}
//			}
//		}
		return fileSortMap;
	}	
	
	public boolean generateHipaaDataGroupedByGS(String flatFileNameWithPath, String fileTypeToGenerate, String envVariable, String xsdLocation, 
			String errorFolder, String outputFileLocation, 
			String isaControlNumberFile, String gsControlNumberFile, String receiverOfReport, String functionCodeForReport) {	
		long start = Calendar.getInstance().getTimeInMillis();
		LOGGER.log(Level.INFO, flatFileNameWithPath +" start time: "+ new Date(start));
		transactionCount = 0;
		boolean isXMLFileGenerated = (fileTypeToGenerate.equals(FILE_TYPE_XML)) ? true:false;
		hostName = xsdLocation;
		//Generator generator = new Generator();	
		RandomFileReader flatFileReader = null;
		TemplateReader template = null;
		Map<String, Map<String, Map<String, ArrayList<DataLocation>>>> fileSortMap = null;
		boolean fullyTranslated = true;
		try
		{
			this.isaControlNumberFile = isaControlNumberFile;
			this.gsControlNumberFile = gsControlNumberFile;
			
			//Flat File content sorting start
			BufferedReader reader = new BufferedReader(new FileReader(flatFileNameWithPath));
		    List<FileSort> sortedFileList=new ArrayList<FileSort>();
		    reader.readLine();
		    String line="";
		    while((line=reader.readLine())!=null){
		    	 sortedFileList.add(getField(line));
		    }
		    reader.close();
		    Collections.sort(sortedFileList, new FileSort.SortByDateTime());
	        FileWriter tempWriter = new FileWriter(flatFileNameWithPath);
	        StringBuilder fileContent = new StringBuilder();
	        fileContent
			.append("MARKET|ADDITINONAL_MNT_CODE|ISSUER_ID|GROUP_RECEIVER_ID|EXCHANGE_ASSIGNED_POLICY_NUMBER|INSURANCE_LINE_OF_BUSINESS|EXCHNAGE_ASSIGNED_SUBSCRIBER_ID|SUBSCRIBER_INDICATOR|DATE|TIME|SENDER ID|XML_DATE|XML_TIME|CONTROL_NUMBER|FILE_NAME|X12_CONTENT\n");
	        for(FileSort val : sortedFileList){
	        	fileContent.append(val.toString() + "\n");
	        	
	        }
	        tempWriter.write(fileContent.toString());
	        tempWriter.close();
			//END
		    
			flatFileReader = new RandomFileReader(flatFileNameWithPath, isXMLFileGenerated);
			
			template = new TemplateReader(templateFileNameWithPath);
			//flatFileReader.setFileToRead(flatFileNameWithPath);
			
			fileSortMap = sort(flatFileReader, isXMLFileGenerated);
				
		FileReportingBean reportingBean = new FileReportingBean();
		
		String fileName = "";
		int index = flatFileNameWithPath.lastIndexOf("/");
		if(index!=-1)
		{
			fileName = flatFileNameWithPath.substring(index+1, flatFileNameWithPath.length());
		}
		index = fileName.lastIndexOf(".");
		if(index!=-1 && !fileName.equals(""))
		{
			fileName = fileName.substring(0, index);
		}
		reportingBean.setFileName(fileName+"."+hostName+".report");		

		
		
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
								if(memberArray.length >= IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX) {
									subscriberIndicator = memberArray[IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX-1].trim();
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
									if(memberArray.length >= IndexConstantsBatching.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX)
										policyNumber = memberArray[IndexConstantsBatching.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1];
									
									//if(memberArray.length >= IndexConstantsBatching.BATCH_ID_INDEX)
									//	batchID = memberArray[IndexConstantsBatching.BATCH_ID_INDEX-1];
									
									//if(memberArray.length >= IndexConstantsBatching.RECORD_ID_INDEX)
									//	recordID = memberArray[IndexConstantsBatching.RECORD_ID_INDEX-1];
									
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
											isaControlNumberFile, gsControlNumberFile);
																			
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
									populateXMLError(e, flatFileNameWithPath, xsdLocation);
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
					transactionCount=0;
				}// end QHP
			
			}catch(Exception e) {
				e.printStackTrace();
				
				issuerReportingBean.setException(policyNumber+ "_" + batchID + "_" + recordID, e);
				fullyTranslated = false;
				populateXMLError(e, flatFileNameWithPath, xsdLocation);
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
		
		//System.out.println("Generating report");
		boolean xsdValidationSuccessful = false;
		try
		{
			xsdValidationSuccessful = generateReport(reportingBean, envVariable, isXMLFileGenerated, xsdLocation, errorFolder, receiverOfReport, functionCodeForReport, flatFileNameWithPath);	
		}
		catch(Exception ex)
		{
			populateXMLError(ex, flatFileNameWithPath, xsdLocation);
		}
		
		if(fullyTranslated)
		{
			fullyTranslated = xsdValidationSuccessful;
			Path source = Paths.get(outputFileLocation + "/" + x12FileName);
			String orgFile = x12FileName.substring(12, x12FileName.length()-1);
		    Path target = Paths.get(outputFileLocation + "/" + orgFile);
		    //System.out.println("Final file name:" + orgFile);
		    Files.move(source, target);
		}
			
		
		
		if(xmlErrorMessage.size()>0)
		{
			generateXMlReport(errorFolder, xmlErrorMessage);
		}
		}
		catch(Exception ex)
		{
			populateXMLError(ex, flatFileNameWithPath, xsdLocation);
			ex.printStackTrace();
			System.exit(0);
		}
		long lEndTime = new Date().getTime();
		long difference = lEndTime - start;
		
		LOGGER.log(Level.INFO, "Total processed time: " + difference + " ms");
		LOGGER.log(Level.INFO, flatFileNameWithPath +" end time: "+ new Date(lEndTime));
		return fullyTranslated;
		//System.out.println("-------------------------------------------------" + count + "|" + dataSetArrayList.get(0)[IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX]);
	}
	
	public boolean writeSTData(ArrayList<String> isaArrayList, ArrayList<String> gsArrayList, boolean fileInitilazied, ArrayList<String[]> dataSetArrayList, 
			TemplateReader template, String flatFileNameWithPath, String fileTypeToGenerate, FileReportingBean reportingBean, 
			IssuerReportingBean issuerReportingBean, int hiosSplitCounter, String envVariable, String outputFileLocation, String receiverOfTranslatedFile,
			String functionCodeForTranslatedFile) throws Exception {
		
		//reportingBean.setNumberOfHouseHolds(1);
		
		memberCountWithinST = dependentCountWithinST = 0;
		int subscriberCount = 0;				
		
		for(String[] data:dataSetArrayList) {
			String subscriberIndicator = "";
			if(data.length >= IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX) {
				subscriberIndicator = data[IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX-1].trim();
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
	
		String exchangeAssignedPolicyNumber = dataSetArrayList.get(0)[IndexConstantsBatching.EXCHANGE_ASSIGNED_POLICY_NUMBER_INDEX-1].trim();
		
		
		ArrayList<String> transactionSegmentData = generateTransaction((TransactionBean)template.getDataBean(template.TRANSACTION_BEAN), dataSetArrayList.get(0));
				
		ArrayList<String> transactionLoopData = new ArrayList<String>();
		for(String[] segmentArrayList:dataSetArrayList) {
			transactionCount = transactionCount + getSTCount(segmentArrayList);
			transactionLoopData.addAll(generateRepeatSegment((TransactionBean)template.getDataBean(template.TRANSACTION_BEAN), segmentArrayList));
		}
		
		String se = generateSE((TransactionBean)template.getDataBean(template.TRANSACTION_BEAN), dataSetArrayList.get(0));
		
		issuerReportingBean.setMemberCount(memberCountWithinST);
		issuerReportingBean.setDependentCount(dependentCountWithinST);
		issuerReportingBean.setSubscriberCount(subscriberCount);
		issuerReportingBean.setNumberOfHouseHolds(1);
		
		
		if(!fileInitilazied) {
			String fileName = "";
			String fileExtension = null;
			String receiver = dataSetArrayList.get(0)[3].trim();
			String functionCode = "";
			if(fileTypeToGenerate.equals(FILE_TYPE_XML)) {
				//fileExtension = "xml";
				receiver = receiverOfTranslatedFile;
				functionCode = functionCodeForTranslatedFile;
			}
			else if(fileTypeToGenerate.equals(FILE_TYPE_BATCH))
			{
				int index = flatFileNameWithPath.lastIndexOf("/");
				if(index!=-1)
				{
					fileName = flatFileNameWithPath.substring(index+1, flatFileNameWithPath.length());
				}
				index = fileName.lastIndexOf(".");
				if(index!=-1)
				{
					fileName = fileName.substring(0, index);
				}
				if(fileName.endsWith("Individual") || fileName.endsWith("SHOP"))
				{
					index = fileName.lastIndexOf(".");
					if(index!=-1)
					{
						fileName = fileName.substring(0, index);
					}
				}
			}
			else {
				functionCode = "834X12";
			}
			fileName = "_Aggregator." + fileName;
			fileName = setOuputFileLocation(true, null, outputFileLocation, fileName, null, "");
			//x12FileName = fileName.substring(0, fileName.length()-1);
			x12FileName = fileName;
			//System.out.println("=======================================X12 file name" + x12FileName);
			issuerReportingBean.setFileGenerated(fileName);
			issuerReportingBean.setFileGeneratedType(fileTypeToGenerate);
			issuerReportingBean.setFileGeneratedLocation(outputFileLocation);
			//String isa = generateISA((InterchangeBean)template.getDataBean(template.INTERCHANGE_BEAN), dataSetArrayList.get(0));
			//String gs = generateGS((GroupBean)template.getDataBean(template.GROUP_BEAN), dataSetArrayList.get(0));
			
			//write(isa);
			//write(gs);
										
			//writerInitialized = true;
			//write(gs);
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
		//reportingBean.setTranslatedHouseHoldCount(issuerReportingBean.getNumberOfHouseHolds());
					
						
		
		return fileInitilazied;
		
		
	}
	
	private int getSTCount(String[] segmentArrayList) {
		String source = segmentArrayList[segmentArrayList.length-1];
		String sentence = "ST*834*";
		int occurrences = 0;

	    if (source.contains(sentence)) {
	        int withSentenceLength    = source.length();
	        int withoutSentenceLength = source.replace(sentence, "").length();
	        occurrences = (withSentenceLength - withoutSentenceLength) / sentence.length();
	    }
		return occurrences;
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
			
			if(lineArray.length >= IndexConstantsBatching.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX)
				memberID = lineArray[IndexConstantsBatching.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1];
			
			if(lineArray.length >= IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX)
				memberIndicator = lineArray[IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX-1];	
				
			if(lineArray.length >= IndexConstantsBatching.HIOS_ID_INDEX)
				HIOSID = lineArray[IndexConstantsBatching.HIOS_ID_INDEX-1];	
				
			if(lineArray.length >= IndexConstantsBatching.INSURANCE_LINE_OF_BUSINESS_INDEX)
				insuranceLineOfBusiness = lineArray[IndexConstantsBatching.INSURANCE_LINE_OF_BUSINESS_INDEX-1];	
			
			
			if(prevElementDataArray.length >= IndexConstantsBatching.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX)
				previousMemberID = prevElementDataArray[IndexConstantsBatching.EXCHNAGE_ASSIGNED_SUBSCRIBER_ID_INDEX-1];
			
			if(prevElementDataArray.length >= IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX)
				previousMemberIndicator = prevElementDataArray[IndexConstantsBatching.SUBSCRIBER_INDICATOR_INDEX-1];	
				
			if(prevElementDataArray.length >= IndexConstantsBatching.HIOS_ID_INDEX)
				previousHIOSID = prevElementDataArray[IndexConstantsBatching.HIOS_ID_INDEX-1];	
				
			if(prevElementDataArray.length >= IndexConstantsBatching.INSURANCE_LINE_OF_BUSINESS_INDEX)
				previousInsuranceLineOfBusiness = prevElementDataArray[IndexConstantsBatching.INSURANCE_LINE_OF_BUSINESS_INDEX-1];	
				
				
			if((memberID.equals(previousMemberID)) 
					&& (insuranceLineOfBusiness.equals(previousInsuranceLineOfBusiness))) {
				if(previousMemberIndicator.equals(ConfigurationConstants.SUBSCRIBER_INDICATOR_Y) && memberIndicator.equals(ConfigurationConstants.SUBSCRIBER_INDICATOR_Y)) {
					//transactionArrayList.add(0, lineArray);	// Changed for Batching
					//transactionArrayList.add(lineArray);
					
					policyArrayList.add(new ArrayList<String[]>());
					
					transactionArrayList = policyArrayList.get(policyArrayList.size() -1);
					transactionArrayList.add(lineArray);
					
					policyChanged = true;
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
	
	private void generateXMlReport(String alertFolderLocation, Map<String, String> alertOrErrorMessage) {
		try {
			for(Map.Entry<String, String> entry : alertOrErrorMessage.entrySet()){
				SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyyyyHHmmSS");
				File outputDirectory = new File(alertFolderLocation);
				if (!outputDirectory.exists()) {
					outputDirectory.mkdir();
				}
				String fileWithPath = entry.getKey();
				int fileBegining = fileWithPath.lastIndexOf("/");
				fileWithPath = fileWithPath.substring(fileBegining, fileWithPath.length());
				File alertFile = new File(outputDirectory, fileWithPath+"_"+DATE_FORMAT.format(new Date())+".xml");
				BufferedWriter bufferWritter = new BufferedWriter(new FileWriter(alertFile));
				bufferWritter.write(entry.getValue());
				bufferWritter.close();
				} 
			}catch (IOException e) {
				System.err.println(e);
			}
		}
	
	private void populateXMLError(Exception ex, String fileName,	String host) {
		try
		{
			StringBuilder xmlMessage = new StringBuilder();
			xmlMessage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			xmlMessage.append("<ti:BusinessItems xmlns:ti='http://com.edifecs/schema/TrackingInfo/2.0'>");
			xmlMessage.append("<ti:BusinessItem name=\"ETCPTransmission\">");
			xmlMessage.append("<ti:Identification>");
			xmlMessage.append("<ti:TrackingID>"+fileName+"</ti:TrackingID>");
			xmlMessage.append("</ti:Identification>");
			xmlMessage.append("<ti:Exceptions>");
			xmlMessage.append("<ti:Exception priority=\"P1\" severity=\"Critical\" type=\"BadMData\">");
			xmlMessage.append("<ti:Event>");
			xmlMessage.append("<ti:Type>Exception</ti:Type>");
			xmlMessage.append("<ti:Topic>XML to FlatFile</ti:Topic>");
			xmlMessage.append("<ti:Severity>Critical</ti:Severity>");
			xmlMessage.append("<ti:Situation>");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			Date dateobj = new Date();
			String date = df.format(dateobj);
			df = new SimpleDateFormat("HH:mm:SS");
			String time = df.format(dateobj);
			xmlMessage.append("<ti:Time>"+date+"T"+time+"</ti:Time>");
			xmlMessage.append("<ti:Category>Report</ti:Category>");
			xmlMessage.append("<ti:Disposition>UNSUCCESSFUL</ti:Disposition>");
			xmlMessage.append("<ti:ReasoningScope>INTERNAL</ti:ReasoningScope>");
			xmlMessage.append("</ti:Situation>");
			StringWriter detailedMessage = new StringWriter();
			ex.printStackTrace(new PrintWriter(detailedMessage));
			xmlMessage.append("<ti:Message><![CDATA["+detailedMessage+"]]></ti:Message>");
			xmlMessage.append("<ti:ReporterComponent>");
			File applicationExecutionPath = new File(BatchGenerator.class.getClassLoader().getResource("").toURI());
			xmlMessage.append("<ti:URL>"+applicationExecutionPath.getAbsolutePath()+"</ti:URL>");
			xmlMessage.append("<ti:Name>XSLT Transformation</ti:Name>");
			xmlMessage.append("<ti:Type>Java Program</ti:Type>");
			xmlMessage.append("<ti:Application>EDI</ti:Application>");
			xmlMessage.append("<ti:Location>"+host+"</ti:Location>");
			xmlMessage.append("</ti:ReporterComponent>");
			xmlMessage.append("</ti:Event>");
			xmlMessage.append("<ti:Subject>XSLT Transformation</ti:Subject>");
			xmlMessage.append("<ti:Summary>XSLT Transformation Failed</ti:Summary>");
			xmlMessage.append("</ti:Exception>");
			xmlMessage.append("</ti:Exceptions>");
			xmlMessage.append("</ti:BusinessItem>");
			xmlMessage.append("</ti:BusinessItems>");
			xmlErrorMessage.put(fileName, xmlMessage.toString());
		}
		catch (Exception exc)
		{
			 System.err.println(exc);
		}
	}
	private static FileSort getField(String line) {
    	String[] lineArray = line.split("\\|");
    	for(String str:lineArray)
    	{
    		System.out.println(str);
    	}
    
    	String date = lineArray[11];
    	String time = lineArray[12];
    	String fileDate = lineArray[8];
    	String fileTime = lineArray[9];
    	String controlNumber = lineArray[13];
    	//System.out.println(date+time);
    	if(time!=null && !time.matches("\\d+"))
    	{
    		time = time.substring(0, 6);
    	}
    	FileSort fileSort = new FileSort(Long.valueOf(date), Long.valueOf(time),Long.valueOf(fileDate),Long.valueOf(fileTime), Long.valueOf(controlNumber), line);
    	return fileSort;//extract value you want to sort on
    }
}
