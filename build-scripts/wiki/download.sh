#!/bin/bash
function download {
      echo "Start download $2";
      wget --quiet --output-document="$2"_wiki_1."$1".xml.bz2 http://dumps.wikimedia.org/"$1"wiki/latest/"$1"wiki-latest-pages-articles.xml.bz2
}
cd src;
download en English;
download de Deutch;
download nl Netherlands;
download fr French;
download ru Russian;
download es Spanish;
download it Italian;
download pt Portuguese;
download ja Japanese;
