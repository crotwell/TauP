# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'TauP'
copyright = '2023, Philip Crotwell'
author = 'Philip Crotwell'
release = '3.0'

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
    'description': 'Flexible Seismic Travel-Time and Raypath Utilities. Version 3.0 Beta',
    'fixed_sidebar': "true",
    'github_user': 'crotwell',
    'github_repo': 'taup'
}
