# default preFix is the current date
# change it to fit your data

#preFix=D14`date "+-%m-%d" | sed -e 's/[-:.T]//g'`
#if [ "$USECONFIG" == Y ];
# then
 #       datevar="`echo ${STARTDATETIME:0:10}`"
  #      TODAY=`date  --date=$datevar '+%Y%m%d%H%m%s%n'`
#else
 #       TODAY=$(date +"%Y%m%d%H%m%s%n" -d '1 day ago')
# fi
DESTINATIONPATH=$1
ENVIRONMENTSUFFIX=$2



REPORT_LOCATION="/opt/EDI_HIPAA_FOLDER/Processing/Outbound/FLATFILE/Report"
REPORT_ARCHIVE_LOCATION="/opt/EDI_HIPAA_FOLDER/Archive/Outbound/FLATFILE/Report"
if [ ! -d $REPORT_LOCATION ]; then
	mkdir -p $REPORT_LOCATION
fi
SCRIPT_LOCATION="/home/ediadmin/scripts/BUU_Scripts/generateBUUReport"

cd $SCRIPT_LOCATION
returncode=$?
if [ $returncode -ne 0 ]; then
	exit 1
fi

preFix=${1-`date "+%y%m%d"`}

echo $preFix

CURRENT_TS=`date "+%Y-%m-%dT%H:%M:%S"`
CURRENT_TS_STR=`echo "$CURRENT_TS" | sed -e 's/[-:.T]//g'`
EDIFile=EDIFile_count${CURRENT_TS_STR}.csv
EDIFullFile=EDIFullFile_count${CURRENT_TS_STR}.csv
#count=`ls -1 $REPORT_LOCATION/*D$preFix*.$ENVIRONMENTSUFFIX 2>/dev/null | wc -l` 
count=`ls -1 $REPORT_LOCATION/ | wc -l` 
if [ $count -ne 0 ]; then 
	files=`ls $REPORT_LOCATION/` 
	for file in $files
	do
		./EDIFile_Parameters $REPORT_LOCATION/$file >> Report/EDIFile_tmp
		./EDIFile_Parameters_Fullname $REPORT_LOCATION/$file >> Report/EDIFullFile_tmp
	done
	if [ "$DESTINATIONPATH" != "" ]; then
		echo REPORTFILES=$files
	fi
	sort Report/EDIFullFile_tmp > Report/$EDIFullFile 
	sort Report/EDIFile_tmp > Report/$EDIFile
	rm Report/EDIFullFile_tmp
	rm Report/EDIFile_tmp 
	sed -i '1iMIDAS Filename,EDI Translated FileName,EDI Report Filename,Policy Success Counts,Policy Failed Counts' Report/$EDIFile
	
	sed -i '1iMIDAS Filename|EDI Translated FileName|EDI Report Filename|Policy Success Counts|Policy Failed Counts|Record Counts|Status|Error Message' Report/$EDIFullFile
       
else
	echo "No files to count with today's date. Please pass date parameter to script for other dates as YYDDMM"
fi
if [ "$DESTINATIONPATH" != "" ]; then
	cp Report/$EDIFullFile $DESTINATIONPATH
	echo EDIFullFile=$EDIFullFile
fi
exit

