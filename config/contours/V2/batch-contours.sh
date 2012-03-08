for F in polys/* ; do
    nice -n 19 ./contours-extract.sh $F;
done
