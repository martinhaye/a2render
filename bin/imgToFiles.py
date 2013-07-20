#!/usr/bin/env python

# This wrapper script copies out the files from 'platform/LL.2mg' into
# the 'files' directory, replacing everything that was in 'files'.

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
             '-extract', 'platform/LL.2mg', 'files/']
  proc = subprocess.Popen(javaCmd)
  proc.communicate()
  sys.exit(proc.returncode)


################################################################################
main()
