.PHONY : main

MAIN=submission
LATEX=pdflatex
BIBTEX=bibtex

all: paper tidy

paper: $(MAIN).tex
	$(LATEX) $(MAIN)
	$(BIBTEX) $(MAIN)
	$(LATEX) $(MAIN)
	$(LATEX) $(MAIN)

tidy:
	rm -f *.dvi *.aux *.log *.nav *.snm *.toc *.out *.vrb *.bbl *.blg

clean:
	rm -f *.dvi *.aux *.log *.nav *.snm *.toc *.out *.vrb *.bbl *.blg $(MAIN).ps $(MAIN).pdf
