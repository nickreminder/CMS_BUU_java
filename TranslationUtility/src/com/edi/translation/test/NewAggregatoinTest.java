package com.edi.translation.test;

import java.io.File;

import com.edi.translation.generation.BatchGenerator;

public class NewAggregatoinTest {

	public static void main(String[] args) {
		
		String tempalte = "C:/Delivery/EDI/KT/TranslationUtility/Template/834X12BatchingTemplate.ver0.2.tem";
		String outputLocation = "C:/Delivery/EDI/KT/TranslationUtility/Output/AGGR/X12/";
		String inputFolder = "C:/Delivery/EDI/KT/XMLTOX12/output/";
		String reportLocation = "C:/Delivery/EDI/KT/TranslationUtility/Output/AGGR/Report/";
		String mode = "BATCH";
		String environment = "T";
		String host = "localhost";
		String errorLocation = "C:/Delivery/EDI/KT/TranslationUtility/Error/";
		String isaLocation = "C:/Delivery/EDI/KT/TranslationUtility/Properties/ISAControlNumbers.properties";
		String gsLocation = "C:/Delivery/EDI/KT/TranslationUtility/Properties/GSControlNumbers.properties";
		
		File flatFilesFolder = new File(inputFolder); 
		File files[] = flatFilesFolder.listFiles();
		BatchGenerator generator = new BatchGenerator(tempalte, reportLocation,"C:/Delivery/EDI/KT/TranslationUtility/log");
		for (File file : files) {
			
			String filName = file.toString();
			filName = filName.replace("\\", "/");
			generator.generateHipaaDataGroupedByGS(filName, mode, environment, 
					host, errorLocation, outputLocation, 
					isaLocation, gsLocation, "", "");
		}
	}

}
