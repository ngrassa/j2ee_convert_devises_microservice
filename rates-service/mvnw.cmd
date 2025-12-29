@ECHO OFF
SETLOCAL
WHERE mvn >NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  ECHO Apache Maven is required but not found in PATH.
  EXIT /B 1
)
mvn %*
