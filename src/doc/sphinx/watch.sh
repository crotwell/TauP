#!/bin/zsh

# compile once to start
make html
date
echo "OK"

# recompile on file change within 1 seconds, except version.ts
fswatch -o -l 1 --exclude version.ts  source | while read stuff; do
  echo $stuff
  #SPHINXOPTS='-D html_theme_options.nosidebar=True' make html htmlhelp
  make html htmlhelp

  date
  echo "OK"
done
