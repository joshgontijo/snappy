#!/bin/bash

FILE1=$1
if [[ -z "$1" ]]
  then
    echo "No version supplied"
    exit 1
fi

echo "Releasing version $1"

mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
mvn clean install

git commit -a -m "Release $1"
git push origin master

git tag -a $1 -m "Release $1"
git push origin $1

mvn clean deploy -P release
mvn nexus-staging:release -P release

# Note: Be sure that the local Maven settings.xml file is configured with the right credentials, and that the system has the right GPG keys installed (along with a valid GPG installation)