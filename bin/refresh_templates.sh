#!/bin/bash

BASEDIR=$1
if [ "x$BASEDIR" = "x" ] ; then
  echo "No template git repo specified. Usage: $0 <template-git-repo>"
  exit 1
fi

cd $BASEDIR
git fetch
git rebase
