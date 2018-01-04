package com.edi.translation.reader;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
/*
 * Reads one line at a time.
 */

public class RandomFileReader {
	private String fileName = "";
	
	public RandomAccessFile fis;				
	private final int BYTES_TO_READ = 20480;				// Number of bytes to be read at a time.
	private String[] cachedFileLineArray;				// Cache for file lines.
	private boolean lastLineComplete = true;			// If the last line had full data
	private int cachedLinePointer = 0;					// Line to be read.
	private boolean dataAvailableForCaching = false;	// file has more data
	private int rowNumber = 0;	
	
	private String leftOverString = "";
	
	private long totalBytes;
	private long bytesRemanining;
	private long byteLocationForSegment;
	private int prevSegmentLength;
	
	private String LINE_DELIMITER = "\n";
	private String ELEMENT_DELIMITER = "\\|";
	
	private boolean isFileTypeXML = false;
	
	public static void main(String args[]) throws Exception {
		String fileNameWithPath = "C:/Parimanshu/EDI/Mapping/testFiles/5.2.D20140411.T104515534.out";
		//String fileNameWithPath = "C:/Parimanshu/poi/Output/family/initial/Set1/5.1.D20140410.T16414031.out";
		
		long start = Calendar.getInstance().getTimeInMillis();
		//HipaaReader reader = new HipaaReader(fileNameWithPath);
		
		search(fileNameWithPath, false);
				
		System.out.println("Time Taken(s): " + (Calendar.getInstance().getTimeInMillis() - start));
	}
	
	public static void search(String fileNameWithPath, boolean isFileTypeXML) throws Exception {	
		RandomFileReader reader = new RandomFileReader(fileNameWithPath, isFileTypeXML);
		
		while(reader.hasMoreData()) {			
			String line = reader.getCurrentSegmentString();
			
			if(line != null) {
				if(line.startsWith("ST")) {
					//System.out.println(line);
					//System.out.println("Pointer:" + reader.getCurrentSegmentLocation());
					
				}
				else if(line.startsWith("SE")) {
					//System.out.println(line);
					try {
						reader.seekFile(164);
					} catch (Exception e) {
						throw e;
					}
				}
				else if(line.startsWith("GS")) {
					//System.out.println(line);
				}
				else if(line.startsWith("GE")) {
					//System.out.println(line);
				}
				else if(line.startsWith("ISA")) {
					//System.out.println(line);
				}
				else if(line.startsWith("IEA")) {
					//System.out.println(line);
				}
			}
			
			reader.linePointerIncrement();
		}
		
		//System.out.println("Segment Array size: " + segmentArrayList.size());
	}
	
	public RandomFileReader(String fileNameWithPath, boolean isFileTypeXML) throws Exception {
		fileName = fileNameWithPath;
		this.isFileTypeXML = isFileTypeXML;
		
		if(fileName != null && fileName.length() != 0)
		{
			try 
			{
				fis = new RandomAccessFile(fileName, "r");
				
				totalBytes = fis.length();
				bytesRemanining = totalBytes;
				
				byte[] buf = readBytes();
				if(dataAvailableForCaching) {
					fillLineArray(buf);
				}
			}
			catch (Exception e) {
				System.err.println(e);
				throw e;
			}
		}
	}
	
	private long fileLocation() {
		long returnVal = 0;		
		return returnVal;
	}
	
	/*
	 * read bytes from the file
	 */
	private byte[] readBytes() throws Exception {
		byte buf[] = null;	
		
		try {					
			if(bytesRemanining == 0) {	// Closes the file and sets the cachedFileLineArray to null, if no data found.
				//fis.close();
				cachedFileLineArray = null;
				dataAvailableForCaching = false;
			}
			else {			
				dataAvailableForCaching = true;
				
				if(bytesRemanining < BYTES_TO_READ) {
					buf = new byte[(int)bytesRemanining];
					bytesRemanining -= bytesRemanining;
				}
				else {
					buf = new byte[BYTES_TO_READ];
					bytesRemanining -= BYTES_TO_READ;
				}
							
				fis.read(buf);
				//System.out.println(new String(buf));
			}
		}
		catch (Exception e){
			System.err.println(e);
			throw e;
		}
		
		return buf;
	}
		
	private String escapeSpecialCharactersForXML(String data) {
		data = data.replace("&", "&amp;");
		data = data.replace("<", "&lt;");
		data = data.replace(">", "&gt;");
		data = data.replace("\"", "&quot;");
		data = data.replace("'", "&apos;");
		
		return data;
	}
	
