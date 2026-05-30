@echo off
cd /d "d:\PROJECTS\WEB\Electronic-Clinic"
java -cp "target/classes;lib/*" com.eclinic.api.RestServer
pause
