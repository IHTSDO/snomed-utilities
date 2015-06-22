#!/bin/bash
set -e

function findEffectiveTime() {

searchDir=$1
ls -1 ${searchDir}/sct2*Sta*Sn* | sed  's/.*\([0-9]\{8\}\).*/\1/'
}

echo
echo "Replace currently redundant relationships in the Stated Relationship File"
echo
default="/Users/Peter/tmp/20150731_flat"
read -p "What directory are your snapshot relationship files in? [${default}] " sourceDir
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

mvn clean install -P stand-alone
executable=`ls -1 target/uber*.jar`
if [ -z "${executable}" ]
then
  echo "Failed to find executable jar in target directory"
  exit -1
fi
set -x;
java -Xms4g -Xmx5g -classpath ${executable}  org.ihtsdo.snomed.util.rf2.RelationshipProcessor ${sourceDir}/sct2_StatedRelationship_Snapshot_INT_${effectiveTime}.txt ${sourceDir}/sct2_Relationship_Snapshot_INT_${effectiveTime}.txt target/sct2_StatedRelationship_Snapshot_INT_{effectiveTime}.txt


