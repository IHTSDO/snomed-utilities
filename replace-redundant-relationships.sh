#!/bin/bash
set -e

function findEffectiveTime() {

searchDir=$1
ls -1 ${searchDir}/*Sta*Sn* | sed  's/.*\([0-9]\{8\}\).*/\1/'
}

echo
echo "Replace currently redundant relationships in the Stated Relationship File"
echo

read -p "What directory are your snapshot relationship files in? [/Users/Peter/tmp/20150131_flat/] " sourceDir
if [ -z "${sourceDir}" ]
then
  sourceDir="/Users/Peter/tmp/20150131_flat/"
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

java -classpath ${executable}  org.ihtsdo.snomed.util.rf2.RelationshipProcessor ${sourceDir}/sct2_StatedRelationship_Snapshot_INT_${effectiveTime}.txt ${sourceDir}/sct2_Relationship_Snapshot_INT_${effectiveTime}.txt target/sct2_StatedRelationship_Snapshot_INT_{effectiveTime}.txt


