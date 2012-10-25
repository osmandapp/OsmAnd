#!/usr/bin/python
import shapefile
import os, sys
from lxml import etree
from optparse import OptionParser

def listsplit(tosplit, n):
	splited=[]
	i=0
	while i < (len(tosplit)-n):
		splited.append(tosplit[i:i+n])
		i+=n
	last=tosplit[i:]
	last.append(tosplit[0])# first == last to close
	splited.append(last)
	return splited


parser = OptionParser()
parser.add_option("-f", "--file", dest="inputfilename",
                  help="Shapefile filename to process", metavar="FILE")
parser.add_option("-o", "--output", dest="outputfilename",
                  help="Output filename", metavar="FILE")
parser.add_option("-p", "--pretty",
                  action="store_true", dest="pretty", default=False,
                  help="pretty-print output xml file")
parser.add_option("-q", "--quiet",
                  action="store_true", dest="quiet", default=False,
                  help="suppress progress, usefull when logging")

(options, args) = parser.parse_args()
if not options.inputfilename:
    print "You must provide an input filename, -h or --help for help"
    exit(1)
if not options.inputfilename:
    print "You must provide an output filename, -h or --help for help"
    exit(1)

out=open(options.outputfilename,'w')
out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
out.write("<osm generator=\"srtmshp2osm\" version=\"0.6\">")

sf = shapefile.Reader(options.inputfilename)

if len(sf.shapes()) != 0:
	# read shapeRecords only if non-empty
	shapeRecs = sf.shapeRecords()
	print "shp loaded"
	
	index= 3000000000
	l=len(shapeRecs)
	for shapeRec in shapeRecs:
	    height=int(shapeRec.record[1]) # there only one field in my shapefile, 'HEIGHT' 
	    pointList=shapeRec.shape.points # [[x1,y1],[x2,y2]...]
	    splitPointList=listsplit(pointList,100)
	    
	    for points in splitPointList:
			
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
		        
		    #way.append(etree.Element("nd", ref=str(firstindex)))
		    
		    # tags:
		    way.append(etree.Element("tag",k="elevation", v=str(height)))
		    way.append(etree.Element("tag",k="contour", v="elevation"))
		    if height % 100 ==0:
		        way.append(etree.Element("tag",k="contourtype", v="100m"))
		        way.append(etree.Element("tag",k="name", v=str(height)))
		    elif height % 50 ==0:
		        way.append(etree.Element("tag",k="contourtype", v="50m"))
		    elif height % 20 ==0:
		        way.append(etree.Element("tag",k="contourtype", v="20m"))
		    elif height % 10 ==0:
		        way.append(etree.Element("tag",k="contourtype", v="10m"))
		    
		    out.write(etree.tostring(way, pretty_print=options.pretty))
		    
	    #progress:
	    l-=1
	    if not options.quiet:
			sys.stdout.write("\r")
			sys.stdout.write("% 12d" % l)
			sys.stdout.flush()
else:
	print "Shapefile was empty"

print ""
out.write("</osm>")
out.close()
