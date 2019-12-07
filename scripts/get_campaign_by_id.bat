@echo off
IF [%1] == [] (
	echo Please provide the ID of the campaign to return.
	exit /b
)
@echo on
curl --include http://localhost:8080/campaigns/%1