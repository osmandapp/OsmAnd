#!/bin/bash

# the CC field
echo > mail.txt
echo "Uploading finished or crashed." >> mail.txt
echo "Files in upload dir:" >> mail.txt
echo >> mail.txt
ls -alt indexes/uploaded >> mail.txt
echo >> mail.txt
echo "Files in indexes dir:" >> mail.txt
ls -alt indexes >> mail.txt

mail -s "Uploading indexes finsihed or chrased" pavol.zibrita+index@gmail.com < mail.txt
mail -s "Uploading indexes finsihed or chrased" victor.shcherb+index@gmail.com < mail.txt
