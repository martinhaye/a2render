#!/usr/bin/env python

# This wrapper script takes everything in the 'files' directory and puts it
# into image file 'platform/LL.2mg'.

import os, re, subprocess, sys
from os.path import join as pjoin

# No matter where we run from, use the right directories.
binDir = os.path.dirname(sys.argv[0])
if not os.path.isabs(binDir):
  binDir = pjoin(os.getenv("PWD", os.getcwd()), binDir)
binDir = re.sub("\/?\.?$", "", binDir)
mainDir = os.path.dirname(binDir) # binDir/..


################################################################################
def main():
  """ Command-line driver. """

  os.chdir(mainDir)
  javaCmd = ['java', 
             '-cp', 'a2copy/lib/ac.jar:a2copy/a2copy.jar',
             'A2copy',
             '-create', 'platform/LL.2mg', 'files/']
  proc = subprocess.Popen(javaCmd)
  proc.communicate()
  sys.exit(proc.returncode)


################################################################################
main()
