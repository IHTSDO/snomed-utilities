#!/bin/bash
set -e

function findEffectiveTime() {

searchDir=$1
ls -1 ${searchDir}/sct2*Sta*Sn* | sed  's/.*\([0-9]\{8\}\).*/\1/'
}

while getopts ":dsp:" opt
do
	case $opt in
		d) 
			debugMode=true
			echo "Option set to start API in debug mode."
			debugFlags="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -Djava.compiler=NONE" 
		;;
		s) 
			skipMode=true
			echo "Option set to skip build."
		;;
		help|\?)
			echo -e "Usage: [-d]  [-p <port>]"
			echo -e "\t d - debug. Starts the program in debug mode, which an IDE can attach to on port 8000"
			echo -e "\t s - skip.  Skips the build"
			exit 0
		;;
	esac
done

echo
echo "Interactive MRCM"
echo
default="/Users/Peter/tmp/20160131_flat"
read -p "Where are your snapshot SNOMED files (? [${default}] " sourceDir
if [ -z "${sourceDir}" ]
then
  sourceDir=${default}
fi

effectiveTime=$(findEffectiveTime ${sourceDir})

if [ -z "${effectiveTime}" ] 
then
  echo "Failed to find Stated Relationship file with effective time"
  exit -1
fi

echo "Loading files from ${sourceDir} with effective time ${effectiveTime}"

if [ -z "${skipMode}" ] 
then 
  mvn clean install -P stand-alone
fi

executable=`ls -1 target/uber*.jar`
if [ -z "${executable}" ]
then
  echo "Failed to find executable jar in target directory"
  exit -1
fi
#set -x;
today=`date +%Y%m%d`
java -Xms4g -Xmx5g -enableassertions -classpath ${executable}  org.ihtsdo.snomed.util.mrcm.MrcmInteractiveMenu \
${sourceDir}/sct2_Concept_Snapshot_INT_${effectiveTime}.txt \
${sourceDir}/sct2_StatedRelationship_Snapshot_INT_${effectiveTime}.txt \
${sourceDir}/sct2_Relationship_Snapshot_INT_${effectiveTime}.txt \
${sourceDir}/sct2_Description_Snapshot-en_INT_${effectiveTime}.txt \
/Users/Peter/tmp/20160131_rf1_flat/sct1_Relationships_Core_INT_20160131.txt 


