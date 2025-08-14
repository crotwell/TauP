
See conf.py for more options and the definition of template patterns like
|release| and |version|.

conda create -n sphinx python=3.13
conda install sphinx
pip install --upgrade myst-parser sphinxcontrib-bibtex pip-tools
cd src/doc/sphinx && pip-compile requirements.in  > requirements.txt
