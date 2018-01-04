#!/bin/bash
#
# auto process BUU 
#
# dcaddes
# 08/11/2015
#
#Modified by Kranthi Kumar Mandumula on 10/24/2016 w.r.t artifact250468
# 11/04/2015 dac add 2016 File name codes added
#Modified on 02/01/2017 for artf257011 by pvanju
####

. /home/ediadmin/scripts/BUU_Scripts/EDI_EFT_FFM_MoveScript/props/BUU_Control.prop

TODAY=`date +%y%m%d`

cd $MOVESCRIPT_LOCATION

status=`/sbin/pidof -x $MOVESCRIPT_LOCATION/$SCRIPT_NAME`
echo status=$status
statusArray=($status)

if [ ${#statusArray[@]} -gt 1 ]; then
	echo "$(date): ================ Script $0 already running so this instance exiting. Process: $status  Count: ${#statusArray[@]}"
    exit 1
    echo "$(date): ================ Should have exited but didn't"
else
	echo count of statusArray=${#statusArray[@]}
fi


hostnameShort=`hostname | cut -d"." -f1 `


. ./System.prop
PROPERTIES_FILES_ARRAY=($PROPERTIES_FILES)
ENABLED_VAR=$ENABLED_VARIABLE
SRC_DEST_SEPERATOR=$SOURCE_DESTIONATION_SEPERATOR
#LOG_PATH_FILE=$SOURCE_LOG_PATH_FILE

ErrorMail () {
	ERRORSKIP=1
	echo $SENDNOTICEBODY
	if [ "$SENDTEXTTO" == "" ]; then
		$SENDNOTICEPATH/SendNotice_DataZone.sh -s "$LOCALHOSTNAME" -S "HIGH" "$SENDNOTICETO" "$SENDNOTICESUBJECT" "$SENDNOTICEBODY"
	else
		$SENDNOTICEPATH/SendNotice_DataZone.sh -t "$SENDTEXTTO" -m "$SENDTEXTMESSAGE" -s "$LOCALHOSTNAME" -S "HIGH" "$SENDNOTICETO" "$SENDNOTICESUBJECT" "$SENDNOTICEBODY"
	fi
}

echo "$(date):====== Starting $0 ======" 


for file in "${PROPERTIES_FILES_ARRAY[@]}"
do
	if [ "$file" == "props/EFT_moveBetweenEDI.prop" ]; then
		echo "$(date):------------Poperty file: $file Is Enabled: $IS_ENABLED"
		. ./$file

		enabled=$IS_ENABLED
		if [ "$enabled" == $ENABLED_VAR ];then
	
			INBOUNDPATH=$DESTINATION_ROOT_FOLDER_IN
	
			SOURCE_DEST_OUT=`echo $SOURCE_DESTINATION_FOLDER_MAPPING_OUT | cut -d$SOURCE_DESTIONATION_SEPERATOR -f1 `
			OUTBOUNDPATH=$SOURCE_ROOT_FOLDER_OUT/$SOURCE_DEST_OUT

			ZipFiles=`find $INBOUNDPATH -mindepth 1 -maxdepth 1 -type f `
			echo "$(date): opera zip files to process:"
			echo $ZipFiles | tr ' ' '\n'
			ZipFiles2=` echo $ZipFiles | tr ' ' '\n' | head -1 `

			for zipfile in $ZipFiles2
			do
				ERRORSKIP=0
				echo "$(date): processing $zipfile zip file"
				# store date and time stamp name from #zipfile
				zipfileNoPath=`basename $zipfile`
				zipDateTime=`echo $zipfileNoPath | cut -d"." -f3,4 `
				ENVIRONMENTSUFFIX=`echo $zipfileNoPath | cut -d"." -f5 `
				# create working directory
				WORKDIR="WorkDir_"
				wddone=0
				while [ $wddone -eq 0 ]
				do
					if [ -d $INBOUNDPATH/$WORKDIR$zipDateTime ]; then
						WORKDIR=$WORKDIR"1_"
					else
						wddone=1
					fi
				done
				echo "$(date): making $INBOUNDPATH/$WORKDIR$zipDateTime subdirectory "
				mkdir -p $INBOUNDPATH/$WORKDIR$zipDateTime
				echo "$(date): making $OUTBOUNDPATH/$WORKDIR$zipDateTime subdirectory "
				mkdir -p $OUTBOUNDPATH/$WORKDIR$zipDateTime
				# unzip file into working directory
				cd $INBOUNDPATH/$WORKDIR$zipDateTime
				echo "$(date): unziping $zipfile in $INBOUNDPATH/$WORKDIR$zipDateTime"
				unzip $zipfile
				returncode=$?
				# get OPERA file total from control sheet
				JournalFileName=`ls | grep ".J$" `
				# Update for artf257011---
				JournalDateTime=`echo $JournalFileName | cut -d"." -f3,4 ` 
				# Update for artf257011 done---
				OPERAJOURNALCNT=`tail -n +2 $JournalFileName | tr -d '\r'| grep -v "^$" | wc -l`
				echo "$(date): $zipfile contains $OPERAJOURNALCNT files in JOURNAL file $JournalFileName from OPERA"
				# get actual number of files
				OPERAFILES=`ls | grep -v $JournalFileName `
				OPERACOUNT=`echo $OPERAFILES | tr ' ' '\n' | grep -v $JournalFileName | wc -l `
				echo "$(date): $zipfile contains $OPERACOUNT actual files from OPERA"
				# do counts match
				# Update for artf257011---
				#if [ $OPERAJOURNALCNT -ne $OPERACOUNT ]; then
				#	# NO - error notification 
				#	SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile contained $OPERACOUNT files but expected $OPERAJOURNALCNT from journal.   Processing halted for this zipfile.   Manual intervention required."
				#	ErrorMail
				#fi
				ErrorCheckOne=0
				ErrorCheckTwo=0
				if [ $OPERAJOURNALCNT -ne $OPERACOUNT ]; then
					ErrorCheckOne=1
				fi
				if [ "$zipDateTime" != "$JournalDateTime" ]; then
					ErrorCheckTwo=1
				fi
				if [[ $ErrorCheckOne -eq 1 ]] && [[ $ErrorCheckTwo -eq 1 ]]; then
					# NO - error notification 
					SENDNOTICETO=$SENDERRORCHECKNOTICETO
					SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile contained $OPERACOUNT files but expected $OPERAJOURNALCNT from journal. The date and timestamp of Opera zip file $zipDateTime does not match the date and timestamp of Journal file $JournalDateTime. Processing halted for this zipfile.   Manual intervention required."
					ErrorMail
				elif [[ $ErrorCheckOne -eq 1 ]] && [[ $ErrorCheckTwo -eq 0 ]]; then
					SENDNOTICETO=$SENDERRORCHECKNOTICETO
					SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile contained $OPERACOUNT files but expected $OPERAJOURNALCNT from journal.   Processing halted for this zipfile.   Manual intervention required."
					ErrorMail
				elif [[ $ErrorCheckOne -eq 0 ]] && [[ $ErrorCheckTwo -eq 1 ]]; then
					SENDNOTICETO=$SENDERRORCHECKNOTICETO
					SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile -  The date and timestamp of Opera zip file $zipDateTime does not match the date and timestamp of Journal file $JournalDateTime. Processing halted for this zipfile.   Manual intervention required."
					ErrorMail
				fi
				# Update for artf257011 done---
				if [ $ERRORSKIP -eq 0 ]; then
					mv $INBOUNDPATH/$WORKDIR$zipDateTime/$JournalFileName $OUTBOUNDPATH/$WORKDIR$zipDateTime/
					# for each file - run through BUU process
					echo "$(date): Running translation.sh"
					$TRANSLATESCRIPT_LOCATION/translation.sh "XML" $TRANSLATESCRIPT_LOCATION $INBOUNDPATH/$WORKDIR$zipDateTime/ $OUTBOUNDPATH/$WORKDIR$zipDateTime/
					returncode=$?
					if [ $returncode -ne 0 ]; then
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile translation.sh errored on one or more files. Processing halted for this zipfile.   Manual intervention required."
						ErrorMail
					fi
				fi
				if [ $ERRORSKIP -eq 0 ]; then
					# generate MPEDI control file
					cd $OUTBOUNDPATH/$WORKDIR$zipDateTime
					echo "$(date): Running BUU_Report.sh "
					#$REPORTSCRIPT_LOCATION/BUU_Report.sh $TODAY
					capture=`$REPORTSCRIPT_LOCATION/BUU_Report.sh $OUTBOUNDPATH/$WORKDIR$zipDateTime/ $ENVIRONMENTSUFFIX 2>&1`
					returncode=$?
					if [ $returncode -ne 0 ]; then
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile BUU_REPORT.sh errored. Processing halted for this zipfile.   Manual intervention required."
						ErrorMail
					fi
				fi
				if [ $ERRORSKIP -eq 0 ]; then
					# verify MPEDI control file
					cd $OUTBOUNDPATH/$WORKDIR$zipDateTime
					ReportFileName=` echo $capture | tr ' ' '\n' | grep EDIFullFile | cut -d"=" -f2 `
					MPEDIJOURNALCNT=`tail -n +2 $ReportFileName | tr -d '\r' | grep -v "^$" | wc -l `
					echo ReportFileName=$ReportFileName    contents=
					cat $ReportFileName
					echo finish
					echo "$(date): $zipfile contains $MPEDIJOURNALCNT files in JOURNAL file created by MPEDI"
					MPEDIJOURNALGOOD=`tail -n +2 $ReportFileName | tr -d '\r' | grep -v "^$" | cut -d"|" -f4,5 `
					MPEDIJOURNALGOODCNT=0
					MPEDIJOURNALBADCNT=0
					MPEDIJOURNALBOTHCNT=0
					for goodbad in $MPEDIJOURNALGOOD
					do
						good=`echo $goodbad | cut -d"|" -f1 `
						bad=`echo $goodbad | cut -d"|" -f2 `
						if [[ "$good" != "" ]] && [[ "$good" != "0" ]]; then
							MPEDIJOURNALGOODCNT=$(( $MPEDIJOURNALGOODCNT + 1 ))
						elif [[ "$bad" != "" ]] && [[ "$bad" != "0" ]]; then
							MPEDIJOURNALBADCNT=$(( $MPEDIJOURNALBADCNT + 1 ))
						fi
						MPEDIJOURNALBOTHCNT=$(( $MPEDIJOURNALBOTHCNT + 1 ))
					done
					echo MPEDIJOURNALGOODCNT=$MPEDIJOURNALGOODCNT
					echo MPEDIJOURNALBADCNT=$MPEDIJOURNALBADCNT
					echo MPEDIJOURNALBOTHCNT=$MPEDIJOURNALBOTHCNT
					# get actual number of files
					PROD=`echo $zipfileNoPath | cut -d"." -f1-2 `
					ZIPPREFIX=$PROD
					REPORTPREFIX=$PROD
					PROD=`echo $PROD | cut -d"." -f1 `
					# Begin 2017 codes
                    if [ $PROD == "OCFQ" ]; then
                        ZIPPREFIX='EDI.ICFQ'
                        REPORTPREFIX='EDI.IBRQ'
                    elif [ $PROD == "OCFP" ]; then
                        ZIPPREFIX='EDI.ICFP'
                        REPORTPREFIX='EDI.IBRP'
                    # end 2017 codes
					elif [ $PROD == "O834BU" ]; then
                        ZIPPREFIX='EDI.I834BU'
                        REPORTPREFIX='EDI.IBUR'
					elif [ $PROD == "O834BP" ]; then
						ZIPPREFIX='EDI.I834BP'
						REPORTPREFIX='EDI.IBURP'
					elif [ $PROD == "OCF15" ]; then
						ZIPPREFIX='EDI.ICF15Q'
						REPORTPREFIX='EDI.IBR15'
					elif [ $PROD == "OCF15P" ]; then
						ZIPPREFIX='EDI.ICF15P'
						REPORTPREFIX='EDI.IBR15P'
					# begin 2016 codes
					elif [ $PROD == "OCF16" ]; then
						ZIPPREFIX='EDI.ICF16Q'
						REPORTPREFIX='EDI.IBR16Q'
					elif [ $PROD == "OCF16Q" ]; then
						ZIPPREFIX='EDI.ICF16Q'
						REPORTPREFIX='EDI.IBR16Q'
					elif [ $PROD == "OCF16P" ]; then
						ZIPPREFIX='EDI.ICF16P'
						REPORTPREFIX='EDI.IBR16P'
					# end 2016 codes
					fi
					MPEDIXMLFILESCOUNT=`ls $OUTBOUNDPATH | grep -v $REPORTPREFIX |grep -v $ReportFileName| grep -v $JournalFileName | grep -v $WORKDIR$zipDateTime| wc -l `
					echo "$(date): Zipfile $zipfile will contain $MPEDIXMLFILESCOUNT BUU files created by MPEDI"
					# do counts match
					# valid files match actual files
					# NO - error notification - goto next zip file
					if [[ $MPEDIJOURNALGOODCNT -ne $MPEDIXMLFILESCOUNT ]] || [[ $MPEDIJOURNALGOODCNT -eq 0 ]]; then
						# NO - error notification - goto next zip file
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile MPEDI created $MPEDIXMLFILESCOUNT files but expected $MPEDIJOURNALGOODCNT from MPEDI journal.   Processing halted for this zipfile.   Manual intervention required."
						ErrorMail
					fi
					# valid file cnt + invalid file cnt = Opera file count
					# NO - error notification - goto next zip file
					if [ $MPEDIJOURNALBOTHCNT -ne $OPERAJOURNALCNT ]; then
						# NO - error notification - goto next zip file
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile MPEDI Journal contains $MPEDIJOURNALBOTHCNT files but expected $OPERAJOURNALCNT from OPERA journal.   Processing halted for this zipfile.   Manual intervention required."
						ErrorMail
					fi
					MPEDIJOURNALCALCCNT=$(( $MPEDIJOURNALGOODCNT + $MPEDIJOURNALBADCNT ))
					if [ $MPEDIJOURNALBOTHCNT -ne $MPEDIJOURNALCALCCNT ]; then
						# NO - error notification - goto next zip file
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile MPEDI Journal contains $MPEDIJOURNALBOTHCNT records but calculated total (sum of good and bad) is $MPEDIJOURNALCALCCNT.   Processing halted for this zipfile.   Manual intervention required."
						ErrorMail
					fi
				fi
				if [ $ERRORSKIP -eq 0 ]; then
					# zip up outbound files, opera control, and MPEDI control 
					# with zip filename = zipofile info
					# move outbound zip file to $INBOUNDPATH
					ZIPFILENAME="$ZIPPREFIX.$zipDateTime.$ENVIRONMENTSUFFIX"
					echo "$(date): Zipping output files into $OUTBOUNDPATH/$ZIPFILENAME "
					cd $OUTBOUNDPATH/
					ZIPFiles=`ls | grep -v $ReportFileName | grep -v $JournalFileName | grep -v $REPORTPREFIX | grep -v $WORKDIR$zipDateTime `
					for zipfileLP in $ZIPFiles
					do	
						zip -r $OUTBOUNDPATH/$ZIPFILENAME $zipfileLP
						returncode=$?
						if [ $returncode -ne 0 ]; then
							SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile ERROR in zipping $zipfileLP.   Manual intervention required."
							ErrorMail
						else
							rm $zipfileLP
						fi
					done
					cd $OUTBOUNDPATH/$WORKDIR$zipDateTime
					zip -r $OUTBOUNDPATH/$ZIPFILENAME $JournalFileName
					returncode=$?
					if [ $returncode -ne 0 ]; then
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile ERROR in zipping $JournalFileName.   Manual intervention required."
						ErrorMail
					fi
					#ReportDate=`echo $ReportFileName | cut -d"_" -f2 | cut -c8-13 `
					#ReportTime=`echo $ReportFileName | cut -d"_" -f2 | cut -d"." -f1 | cut -c14- `
					#ReportDateTime="D$ReportDate.T$ReportTime`date +%j `"
					#NewReportFileName="$REPORTPREFIX.$ReportDateTime.J"
					NewReportFileName="$ZIPPREFIX.$zipDateTime.J"
					mv $ReportFileName $NewReportFileName
					zip -r $OUTBOUNDPATH/$ZIPFILENAME $NewReportFileName
					returncode=$?
					if [ $returncode -ne 0 ]; then
						SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile ERROR in zipping $ReportFileName.   Manual intervention required."
						ErrorMail
					fi
				fi
				# cleanup
				rm $zipfile
				echo "$(date): remove $OUTBOUNDPATH/$WORKDIR$zipDateTime subdirectory "
				rm -r $OUTBOUNDPATH/$WORKDIR$zipDateTime
				returncode=$?
				if [ $returncode -ne 0 ]; then
					SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile Unable to remove $OUTBOUNDPATH/$WORKDIR$zipDateTime directory."
					ErrorMail
				fi
				echo "$(date): remove $INBOUNDPATH/$WORKDIR$zipDateTime subdirectory "
				rm -r $INBOUNDPATH/$WORKDIR$zipDateTime
				returncode=$?
				if [ $returncode -ne 0 ]; then
					SENDNOTICEBODY="$(date): $LOCALHOSTNAME: Zipfile $zipfile Unable to remove $INBOUNDPATH/$WORKDIR$zipDateTime directory."
					ErrorMail
				fi
				if [ $ERRORSKIP -ne 0 ]; then
					# send reports anyway; # rm $REPORTLOCATION/* 2>/dev/null
					rm $OUTBOUNDPATH/* 2>/dev/null
					echo "$(date):ERRORS occured during processing - ABORTING - Manual processing required!"
					#exit 1
				fi
			done
			sleep 4m  
			# do not change Value needed is 4m
		else
			echo "$(date):Not Enabled" 
		fi
	fi
done

echo "$(date):====== Complete $0 ======" 
exit $RETURNCODE

