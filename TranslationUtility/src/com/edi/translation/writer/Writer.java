package com.edi.translation.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import com.edi.translation.constants.ConfigurationConstants;

public class Writer {
	BufferedWriter bw = null;
	PrintWriter out = null;		
	
	public static void main(String[] args) {
		Writer writer = new Writer();
	}
	
	public Writer() {		
	}

	/*
	 * Assumption: The fileIdentifier is always provided.
	 */
	public String initialize(boolean generateDateTime, String outputFileName, String path, String fileIdentifier, String fileExtension, String envVariable) throws IOException {
		if(outputFileName == null) {
			if(generateDateTime) {
				outputFileName = generateFileName(fileIdentifier) + "." + envVariable;
			} else {
				outputFileName = fileIdentifier + "." + envVariable;
			}
			
			if(fileExtension != null)
				outputFileName +=  "." + fileExtension;
				
		} else {
			if(fileIdentifier != null) {			
				if(generateDateTime) {
					outputFileName = generateFileName(fileIdentifier) + "." + outputFileName + "." + envVariable;
				} else {
					outputFileName = fileIdentifier + "." + outputFileName + "." + envVariable;
				}
			} else {
				if(generateDateTime) {
					outputFileName = generateFileName(outputFileName) + "." + envVariable;
				} else {
					outputFileName = outputFileName + "." + envVariable;
				}
			}
			
			if(fileExtension != null)
				outputFileName += "." + fileExtension;			
		}
		
		if(out != null) 
			out.close();
		
		out = new PrintWriter(new BufferedWriter(new FileWriter(path + outputFileName)));
		
		return outputFileName;
	}	
	
	public String generateFileName(String fileIdentifier) {
		String outputFileName =  fileIdentifier + "." + (new SimpleDateFormat("'D'yyMMdd'.T'HHmmssSSS").format(new Date()));
		
		return outputFileName;
	}
	
	public void write(String line) {
		out.write(line);
		out.flush();
	}
	
	public void close() {
		out.close();
	}
	
	/**
	 * New method introduced to handle 2014 and 2015 files with original time stamp.
	 * @param inputFileName
	 * @param isReport
	 * @return
	 */
	public String generateFileName( String inputFileName, boolean isReport)
	{
		String outputFileName;
		try
		{
			StringTokenizer fileNameToeknizer = new StringTokenizer(inputFileName, ".");
			String receiver = fileNameToeknizer.nextToken();
			String functionalCode = fileNameToeknizer.nextToken();
			String date = fileNameToeknizer.nextToken();
			String time = fileNameToeknizer.nextToken();
			String environment = fileNameToeknizer.nextToken();

			if(isReport)
			{
				outputFileName = getReportFileNameFromFunctionCode(functionalCode);
			}
			else
			{
				outputFileName = getXMlFileNameFromFunctionCode(functionalCode);
			}
			
			//outputFileName = outputFileName;
			//System.out.println("OUTPUT FILE NAME: " + outputFileName);
		}
	    catch(Exception ex)
	    {
	    	System.err.println(" File name convention rule violated!!! Expected file format is <FunctionCode>.<Receiver>.DXXXXXX.TNNNNNNNNN.P");
	    	System.err.println(ex);
	    	throw ex;
	    }
		return outputFileName;
	}

	/**
	 * Out bound to in bound functional code mapping for xml file.
	 * @param functionalCode
	 * @return
	 */
	private String getXMlFileNameFromFunctionCode(String functionalCode)
	{
		if (ConfigurationConstants.IN_O834BU.equals(functionalCode)) {
			return ConfigurationConstants.OUT_I834BU;
		} else if (ConfigurationConstants.IN_O834BP.equals(functionalCode)) {
			return ConfigurationConstants.OUT_I834BP;
		} else if (ConfigurationConstants.IN_OCF15.equals(functionalCode)) {
			return ConfigurationConstants.OUT_ICF15;
		} else if (ConfigurationConstants.IN_OCF15P.equals(functionalCode)) {
			return ConfigurationConstants.OUT_ICF15P;
		}
		else if (ConfigurationConstants.IN_OCF16P.equals(functionalCode)) {
			return ConfigurationConstants.OUT_ICF16P;
		}
		else if (ConfigurationConstants.IN_OCF16Q.equals(functionalCode)) {
			return ConfigurationConstants.OUT_ICF16Q;
		}
		else if (ConfigurationConstants.IN_OCFP.equals(functionalCode)) {
			return ConfigurationConstants.OUT_ICFP;
		}
		else if (ConfigurationConstants.IN_OCFQ.equals(functionalCode)) {
			return ConfigurationConstants.OUT_ICFQ;
		}
		return "";
	}
	
	/**
	 * Out bound to in bound functional code mapping for report file.
	 * @param functionalCode
	 * @return
	 */
	private String getReportFileNameFromFunctionCode(String functionalCode)
	{
		if (ConfigurationConstants.IN_O834BU.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBUR;
		} else if (ConfigurationConstants.IN_O834BP.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBURP;
		} else if (ConfigurationConstants.IN_OCF15.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBR15;
		} else if (ConfigurationConstants.IN_OCF15P.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBR15P;
		}
		else if (ConfigurationConstants.IN_OCF16P.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBR16P;
		} else if (ConfigurationConstants.IN_OCF16Q.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBR16Q;
		}
		else if (ConfigurationConstants.IN_OCFP.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBRP;
		} else if (ConfigurationConstants.IN_OCFQ.equals(functionalCode)) {
			return ConfigurationConstants.OUT_RPT_IBRQ;
		}
		return "";
	}
}
