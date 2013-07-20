#!/usr/bin/env python

import os, re, subprocess, sys
from os.path import join as pjoin

# This is a simple wrapper that calls the Java a2copy code.

# No matter where we run from, use the right directories.
binDir = os.path.dirname(sys.argv[0])
if not os.path.isabs(binDir):
  binDir = pjoin(os.getenv("PWD", os.getcwd()), binDir)
binDir = re.sub("\/?\.?$", "", binDir)
mainDir = os.path.dirname(binDir) # binDir/..


################################################################################
def main():
  """ Command-line driver. """

  javaCmd = ['java', 
             '-cp', '%s/a2copy/lib/ac.jar:%s/a2copy/a2copy.jar' % (mainDir, mainDir),
             'A2copy']
  javaCmd.extend(sys.argv[1:])
  proc = subprocess.Popen(javaCmd, stdout = sys.stdout, stderr = sys.stderr)
  proc.communicate()
  sys.exit(proc.returncode)


################################################################################
main()
