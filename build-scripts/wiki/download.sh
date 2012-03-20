#!/bin/bash
function download {
      echo "Start download $2";
      wget --quiet --output-document="$2"_wiki_1."$1".xml.bz2 http://dumps.wikimedia.org/"$1"wiki/latest/"$1"wiki-latest-pages-articles.xml.bz2
}
cd ~/wiki/src;
download en English;
download de German;
download nl Netherlands;
download fr French;
download ru Russian;
download es Spanish;
download pl Polish;
download it Italian;
download ca Catalonian;
download pt Portuguese;
download uk Ukranian;
download ja Japanese;
download vo Volapuk;
download vi Vietnamese;
download eu Basque;
download no Norwegian;
download da Danish;
download sv Swedish;
download sr Serbian;
download eo Esperanto;
download ro Romanian;
download lt Lithuanian;
download fa Farsi;
download cs Czech;
download ms Malay;
download zh Chinese;
download id Indonesian;
download fi Finnish;
download bg Bulgarian;
download et Estonian;
download hr Croatian;
download sk Slovak;
download nn NorwegianNynorsk;
download ko Korean;
download sl Slovene;
download el Greek;
download he Hebrew;
download ar Arabic ;
download tr Turkish;
download th Thai;
download be Belarusian;
download ka Georgian;
download mk Macedonian;
download lv Latvian;
download lb Luxembourgish;
download os Ossetian;
download gl Galician ;

download fy Frysk;
download af Africaans;
download hy Armenian;
download ml Malayalam;
download als Alsatian;
download sw Swahili;
download ta Tamil;
download nds LowSaxon;
download ku Kurdish;
download la Latin;
download ga Irish;
download nv Navajo;
download hi Hindi;
download hu Hungarian;
download te Telugu;
download ht Haitian;


