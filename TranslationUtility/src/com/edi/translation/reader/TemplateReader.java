package com.edi.translation.reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.edi.translation.bean.BaseBean;
import com.edi.translation.bean.GroupBean;
import com.edi.translation.bean.InterchangeBean;
import com.edi.translation.bean.TransactionBean;
import com.edi.translation.constants.ConfigurationConstants;

public class TemplateReader {	
	private Map<String, BaseBean> templateMap = new HashMap<String, BaseBean>();
	public final String INTERCHANGE_BEAN = "Interchange";
	public final String TRANSACTION_BEAN = "Transaction";
	public final String GROUP_BEAN = "Group";
	
	public static void main(String ars[]) {
		String x12Template = "";
		
		try {
			TemplateReader template = new TemplateReader(x12Template);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public TemplateReader(String template) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(template));
		
		String strLine = null;
		BaseBean bean = null;
		boolean repeatSegment = false;
		boolean transaction = false;
		boolean interchange = false;
		boolean group = false;
		while((strLine = br.readLine()) != null) {
			if(strLine.startsWith(ConfigurationConstants.INTERCHANGE_START_INDICATOR)) {
				bean = new InterchangeBean();
				bean.setStartTag(extractGenericData(strLine, ConfigurationConstants.INTERCHANGE_START_INDICATOR));
				templateMap.put(INTERCHANGE_BEAN, bean);
				
				interchange = true;
				transaction = false;
				group = false;
			} else if(strLine.startsWith(ConfigurationConstants.INTERCHANGE_END_INDICATOR)) {
				templateMap.get(INTERCHANGE_BEAN).setEndTag(extractGenericData(strLine, ConfigurationConstants.INTERCHANGE_END_INDICATOR));
			} else if(strLine.startsWith(ConfigurationConstants.GROUP_START_INDICATOR)) {
				bean = new GroupBean();
				bean.setStartTag(extractGenericData(strLine, ConfigurationConstants.GROUP_START_INDICATOR));
				templateMap.put(GROUP_BEAN, bean);
				
				interchange = false;
				transaction = false;
				group = true;
			} else if(strLine.startsWith(ConfigurationConstants.GROUP_END_INDICATOR)) {
				templateMap.get(GROUP_BEAN).setEndTag(extractGenericData(strLine, ConfigurationConstants.GROUP_END_INDICATOR));
			} else if(strLine.startsWith(ConfigurationConstants.TRANSACTION_START_INDICATOR)) {
				bean = new TransactionBean();
				bean.setStartTag(extractGenericData(strLine, ConfigurationConstants.TRANSACTION_START_INDICATOR));
				templateMap.put(TRANSACTION_BEAN, bean);
				
				interchange = false;
				transaction = true;
				group = false;
			} else if(strLine.startsWith(ConfigurationConstants.TRANSACTION_END_INDICATOR)) {
				templateMap.get(TRANSACTION_BEAN).setEndTag(extractGenericData(strLine, ConfigurationConstants.TRANSACTION_END_INDICATOR));
			} else if(strLine.startsWith(ConfigurationConstants.REPEAT_START_INDICATOR)) {
				((TransactionBean)templateMap.get(TRANSACTION_BEAN)).setMemberListArray(extractGenericData(strLine, ConfigurationConstants.REPEAT_START_INDICATOR));
				repeatSegment = true;
			} else if(strLine.startsWith(ConfigurationConstants.REPEAT_END_INDICATOR)) {
				((TransactionBean)templateMap.get(TRANSACTION_BEAN)).setMemberListArray(extractGenericData(strLine, ConfigurationConstants.REPEAT_END_INDICATOR));
				repeatSegment = false;
			} else if(transaction) {
				if(repeatSegment) {
					((TransactionBean)bean).setMemberListArray(strLine);
				} else {
					((TransactionBean)bean).setSegmentListArray(strLine);
				}			
			} else if(group) {
				((GroupBean)bean).setSegmentListArray(strLine);	
			} else if(interchange) {
				((InterchangeBean)bean).setSegmentListArray(strLine);
			}
		}
		
		br.close();
	}
	
	public BaseBean getDataBean(String beanIdentifier) {
		return templateMap.get(beanIdentifier);
	}
	
	private String extractGenericData(String line, String startingIdentifierTag) {
		String dataString = line.substring(startingIdentifierTag.length(), line.length());
		
		return dataString;
	}	
}
