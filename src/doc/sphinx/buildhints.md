
See conf.py for more options and the definition of template patterns like
|release| and |version|.

conda create -n sphinx python=3.13
conda activate sphinx
pip install pip-tools
cd src/doc/sphinx && pip-compile requirements.in  > requirements.txt
pip install -r requirements.txt

make html
