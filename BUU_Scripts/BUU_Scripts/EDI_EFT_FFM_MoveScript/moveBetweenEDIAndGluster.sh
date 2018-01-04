#!/bin/sh

SCRIPT_LOCATION="/home/ediadmin/scripts/BUU_Scripts/EDI_EFT_FFM_MoveScript"
cd $SCRIPT_LOCATION

#SCRIPT_NAME="moveBetweenEDIAndGluster.sh"
SCRIPT_NAME="BUU_Scripts\/EDI_EFT_FFM_MoveScript\/moveBetweenEDIAndGluster.sh"

#status=`/sbin/pidof -x $SCRIPT_LOCATION/$SCRIPT_NAME`
#statusArray=($status)

#if [ ${#statusArray[@]} -gt 1 ]; then
#	echo "$(date): ================ Script already running so this instance exiting. Process: $status  Count: ${#statusArray[@]}"
#    exit 1
#    echo "$(date): ================ Should have exited but didn't"
#fi
#status=`/sbin/pidof -x $SCRIPT_LOCATION/$SCRIPT_NAME`
status=`ps -ef | grep $SCRIPT_NAME `
echo status=$status
status=`echo $status | tr ' ' '\n' | grep $SCRIPT_NAME | wc -l `
echo status=$status
statusArray=($status)

#if [ ${#statusArray[@]} -gt 1 ]; then
#if [ $status -gt 4 ]; then
if [ $status -gt 2 ]; then
        echo "$(date): ================ Script already running so this instance exiting. Process: $status  Count: ${#statusArray[@]}"
        exit 1
        echo "$(date): ================ Should have exited but didn't"
else
        echo "$(date): ================ Script starting. Process: $status  Count: ${#statusArray[@]}"
fi


ORIGumask=`umask -p`
OUTFlag=0
hostnameShort=`hostname | cut -d"." -f1 `


. ./System.prop
PROPERTIES_FILES_ARRAY=($PROPERTIES_FILES)
ENABLED_VAR=$ENABLED_VARIABLE
SRC_DEST_SEPERATOR=$SOURCE_DESTIONATION_SEPERATOR
#LOG_PATH_FILE=$SOURCE_LOG_PATH_FILE

echo "$(date):====== Poperty file: $PROPERTIES_FILES  Process: $status ======" 2>&1 

