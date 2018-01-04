#!/bin/bash
# This script is intended to be used with translation tool for producing the XML and X12 from flat files
## File type to be processed, passed as parameter to script
SCRIPT_NAME="translation.sh"

#export JAVA_HOME=/opt/jdk1.7.0_21
export JAVA_HOME=/opt/jdk1.8.0_73
export PATH=$JAVA_HOME/bin:$PATH

FILE_TYPE=$1
TRANSLATION_SCRIPT_HOME=$2
OVERRIDE_XML_TRANSLATION_SOURCE_DIRECTORY=$3
OVERRIDE_XML_TRANSLATION_PROCESSING_DIRECTORY=$4


cd $TRANSLATION_SCRIPT_HOME
ReturnCode=$?
if [ $ReturnCode != 0 ]; then
	echo "Invalid path $TRANSLATION_SCRIPT_HOME"
	exit 1
fi

status=`/sbin/pidof -x $TRANSLATION_SCRIPT_HOME/$SCRIPT_NAME`
statusArray=($status)

if [ ${#statusArray[@]} -gt 1 ]; then
    echo "$(date): scrip already running"
    exit 1
fi

echo "$(date): ----------------------- Starting Translation type : $FILE_TYPE -----------------------"
## Common Properties
. ./properties/CommonProperties.prop
ENVIRONMENT_VARIABLE=$ENVIRONMENT_VARIABLE

#echo "$(date): Environment Variable: $ENVIRONMENT_VARIABLE"
if [ "$FILE_TYPE" == "$X12_TYPE" ];then
	. ./properties/LoadX12Properties.prop
	sourceDir="$X12_TRANSLATION_SOURCE_DIRECTORY"
	processingDir="$X12_TRANSLATION_PROCESSING_DIRECTORY"
	archivalDir="$X12_TRANSLATION_SOURCE_ARCHIVAL_DIRECTORY"
elif [ "$FILE_TYPE" == "$XML_TYPE" ];then
	. ./properties/LoadXMLProperties.prop
	sourceDir="$XML_TRANSLATION_SOURCE_DIRECTORY"
	processingDir="$XML_TRANSLATION_PROCESSING_DIRECTORY"
	#archivalDir="$XML_TRANSLATION_SOURCE_ARCHIVAL_DIRECTORY"
	archivalDir=""
else
	echo "Unknown FILE_TYPE $FILE_TYPE"
	exit 1
fi
if  [ "$FILE_TYPE" == "$XML_TYPE" ]; then
	if [ "$OVERRIDE_XML_TRANSLATION_SOURCE_DIRECTORY" != "" ]; then
		XML_TRANSLATION_SOURCE_DIRECTORY=$OVERRIDE_XML_TRANSLATION_SOURCE_DIRECTORY
		sourceDir=$XML_TRANSLATION_SOURCE_DIRECTORY
	fi
	if [ "$OVERRIDE_XML_TRANSLATION_PROCESSING_DIRECTORY" != "" ]; then
		XML_TRANSLATION_PROCESSING_DIRECTORY=$OVERRIDE_XML_TRANSLATION_PROCESSING_DIRECTORY
		processingDir="$XML_TRANSLATION_PROCESSING_DIRECTORY"
	fi
fi

ReturnCodes=0

echo "$(date): Source Directory: $sourceDir"
echo "$(date): Archival Directory: $archivalDir"
echo "$(date): Processing Directory: $processingDir"

## Read files from the directory
for file in `find $sourceDir -maxdepth 1 -mindepth 1 -mmin +1 `
do
    if [ -f "$file" ];then
		if [ "$archivalDir" != "" ]; then
			echo "$(date): Copying $file to $archivalDir"
			cp $file $archivalDir/
		fi
	
		echo "$(date): Moving $file to $processingDir"
        mv $file $processingDir

		fileName=`basename $file`

        echo "$(date): Translating file: $processingDir$fileName"
        if [ "$FILE_TYPE" == "$X12_TYPE" ];then
			java -jar translator.jar "$X12_TEMPLATE" "$X12_DESTINATION_DIRECTORY" "$processingDir$fileName" "$X12_TRANSLATION_REPORT_LOCATION" "$FILE_TYPE" "$ENVIRONMENT_VARIABLE" 2>&1
			returncode="$?"
        elif [ "$FILE_TYPE" == "$XML_TYPE" ];then
			java -jar translator.jar "$XML_TEMPLATE" "$XML_DESTINATION_DIRECTORY" "$processingDir$fileName" "$XML_TRANSLATION_REPORT_LOCATION" "$FILE_TYPE" "$ENVIRONMENT_VARIABLE" "$XSD_FOR_TRANSLATED_FILE" "$XML_TRANSLATION_VALIDATION_FAILURE_DIRECTORY" "BUU" "I834BP" "BUU" "IBURP" 2>&1
			returncode="$?"
        fi
        if [ $returncode -eq 0 ];then
        	echo "$(date): File '$file' was sucessfully translated."        
        elif [ $returncode -eq 2 ];then
			echo "$(date): File '$file' was partially Translated. Refer to Translation Report for details"
		elif [ $returncode -gt 2 ];then
			echo "$(date): File '$file' Translation Failed with error code $returncode"
		else
			echo "$(date): File '$file' returned $returncode"
		fi
		if [[ $returncode -ne 0 ]] && [[ $returncode -ne 2 ]]; then
			ReturnCodes=1
		fi
		echo "$(date): Removing file: $processingDir$fileName"
		rm -f "$processingDir$fileName"
	fi
done
echo "$(date): _______________________ Ending Translation type : $FILE_TYPE _______________________"

exit $ReturnCodes
