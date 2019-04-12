#!/bin/bash
if [ "$#" -ne 1 ]; then
    echo "usage: add-osbs.sh <the url of the bxms-jenkins git repository>"
    exit 0
fi
git clone $1
rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-osbs/ansible/ ansible
