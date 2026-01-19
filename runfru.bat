@echo off
chcp 1251 > nul
title FRU Report Generator

echo Starting FRU Report Engine...
java -jar fru.jar %*
pause