#!/bin/bash

# the CC field
echo > mail.txt
echo "Index generating finished or crashed." >> mail.txt
echo "List is sorted by time, first elements are the last created" >> mail.txt
echo >> mail.txt
ls -alrth indexes >> mail.txt

mail -s "Generating indexes finsihed or crashed" pavol.zibrita+index@gmail.com < mail.txt
mail -s "Generating indexes finsihed or crashed" victor.shcherb+index@gmail.com < mail.txt
