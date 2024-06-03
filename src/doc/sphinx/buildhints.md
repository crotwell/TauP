
conda create -n sphinx python=3.12
conda install sphinx
pip install --upgrade myst-parser

pip install pip-tools
cd src/doc/sphinx && pip-compile requirements.in
