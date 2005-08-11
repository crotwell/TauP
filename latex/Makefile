

OBJS = distribution.tex \
	examples.tex \
	install.tex \
	models.tex \
	overview.tex \
	phaseNaming.tex \
	programming.tex \
	taup.tex \
	titlepage.tex \
	tools.tex

.SUFFIXES : .pdf .dvi .bbl .bib .tex

.tex.pdf	: touch
	pdflatex $*
	bibtex $*
	pdflatex $*
	pdflatex $*

.bib.bbl	:
	bibtex $*

doc:	touch taup.pdf
	/bin/rm -f ../doc/taup.pdf
	mv taup.pdf ../doc/.

touch:
	touch taup.tex

clean:
	/bin/rm -f *.bak *.dvi *.pdf *.aux *.bbl *.blg *.log *.toc
