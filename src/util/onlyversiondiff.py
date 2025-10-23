#!/usr/bin/env python

"""
Fix output files were only difference is the version line.
"""
from pathlib import Path
import difflib
import shutil

def isOnlyVersion(diff_lines, filename):
    for line in diff_lines:
        if line.startswith("  "):
            continue
        if line.startswith("- ") or line.startswith("+ "):
            # some difference:
            line = line[2:]
            line = line.strip()
            if line.startswith("# version") \
                    or line.startswith("\"version>\":") \
                    or line.startswith("<version>"):
                continue
            else:
                nonVersionDiff = True
                return (True, line)
    return (False, None)

def checkFile(filename, newdir, olddir):
    newFile = Path(newdir, filename)
    oldFile = Path(olddir, filename)
    if not newFile.exists():
        print(f"new missing, unlink {filename}")
        oldFile.unlink(missing_ok=True)
    elif not oldFile.exists():
        print(f"old missing, copy to {filename}")
        shutil.copy(newFile, olddir)
    else:
        if newFile.is_file() and oldFile.is_file():
            with open(newFile, "r") as inNew:
                newLines = inNew.readlines()
            with open(oldFile, "r") as inOld:
                oldLines = inOld.readlines()
            difflines = difflib.ndiff(newLines, oldLines)
            (actualDiff, line) = isOnlyVersion(difflines, filename)
            if actualDiff:
                print(f"Found diff in {filename}: {line}")
                shutil.copy(newFile, olddir)
        else:
            print(f"skip, not a file: {filename}")
            return
def checkDir(newdir, olddir):
    allFiles = set()
    for f in olddir.iterdir():
        allFiles.add(f.name)
    for f in newdir.iterdir():
        allFiles.add(f.name)
    for filename in allFiles:
        #print(file.name)
        checkFile(filename, newdir, olddir)

def main():
    d = "cmdLineTest"
    newdir = Path(f"build/{d}")
    olddir = Path(f"src/test/resources/edu/sc/seis/TauP/{d}")
    checkDir(newdir, olddir)

    d = "cmdLineHelp"
    newdir = Path(f"build/{d}")
    olddir = Path(f"src/doc/sphinx/source/{d}")
    checkDir(newdir, olddir)

    print("Done!")

if __name__ == "__main__":
    main()
