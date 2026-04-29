@echo off
if exist ".mvn\wrapper\maven-wrapper.jar" (
    java -jar ".mvn\wrapper\maven-wrapper.jar" %*
) else (
    mvn %*
)
