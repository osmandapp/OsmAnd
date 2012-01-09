#!/bin/bash
function download_coord {

  echo "Start download $1"
  wget --quiet --output-document="$1"wiki.sql.gz http://toolserver.org/~dispenser/dumps/coord_"$1"wiki.sql.gz
  echo "Start import $1";
  gunzip -c "$1"wiki.sql.gz | mysql wiki;
}

cd ~/wiki/src_sql;
download_coord en;
download_coord de;
download_coord nl;
download_coord fr;
download_coord ru;
download_coord es;
download_coord pl;
download_coord it;
download_coord ca;
download_coord pt;
download_coord uk;
download_coord ja;
download_coord vo;
download_coord vi;
download_coord eu;
download_coord no;
download_coord da;
download_coord sv;
download_coord sr;
download_coord eo;
download_coord ro;
download_coord lt;
download_coord fa;
download_coord cs;
download_coord ms;
download_coord zh;
download_coord id;
download_coord fi;
download_coord bg;
download_coord et;
download_coord hr;
download_coord sk;
download_coord nn;
download_coord ko;
download_coord sl;
download_coord el;
download_coord he;
download_coord ar;
download_coord tr;
download_coord th;
download_coord be;
download_coord ka;
download_coord mk;
download_coord lv;
download_coord lb;
download_coord os;
download_coord gl;

