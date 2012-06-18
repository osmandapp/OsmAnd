rm -rf ./zlib_library
wget -qO - http://zlib.net/zlib-1.2.7.tar.gz | tar xzvf -
mkdir -p ./zlib_library
mv -f ./zlib-1.2.7/* ./zlib_library
rm -r ./zlib-1.2.7
