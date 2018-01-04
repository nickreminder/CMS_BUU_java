package com.edi.translation.constants;

public class ConfigurationConstants {
	public static final String ELEMENT_DELIMITER = "\\|";
	public static final int LINE_READ_PER_RUN = 20;
	public static final String SUBSCRIBER_INDICATOR_Y = "Y";
	public static final String SUBSCRIBER_INDICATOR_N = "N";
	
	public static final String INTERCHANGE_START_INDICATOR = "Interchange START:";
	public static final String INTERCHANGE_END_INDICATOR = "Interchange END:";
	
	public static final String GROUP_START_INDICATOR = "Group START:";
	public static final String GROUP_END_INDICATOR = "Group END:";
	
	public static final String TRANSACTION_START_INDICATOR = "Transaction START:";
	public static final String TRANSACTION_END_INDICATOR = "Transaction END:";
	
	public static final String REPEAT_START_INDICATOR = "Repeat START:";
	public static final String REPEAT_END_INDICATOR = "Repeat END:";
	
	// Below are specific to file naming
	public static final String IN_O834BU = "O834BU";
	public static final String IN_O834BP = "O834BP";
	public static final String IN_OCF15 = "OCF15";
	public static final String IN_OCF15P = "OCF15P";
	public static final String IN_OCF16P = "OCF16P";
	public static final String IN_OCF16Q = "OCF16Q";
	public static final String IN_OCFP = "OCFP";
	public static final String IN_OCFQ = "OCFQ";


	public static final String OUT_I834BU = "EDI.I834BU";
	public static final String OUT_I834BP = "EDI.I834BP";
	public static final String OUT_ICF15 = "EDI.ICF15Q";
	public static final String OUT_ICF15P = "EDI.ICF15P";
	public static final String OUT_ICF16P = "EDI.ICF16P";
	public static final String OUT_ICF16Q = "EDI.ICF16Q";
	public static final String OUT_ICFP = "EDI.ICFP";
	public static final String OUT_ICFQ = "EDI.ICFQ";

	public static final String OUT_RPT_IBUR = "EDI.IBUR";
	public static final String OUT_RPT_IBURP = "EDI.IBURP";
	public static final String OUT_RPT_IBR15 = "EDI.IBR15";
	public static final String OUT_RPT_IBR15P = "EDI.IBR15P";
	public static final String OUT_RPT_IBR16P = "EDI.IBR16P";
	public static final String OUT_RPT_IBR16Q = "EDI.IBR16Q";
	public static final String OUT_RPT_IBRP = "EDI.IBRP";
	public static final String OUT_RPT_IBRQ = "EDI.IBRQ";
}
