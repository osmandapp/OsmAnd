#where this script stands
WORKDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${WORKDIR}

TMPDIR=${WORKDIR}/tmp/
OSMDIR=${WORKDIR}/osm/
INDEXDIR=${WORKDIR}/index/
OUTDIR=${WORKDIR}/output/
POLYDIR=${WORKDIR}/polygons/

for X in ${POLYDIR}*.poly; do
    echo $X
    base=$(basename $X) # get the basename without the path
    extractname=`echo "${base%.*}"` #remove the extension
    # Capitalize:
    for i in $extractname; do B=`echo -n "${i:0:1}" | tr "[a-z]" "[A-Z]"`; cap=`echo -n "${B}${i:1}" `; done
    
    if [ -f ${OUTDIR}${cap}_SRTM_contours.obf ] # Check for already processed files
        then
            echo "Nothing to do for "${cap}
        else
            echo ${X}"---"${cap}
            ${WORK_DIR}./extract.py ${X}
            ${WORK_DIR}./osmconvert32 ${TMPDIR}out.osm --drop-brokenrefs -B=$X > ${OSMDIR}out.osm
            ${WORK_DIR}./batch_indexing.sh
            mv ${INDEXDIR}Out_1.obf ${OUTDIR}${cap}_SRTM_contours.obf
            rm ${INDEXDIR}*
            rm ${TMPDIR}*
            rm ${OSMDIR}*
        fi
done
