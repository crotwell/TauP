# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

import json
with open("cmdLineHelp/VERSION.json", "r") as vf:
    verjson = json.load(vf)

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'TauP'
copyright = '2024, Philip Crotwell'
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
