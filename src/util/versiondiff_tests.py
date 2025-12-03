#!/usr/bin/env python

from onlyversiondiff import checkDir
import sys
from pathlib import Path

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
    return 0

if __name__ == "__main__":
    sys.exit(main())
