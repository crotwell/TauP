

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

.SUFFIXES : .pdf .ps .dvi .bbl .bib .tex

.tex.dvi	:
	latex $*
	bibtex $*
	latex $*
	latex $*

.dvi.ps	:
	dvips -o $*.ps $*

.tex.ps	:
	latex $*
	bibtex $*
	latex $*
	latex $*
	dvips -o $*.ps $*

.tex.pdf	: touch
	pdflatex $*
	bibtex $*
	pdflatex $*
	pdflatex $*
	latex $*
	dvips -o $*.ps $*

.bib.bbl	:
	bibtex $*

doc:	touch taup.pdf
	/bin/rm -f ../doc/taup.ps ../doc/taup.pdf
	mv taup.ps taup.pdf ../doc/.

touch:
	touch taup.tex

clean:
	/bin/rm -f *.bak *.dvi *.pdf *.ps *.aux *.bbl *.blg *.log *.toc