## function to move files
moveFiles(){
	src_root="$1"
	dest_root="$2"
	src_dest="$3"
	arc_loc="$4"
	zip_at_dest="$5"

	echo "$(date):Source Destination configuration: $src_dest"  2>&1

	src=${src_dest%$SRC_DEST_SEPERATOR*}

	dest=${src_dest#*$SRC_DEST_SEPERATOR}
		
	srcCHECK=`echo $src | grep "|" `
	fileCount=`ls -1 $src_root/$src/ |wc -l`
	if [[ $srcCHECK == "" ]]; then
		foundfiles=`find $src_root/$src -mindepth 1 -maxdepth 1 -type f -not -name ".*" -cmin +3 -print`
	else	
		srcFILE=${src#*|}
		srcPATH=${src%|*}
		foundfiles=`find $src_root/$srcPATH -mindepth 1 -maxdepth 1 -type f -not -name ".*" -name "$srcFILE" -cmin +3 -print`
	fi
	fileCount=`echo $foundfiles | wc -w`
	echo "$(date):Moving files from $src_root/$src to $dest_root/$dest. Files moved $fileCount"  2>&1
#	mv $src_root/$src/*  $dest_root/$dest/
	for mv_fileNM in $foundfiles
	do
		echo "$(date):Moving file $mv_fileNM to $dest_root/$dest. "  2>&1
		if [ $OUTFlag -eq 1 ]; then
			if [ ! -z $arc_loc ];then
				echo "$(date) Archiving the file $mv_fileNM to $arc_loc/."
				$ORIGumask
				if [ ! -d $arc_loc ]; then
					mkdir -p $arc_loc 2>&1
				fi
				umask 117
   				cp $mv_fileNM $arc_loc/  2>&1
			fi

			if [ $zip_at_dest == "TRUE" ]; then
				echo "$(date) Zipping the file."
				$ORIGumask
				zip -j $mv_fileNM.zip $mv_fileNM
				rm $mv_fileNM
				mv $mv_fileNM.zip $mv_fileNM
				umask 117
			fi

			#cp $mv_fileNM $dest_root/$dest/  2>&1
			fileName=$(basename $mv_fileNM)
            cp $mv_fileNM $dest_root/$dest/.$fileName  2>&1
            mv $dest_root/$dest/.$fileName $dest_root/$dest/$fileName 2>&1			

			TEMPRETURNCODE=$?
			if [[ $TEMPRETURNCODE -eq 0 ]]; then
				rm $mv_fileNM  2>&1
				TEMPRETURNCODE=$?
				if [ $TEMPRETURNCODE -ne 0 ]; then
					RETURNCODE=-10
				fi
			else
				RETURNCODE=-11
			fi
		else
			if [ ! -z $arc_loc ];then
                echo "$(date) Archiving the file $mv_fileNM to $arc_loc/."
				if [ ! -d $arc_loc ]; then
					mkdir -p $arc_loc 2>&1
				fi
          		cp $mv_fileNM $arc_loc/  2>&1
       		fi

       		if [ $zip_at_dest == "TRUE" ]; then
           		echo "$(date) Zipping the file."
          		zip -j $mv_fileNM.zip $mv_fileNM
           		rm $mv_fileNM
           		mv $mv_fileNM.zip $mv_fileNM
       		fi

			mv $mv_fileNM $dest_root/$dest/  2>&1
			TEMPRETURNCODE=$?
			if [ $TEMPRETURNCODE -ne 0 ]; then
				RETURNCODE=-12
			fi
		fi
	done
}


for file in "${PROPERTIES_FILES_ARRAY[@]}"
do
	echo "$(date):------------Poperty file: $file Is Enabled: $IS_ENABLED"  2>&1
	. ./$file

	enabled=$IS_ENABLED
	if [ "$enabled" == $ENABLED_VAR ];then

		SRC_ROOT_IN=$SOURCE_ROOT_FOLDER_IN
		DESR_ROOT_IN=$DESTINATION_ROOT_FOLDER_IN
		ZIP_AT_DESTINATION_IN=$ZIP_AT_DESTINATION_IN		

		SOURCE_DEST_IN_ARRAY=($SOURCE_DESTINATION_FOLDER_MAPPING_IN)
		ARCHIVAL_IN_ARRAY=($ARCHIVE_LOCATION_IN)

		counter=0
		for src_dest_str in "${SOURCE_DEST_IN_ARRAY[@]}"
		do
			archival_location=${ARCHIVAL_IN_ARRAY[$counter]}
			moveFiles "$SRC_ROOT_IN" "$DESR_ROOT_IN" "$src_dest_str" "$archival_location" "$ZIP_AT_DESTINATION_IN"
			
			counter=$(( $counter + 1 ))
		done
		if [ "$BUU_PROCESS" != "" ]; then
			$BUU_PROCESS 2>&1
		fi

		SRC_ROOT_OUT=$SOURCE_ROOT_FOLDER_OUT
		DESR_ROOT_OUT=$DESTINATION_ROOT_FOLDER_OUT
		ZIP_AT_DESTINATION_OUT=$ZIP_AT_DESTINATION_OUT

		SOURCE_DEST_OUT_ARRAY=($SOURCE_DESTINATION_FOLDER_MAPPING_OUT)
		ARCHIVAL_OUT_ARRAY=($ARCHIVE_LOCATION_OUT)

		umask 117
		OUTFlag=1
		
		counter=0
		for src_dest_str in "${SOURCE_DEST_OUT_ARRAY[@]}"
		do
			archival_location=${ARCHIVAL_OUT_ARRAY[$counter]}
			moveFiles "$SRC_ROOT_OUT" "$DESR_ROOT_OUT" "$src_dest_str" "$archival_location" "$ZIP_AT_DESTINATION_OUT"
			
			counter=$(( $counter + 1 ))
		done
		
		$ORIGumask
		OUTFlag=0
	
	else
		echo "$(date):Not Enabled"  2>&1
	fi
done

echo "$(date):====== END: $PROPERTIES_FILES Process: $status ======"  2>&1

exit $RETURNCODE


