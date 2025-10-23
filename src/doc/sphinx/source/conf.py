# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

import json
with open("cmdLineHelp/VERSION.json", "r") as vf:
    verjson = json.load(vf)

with open("zenodo_id_num.txt", "r") as vf:
    zenodo_doi_text = vf.read().strip()
zenodo_url = f"https://doi.org/10.5281/zenodo.{zenodo_doi_text}"

release_year = verjson['date'][0:4]

# create text substitutions
rst_epilog = f'''
.. |fullversion| replace:: {verjson['version']}
.. |release_year| replace:: {release_year}
.. |zenodo_all_id_num| replace:: 10794857
.. |zenodo_id_num| replace:: {zenodo_doi_text}
.. |zenodo_url| replace:: Zenodo
.. _zenodo_url: {zenodo_url}
.. |dist_zip| replace:: TauP-{verjson['version'][0:5]}.zip
.. |zenodo_doi| replace:: 10.5281/zenodo.{zenodo_doi_text}
'''

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'TauP'
copyright = f'{release_year}, Philip Crotwell'
author = 'Philip Crotwell'
version = verjson['version'][0:3]
release = verjson['version']
#today = verjson['date']

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = ['myst_parser', 'sphinxcontrib.bibtex']

templates_path = ['_templates']
exclude_patterns = []

bibtex_bibfiles = ['refs_taup.bib']

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'alabaster'
html_static_path = ['_static']

html_theme_options = {
    'logo': 'taupLogo.svg',
    'description': f'Flexible Seismic Travel-Time and Raypath Utilities. Version {verjson["version"]}',
    'fixed_sidebar': "true",
    'github_user': 'crotwell',
    'github_repo': 'taup'
}
