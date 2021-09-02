#!/bin/sh

####################################################################################################################################################
####################################################################################################################################################
####                                                                                                                                            ####
####                This script runs the maven clean install and then moves the jar file and the folder with                                    ####
####                the dependencies to the location where the installed plugins normally reside in the userdir                                 ####
####                of the designer. This means no manual uninstall and install via the plugins window of the designer                          ####
####                                                                                                                                            ####
####################################################################################################################################################
####################################################################################################################################################

# name of the jar file to be moved. Example of the git plugin:
# jarFile="de-adito-git-adito-nbm-git.jar"
jarFile=""
# path to the jar file above, from the location of this script. Example of the git plugin (Note that the git plugin builds the nbm in a submodule,
# path probably starts with target/...):
# jarPath="nbm/target/nbm/netbeans/extra/modules/"
jarPath=""
# the path to the folder in which the jar should be moved. Example of the git plugin:
# jarTargetPath="../0.0/workingdir/nbp_userdir/modules/"
jarTargetPath=""
# name of the folder that contains the gathered dependencies of the plugin. These have to be moved as well. Example of the git plugin:
# folderName="de.adito.git.adito-nbm-git/"
folderName=""
# path to the folder containing the gathered dependencies, as seen from the location of the script. Example of the git plugin:
# folderPath="nbm/target/nbm/netbeans/extra/modules/ext/"
folderPath=""

if test -z "$jarFile" || test -z "$jarPath" || test -z "$jarTargetPath" || test -z "$folderName" || test -z "$folderPath"
then
  echo "Variables for the file paths not set up, aborting the job. Please fill in the variables in the script"

else
targetPathFolder="ext/"
jarFilePath=$jarPath$jarFile
jarFileTargetPath=$jarTargetPath$jarFile
folderNamePath=$folderPath$folderName
folderTargetPath=$jarTargetPath$targetPathFolder

JAVA_HOME="C:/Program Files/Java/open_jdk-13" mvn clean install -T 1C -P adito.m2

cp $jarFilePath $jarFileTargetPath
cp -r $folderNamePath $folderTargetPath

fi

