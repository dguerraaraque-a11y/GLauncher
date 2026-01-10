@echo off 
start javaw -Xmx900M -Dsun.net.http.allowRestrictedHeaders=true --module-path "lib\javafx-sdk-17.0.13\lib" --add-modules javafx.controls,javafx.media,javafx.web -cp "GLauncher.jar;lib\*" glauncher.GLauncher 
