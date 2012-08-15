#! /bin/bash

pages=('AF'  'AR' 'BR' 'CA' 'CS' 'DE' 'EN' 'ES' 'ET' 'EU' 'FA' 'FI' 'FR' 'GL' 'HR' 'HU' 'IA' 'IS' 'IT' 'JA' 'MK' 'NL' 'NO' 'PL' 'PS' 'PT' 'RU' 'SK' 'SV' 'UK' 'VI')

# declare -a pages=('AF' 'AR' 'BR' 'CA' 'CS' 'DE' 'EN' 'ES' 'ET' 'EU' 'FA' 'FI' 'FR' 'GL' 'HR' 'HU' 'IA' 'IS' 'IT' 'JA' 'MK' 'NL' 'NO' 'PL' 'PS' 'PT' 'RU' 'SK' 'SV' 'UK' 'VI')



for lang in ${pages[@]} 
do
    wget http://wiki.openstreetmap.org/wiki/Special:Export/Nominatim/Special_Phrases/${lang} -O /tmp/automatedJavaGenarationFile.txt
    
    cat /tmp/automatedJavaGenarationFile.txt | grep " - " | grep  " N" > /tmp/automatedJavaGenarationFile2.txt

    sed -e 's/ *|/|/g' -e 's/| */|/g' </tmp/automatedJavaGenarationFile2.txt > /tmp/automatedJavaGenarationFile.txt

    file="../assets/specialphrases/specialphrases_${lang,,}.txt"

    echo ""  > $file
    
    while read line; do 
    
        IFS="||"
        arr=( $line )
        
        
        
        echo ${arr[5]}','${arr[1]} >> $file

    done < /tmp/automatedJavaGenarationFile.txt

done


rm /tmp/automatedJavaGenarationFile.txt
rm /tmp/automatedJavaGenarationFile2.txt

