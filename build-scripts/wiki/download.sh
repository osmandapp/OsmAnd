#!/bin/sh
function download {
      wget -o download.log http://dumps.wikimedia.org/"$1"wiki/latest/"$1"wiki-latest-pages-articles.xml.bz2
}
# Arabic
download ar
# English
download en
# Spanish
download es
# Portuguese
download pt
# French
download fr
# German
download de
# Russian
download ru