	private boolean fillLineArray(byte byteArray[])	{
		String line = new String(byteArray);
		
		
		// Logic to copy the leftover line
		if(!lastLineComplete) {
			line = cachedFileLineArray[cachedFileLineArray.length - 1] + line; 
		}
		
		if(String.valueOf((char)byteArray[byteArray.length - 1]).equals(LINE_DELIMITER) || bytesRemanining == 0)
			lastLineComplete = true;
		else
			lastLineComplete = false;
		
		
		
		cachedFileLineArray = line.split(LINE_DELIMITER);	
		
		return true;
	}
	
	
	
	public long getCurrentSegmentLocation() {
		return byteLocationForSegment;
	}
	
	public boolean moveFilePointer(long seekValue) throws Exception {
		fis.seek(seekValue);
		
		byteLocationForSegment = seekValue;
		bytesRemanining = totalBytes - seekValue;
		return overwriteLineArray(readBytes());
	}
	
	private boolean overwriteLineArray(byte byteArray[]) {
		lastLineComplete = true;
		
		cachedLinePointer = 0;
		rowNumber = 0;
		
		return fillLineArray(byteArray);
	}
	
	
	
	public boolean seekFile(long seekValue) throws IOException {
		fis.seek(seekValue);
		
		return true;
	}
	
	public String getCurrentSegmentString() {
		String line = null;
		
		if(dataAvailableForCaching)
		{
			if(prevSegmentLength > 0)
				byteLocationForSegment += prevSegmentLength;			
			
			line = cachedFileLineArray[cachedLinePointer];	
			prevSegmentLength = line.length() + 1;
		}
		
		if(isFileTypeXML) {
			line = escapeSpecialCharactersForXML(line);
		}
		
		return line;
	}
	
	/*
	 * Returns null if no line found.
	 */
	public String[] getCurrentElementArray()
	{
		String[] lineArray = null;
		
		String line = getCurrentSegmentString();
		
		if(line == null)
			System.out.println(line);
		
		if("\r".equals(line)) {
			line = null;
		}

		if(line != null)
		{
			line = line.trim();
			
			if(line.length() > 0)
				lineArray = line.split(ELEMENT_DELIMITER);
		}
		
		return lineArray;
	}	
	
	public boolean closeFile() throws IOException {
		fis.close();
		
		return true;
	}
	
	public String getNextSegmentString() throws Exception
	{
		String line = null;
		
		if(dataAvailableForCaching) {
			int nextLinePointer = cachedLinePointer + 1;
			
			if(nextLinePointer < cachedFileLineArray.length)			
				line = cachedFileLineArray[nextLinePointer];
			else {
				String currentLine = cachedFileLineArray[nextLinePointer];
				
				byte[] buf = readBytes();
				if(buf != null) {
					byte[] buf1 = currentLine.getBytes();
					byte[] buf2 = new byte[buf.length + buf1.length];
					
					System.arraycopy(buf1, 0, buf2, 0, buf1.length);
					System.arraycopy(buf, 0, buf2, 0, buf.length);
					
					fillLineArray(buf);					
				}
				else
				{
					cachedFileLineArray = new String[1];
					cachedFileLineArray[0] = currentLine;					
				}
				
				cachedLinePointer = 0;
				nextLinePointer = cachedLinePointer + 1;
				if(nextLinePointer < cachedFileLineArray.length)			
					line = cachedFileLineArray[nextLinePointer];
			}
		}
				
		if(isFileTypeXML) {
			line = escapeSpecialCharactersForXML(line);
		}
		
		return line;
	}	
	
	public String[] getNextElementArray() throws Exception
	{
		String[] lineArray = null;
		String line = getNextSegmentString();
		
		if(line != null)
		{
			lineArray = line.split(ELEMENT_DELIMITER);
		}
		
		return lineArray;
	}	
	
	
	
	/*
	 *  Increases the pointer for the line
	 */
	public void linePointerIncrement() throws Exception
	{
		rowNumber++;
		if(!lastLineComplete &&  cachedLinePointer + 2 < cachedFileLineArray.length) {
			++cachedLinePointer;
			
		}
		else if(lastLineComplete && cachedLinePointer + 1 < cachedFileLineArray.length) {
			++cachedLinePointer;
		}
		else {
			cachedLinePointer = 0;
			byte[] buf = readBytes();
			if(buf != null)
				fillLineArray(buf);
		}
	}	
	
	public int getCurrentRowCount() {
		return rowNumber;
	}
	public boolean hasMoreData() {
		return dataAvailableForCaching;
	}
}
