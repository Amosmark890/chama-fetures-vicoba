#!/bin/sh
# Requirement For Windows
#  1. Versioned Project with git,svn. 
#  2. Download kiuwan application
#  3. Username and password for kiuwan and svn
#  4. Enable CLI in  tortoise
#  5. pushsvn.sh file
#  6. Use git bash

# Steps 
#  1. Add both kiuwan and svn credentials in windows environment 
#  2. put pushsvn.sh file on the root directory of  your project
#  3. On git bash terminal execute this file with git/svn comment as --- sh pushsvn.sh "comment"
#  4. Hit Enter
#  3. Grab a cup of coffee, that single command automates the code versioning process
#DEVELOPERS  DAVID MANDUKU AND OSCAR MUIGAI


#Kiuwan analyzer location/path

alias agent='C:/Kiuwan/bin/agent.cmd'

CI_PROJECT_DIR=$(pwd)

COMMENT_ARGUMENT="$1"
 #  rm -rf "svnD"
 # svn -username=$SVN_USERNAME --password=$SVN_PASSWORD
 # kiuwan -user $KIUWAN_USER --pass $KIUWAN_PASSWD


#svn status
#
#svn  add * --force
#
## get credentials from windows environment
#svn commit -m "$COMMENT_ARGUMENT" --username=$SVN_USERNAME --password=$SVN_PASSWORD

git add .

git commit -m "$COMMENT_ARGUMENT"

#getting remote name
#Get git branch - git branch -v
for OUTPUT in $(git remote -v | grep -w "fetch" | awk '{print $1}')
do
	echo $OUTPUT
	git push  $OUTPUT

done

#PUSHING CODE TO KIUWAN

##getting folder name
#CI_PROJECT_DIR=$(pwd)
#basename "$CI_PROJECT_DIR"
#folderName="$(PWD | sed 's!.*/!!')"
#
##this method checks the project type(android or ios app)
agent -n "Java Apps" -s "$CI_PROJECT_DIR/ChamaPayments/src/main/java" -l "devops__dcb_vicoba_payments_service" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
agent -n "Java Apps" -s "$CI_PROJECT_DIR/Chama-Authorization-server/src/main/java" -l "devops__dcb_vicoba_auth-server" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
agent -n "Java Apps" -s "$CI_PROJECT_DIR/APIGateway/src/main/java" -l "devops__dcb_vicoba_gateway" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
agent -n "Java Apps" -s "$CI_PROJECT_DIR/Chama-Eureka-server/src/main/java" -l "devops__dcb_vicoba_eureka" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
agent -n "Java Apps" -s "$CI_PROJECT_DIR/ChamaKYC/src/main/java" -l "devops__dcb_vicoba_kyc_service" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
agent -n "Java Apps" -s "$CI_PROJECT_DIR/Notifications/src/main/java" -l "devops__dcb_vicoba_notifications_service" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
agent -n "Java Apps" -s "$CI_PROJECT_DIR/Chama-Poll/src/main/java" -l "devops__dcb_vicoba_polls" -c --user $KIUWAN_USER --pass $KIUWAN_PASSWD
