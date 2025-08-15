DOC := proposal
LATEXMK := latexmk -pdf -interaction=nonstopmode -shell-escape

.PHONY: all
all: 
	@ echo 'Build full PDF with bibliography'
	$(LATEXMK) $(DOC)

.PHONY: clean
clean:
	@ echo 'Clean'
	$(LATEXMK) -C
