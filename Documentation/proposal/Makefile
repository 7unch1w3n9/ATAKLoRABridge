DOC := seminar-paper
LATEXMK := latexmk -pdf

.PHONY: all
all: pdf bib pdf pdf

.PHONY: pdf
pdf:
	@ echo 'Build main PDF'
	$(LATEXMK) -pdflatex="pdflatex -synctex=1 -shell-escape %O %S" $(DOC)

.PHONY: bib
bib:
	@ echo 'Build bibliography'
	bibtex $(DOC)

.PHONY: glossaries
glossaries:
	@ echo 'Build glossaries'
	makeglossaries $(DOC)

.PHONY: clean
clean:
	@ echo 'Clean'
	$(LATEXMK) -C
	@ find . \( -name '*.bbl' -or -name '*.blg' -or -name '*.log' -or -name '*.aux' -or -name '*.out' -or -name '*.synctex.gz' -or -name '*.synctex.gz(busy)' -or -name '*.auxlock' -or -name '*.fdb_latexmk' -or -name '*.fls' \) -type f -delete
	@ rm -f $(DOC).pdf

