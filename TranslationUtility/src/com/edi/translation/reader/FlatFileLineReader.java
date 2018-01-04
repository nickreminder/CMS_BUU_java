package com.edi.translation.reader;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import com.edi.translation.constants.ConfigurationConstants;

/*
 * Reads one line at a time.
 */

public class FlatFileLineReader {
	BufferedReader br = null;
	private ArrayList<String[]> cachedFileLineArrayList;				// Cache for file lines.
	
	private int currentLineCount = 0;
	private boolean fileReadComplete = false;
	private boolean firstLineSkipped = false;
		
	public static void main(String args[]) {
		String fileNameWithPath = "C:/Parimanshu/FlatFileTranslation/TestFile.txt";
		//String fileNameWithPath = "C:/Parimanshu/poi/Output/family/initial/Set1/5.1.D20140410.T16414031.out";
		
		long start = Calendar.getInstance().getTimeInMillis();
		FlatFileLineReader reader = new FlatFileLineReader();
		try {
			reader.setFileToRead(fileNameWithPath);
			while(reader.isDataAvaliable()) {	
				String[] lineArray = reader.getCurrentLine();
				//System.out.println(lineArray[0]);
				
				reader.linePointerIncrement();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//reader.search();
				
		System.out.println("Time Taken(s): " + (Calendar.getInstance().getTimeInMillis() - start));
	}
	
	public FlatFileLineReader() {				
	}
	
	public boolean setFileToRead(String fileNameWithPath) throws IOException {
		if(fileNameWithPath != null && fileNameWithPath.length() != 0) {
			FileInputStream fis = new FileInputStream(fileNameWithPath);
			DataInputStream in = new DataInputStream(fis);
			br = new BufferedReader(new InputStreamReader(in));
			
			cachedFileLineArrayList = readLine();	
		}
		
		return true;
	}	
	
	/*
	 * read n lines  per read
	 */
	private ArrayList<String[]> readLine() throws IOException {
		ArrayList<String[]> lineArrayList = new ArrayList<String[]>();		
		
		String strLine = null;
		int lineCounter = 0;
		while (!fileReadComplete && true ) {
			if((lineCounter >= ConfigurationConstants.LINE_READ_PER_RUN)) {
				break;
			} else if((strLine = br.readLine()) == null) {
				fileReadComplete = true;
				break;
			}			
			
			if(strLine != null && strLine.trim().length() == 0) {
				continue;
			}
			
			if(!firstLineSkipped) {	// First line skipped as it contains the difinitions.
				firstLineSkipped = true;
				continue;
			}					
			
			//System.out.println(strLine);
			String[] lineArray = strLine.split(ConfigurationConstants.ELEMENT_DELIMITER);
			
			lineCounter++;
			lineArrayList.add(lineArray);
		}		
		
		return lineArrayList;
	}
		
	public String[] getCurrentLine() {		
		return cachedFileLineArrayList.get(currentLineCount);
	}
	
	private String[] getNextLine() throws IOException {
		String[] lineArray = null;
		
		int nextLinePointer = currentLineCount + 1;
			
		if(nextLinePointer < cachedFileLineArrayList.size())			
			lineArray = cachedFileLineArrayList.get(nextLinePointer);
		else
		{
			if(!fileReadComplete) {
				String[] currentLine = cachedFileLineArrayList.get(currentLineCount);
				
				cachedFileLineArrayList = readLine();
				cachedFileLineArrayList.add(0, currentLine);
				
				currentLineCount = 0;
				nextLinePointer = currentLineCount + 1;
				lineArray = cachedFileLineArrayList.get(nextLinePointer);
			}
		}		
						
		return lineArray;
	}
	
	public void linePointerIncrement() throws IOException {
		if(++currentLineCount  < cachedFileLineArrayList.size()) {
			//System.out.println("Pointer increased.");
		} else if(!fileReadComplete){
			currentLineCount = 0;
			cachedFileLineArrayList = readLine();
		}
	}
	
	public boolean isDataAvaliable() {
		boolean dataAvaliable = false;
	
		if(!fileReadComplete) {
			dataAvaliable = true;
		} else if(fileReadComplete && currentLineCount < cachedFileLineArrayList.size()) {
			dataAvaliable = true;
		}	
		
		return dataAvaliable;
	}
}
