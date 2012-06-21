#!/bin/sh
echo "Uploading latest Version"
version=$(grep '<version>' pom.xml | head -n1 | tr -d ' a-z<>/')
read a a a name a pw < ~/.netrc
echo "Current Version $version uploading using username $name"
echo "uploading jar..."
python ./googlecode_upload.py -p morphium -s "MorphiumGUI V$version" -u $name -w $pw -l "Featured" target/morphium-gui-$version.jar
echo "done... uploading uploading sources..."
python ./googlecode_upload.py -p morphium -s "MorphiumGUI V$version - Sources" -u $name -w $pw -l "Featured" target/morphium-gui-$version-sources.jar
echo "done... uploading uploading docs..."
python ./googlecode_upload.py -p morphium -s "MorphiumGUI V$version - JavaDoc" -u $name -w $pw -l "Featured" target/morphium-gui-$version-javadoc.jar
echo "All done\!"
