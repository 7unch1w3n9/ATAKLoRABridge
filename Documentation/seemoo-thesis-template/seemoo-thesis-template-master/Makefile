SRC_DIR := .
DOCUMENT := $(SRC_DIR)/Thesis
# max_repeat=6 is needed to make the reference numbering for author publication work.
# -cd will invoke build command within subdirectory SRC_DIR.
LATEXMK := latexmk -e '$$max_repeat=6' -pdf -bibtex -synctex=1 -cd

.PHONY: document sort-acronyms clean
all : document
default: document

sort-acronyms:
	sort $(SRC_DIR)/Acronyms.tex -o $(SRC_DIR)/Acronyms.tex

document:
	@echo "Build document"
	$(LATEXMK) $(DOCUMENT)

gitinfo2-hooks: gitinfo2_post-xxx.sample
	git config --local include.path ../.gitconfig

clean:
	@ echo "Clean up"
	$(LATEXMK) -C $(DOCUMENT)
	rm -f $(SRC_DIR)/pdfa.xmpi
	rm -f $(SRC_DIR)/$(DOCUMENT)-blx.bib
	rm -f $(SRC_DIR)/GraphicsCache/*
