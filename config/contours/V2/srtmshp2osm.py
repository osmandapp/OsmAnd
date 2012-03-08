#!/usr/bin/python
import shapefile
import os, sys
from lxml import etree

from optparse import OptionParser
parser = OptionParser()
parser.add_option("-f", "--file", dest="inputfilename",
                  help="Shapefile filename to process", metavar="FILE")
parser.add_option("-o", "--output", dest="outputfilename",
                  help="Output filename", metavar="FILE")
parser.add_option("-p", "--pretty",
                  action="store_true", dest="pretty", default=False,
                  help="pretty-print output xml file")


(options, args) = parser.parse_args()
if not options.inputfilename:
    print "You must provide an input filename, -h or --help for help"
    exit(1)
if not options.inputfilename:
    print "You must provide an output filename, -h or --help for help"
    exit(1)

out=open(options.outputfilename,'w')
sf = shapefile.Reader(options.inputfilename)

shapeRecs = sf.shapeRecords()
print "shp loaded"

out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
out.write("<osm generator=\"srtmshp2osm\" version=\"0.6\">")

index= 3000000000
l=len(shapeRecs)
for shapeRec in shapeRecs:
    height=int(shapeRec.record[0]) # there only one field in my shapefile, 'HEIGHT' 
    points=shapeRec.shape.points # [[x1,y1],[x2,y2]...]
    
    # nodes:
    firstindex = index
    for p in points:
        node = etree.Element("node",
         id=str(index),
         lat=str(p[1]),
         lon=str(p[0]),
         version="1")
        out.write(etree.tostring(node, pretty_print=options.pretty))
        index +=1
    lastindex=index-1
    
    # way:
    way = etree.Element("way", id=str(index),version="1")
    index +=1
    for i in range(firstindex,lastindex):
        way.append(etree.Element("nd", ref=str(i)))
    
    # tags:
    way.append(etree.Element("tag",k="elevation", v=str(height)))
    way.append(etree.Element("tag",k="contour", v="elevation"))
    if height % 100 ==0:
        way.append(etree.Element("tag",k="name", v=str(height)))
    
    out.write(etree.tostring(way, pretty_print=options.pretty))
    
    #progress:
    l-=1
    sys.stdout.write("\r")
    sys.stdout.write("% 12d" % l)
    sys.stdout.flush()
    

print ""
out.write("</osm>")
out.close()
