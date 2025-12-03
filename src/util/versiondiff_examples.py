#!/usr/bin/env python

from onlyversiondiff import checkDir
import sys
from pathlib import Path

def main():
    d = "docExamples"
    newdir = Path(f"build/{d}")
    olddir = Path(f"src/doc/sphinx/source/examples")
    checkDir(newdir, olddir)


    print("Done!")
    return 0

if __name__ == "__main__":
    sys.exit(main())
