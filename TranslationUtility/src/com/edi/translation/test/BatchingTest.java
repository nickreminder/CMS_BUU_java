package com.edi.translation.test;

import java.io.IOException;
import java.util.Calendar;

import org.xml.sax.SAXException;

import com.edi.translation.generation.BatchGenerator;
import com.edi.translation.generation.Generator;

public class BatchingTest {
	public static void main(String args[]) {
		long start = Calendar.getInstance().getTimeInMillis();
		String templateFileNameWithPath = "C:/Delivery/EDI/KT/TranslationUtility/Template/834X12BatchingTemplate.ver0.2.tem";
		
		
		//String templateFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Template/834X12Template.ver7.12.tem";
		//String outputFileLocation = "C:/Parimanshu/FlatFileTranslation/Output/";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/AllEnrolledMembers-2014-03-26.psv";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/TEST DATA.dat";
			
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/FlatFile_UnitTesting_4242014.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Termination+Subscriber+Dependent.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Cancellation+Subscriber+Dependent.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Effecutaion+Subscriber+Dependent.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/IE+Subscriber+Dependent.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/IE+Subscriber.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Test_10k.txt.Archieve.Archieve_4";		// Test file
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/TestFile50000.txt";		// Test file
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/FlatFile-20140508.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/IE+Subscriber+Dependent2.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/IE+Subscriber+Dependent.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/5.1.D20140610.T100831864.out";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/FlatFile_UnitTesting_4242015.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Sub&dependent.txt";	
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/BatchTestFile/test1.txt";
		String flatFileNameWithPath = "C:/Delivery/EDI/KT/XMLTOX12/output/D100000002_S834AR.txt";
		
		//String flatFileNameWithPath = "C:/Parimanshu/poi/Templates/FlatFile/Initial/Output/Set1/5.1.D20140912.T140109951.out";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/LdSpaces.txt";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/MissingElement.txt";
		
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/NoData.txt";
		
		//String templateFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/Template/Test Template.tem";
		String outputFileLocation = "C:/Delivery/EDI/KT/TranslationUtility/Output/AGGR/";
		//String flatFileNameWithPath = "C:/Parimanshu/FlatFileTranslation/TEST DATA.dat";
		
		String reportingFileLocation = "C:/Delivery/EDI/KT/TranslationUtility/Output/AGGR/";
		
		String errorFolder = "C:/Delivery/EDI/KT/TranslationUtility/Error/";
		
		BatchGenerator generator = new BatchGenerator(templateFileNameWithPath, reportingFileLocation,"");
		try {
			//generator.generateHipaaData(flatFileNameWithPath, Generator.FILE_TYPE_XML);
			String receiverOfTranslatedFile = "C:/Delivery/EDI/KT/TranslationUtility/Properties/ISAControlNumbers.properties";
			String functionCodeForTranslatedFile = "C:/Delivery/EDI/KT/TranslationUtility/Properties/GSControlNumbers.properties";
			
			String receiverOfReport = "EDI";
			String functionCodeForReport = "IBUR";
			
			generator.generateHipaaDataGroupedByGS(flatFileNameWithPath, Generator.FILE_TYPE_X12, "T", 
					"tcdsh0d5r6da04", errorFolder, outputFileLocation, 
					receiverOfTranslatedFile, functionCodeForTranslatedFile, receiverOfReport, functionCodeForReport);
		}
		 catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		System.out.println("Time Taken(s): " + (Calendar.getInstance().getTimeInMillis() - start));	
	}
}