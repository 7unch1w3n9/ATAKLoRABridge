@ECHO OFF
SETLOCAL
SET DOCUMENT=Thesis
SET LATEXMK=latexmk -e '$$max_repeat=6' -pdf -bibtex -synctex=1 -cd

IF [%1] == [] GOTO default
IF [%1] == [document] GOTO document
IF [%1] == [clean] GOTO clean
GOTO :EOF

:default
:document
ECHO build document
%LATEXMK% %DOCUMENT%
GOTO :EOF

:clean
ECHO clean up
%LATEXMK% -C %DOCUMENT%
DEL /F pdfa.xmpi 2>nul
DEL /F %DOCUMENT%-blx.bib 2>nul
GOTO :EOF
