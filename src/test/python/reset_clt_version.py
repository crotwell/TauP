#!/usr/bin/env python


import os
import shutil
import subprocess

ignore_ver_svg='--ignore-matching-lines=<version>'
ignore_ver_gmt='--ignore-matching-lines=# version'

cmdTestDir='src/test/resources/edu/sc/seis/TauP/cmdLineTest'
docExampleDir='src/doc/sphinx/source/examples'
buildTestDir='build/cmdLineTest'
versionOnly=[]
otherDiff=[]
dirsToCheck = [ cmdTestDir, docExampleDir]
for dirToCheck in dirsToCheck:
    for root, dirs, files in os.walk(dirToCheck):
        for name in files:

            outpath=os.path.join(root, name)
            result = subprocess.run(["git", "ls-files", "--error-unmatch", outpath ],
                                    capture_output=True)
            if result.returncode != 0:
                #print("not known to git")
                continue

            result = subprocess.run(["git", "diff", "-U0", outpath],
                             capture_output=True)
            gitalldiff = result.stdout.decode("UTF-8").strip()
            if len(gitalldiff) == 0:
                #print("no differences")
                continue
            cmd=["git", "diff", "-U0", ignore_ver_svg, ignore_ver_gmt, outpath]
            result = subprocess.run(cmd,
                                     capture_output=True)
            if result.returncode != 0:
                raise Error(f"Subprocess did not complete successfully: {name}")
            gitdiff = result.stdout.decode("UTF-8").strip()
            if len(gitdiff) == 0:
                # only version differences
                result = subprocess.run(["git", "restore", outpath])

                versionOnly.append(name)
            else:
                otherDiff.append(name)

        break # no subdirs

print("Larger differences:")
print()
for f in otherDiff:
    print(f"  {f}")
print()
print("Version Diff Only:")
print()
for f in versionOnly:
    print(f"  {f}")
