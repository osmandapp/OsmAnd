#!/usr/bin/env python
# Source is GPL

import sys, re
import pdb
import os
from lxml import etree

if (len(sys.argv) > 1):
    filename= sys.argv[1]
# Extract the bounding box around the .poly:
f=open(filename,'r')
maxx = -360;
maxy = -360;
minx = 360;
miny = 360;
for l in f.readlines():
    coord=re.findall('[0-9.E+-]+',l)
    # At least numbers, at least two
    if re.findall('[0-9.]+',l):
        if len(coord)==2:
            if float(coord[0]) > maxx: maxx= float(coord[0])
            if float(coord[1]) > maxy: maxy= float(coord[1])
            if float(coord[0]) < minx: minx= float(coord[0])
            if float(coord[1]) < miny: miny= float(coord[1])
#print minx, miny, maxx, maxy

# It is better to split the area on 0.25x0.25 degrees tiles to allow low
# memory usage of lxml and small lines
box=0.25

Xs=[]
x=int(minx)
while x < maxx+box:
    Xs.append(x)
    x += box
Ys=[]
y=int(miny)
while y < maxy+box:
    Ys.append(y)
    y += box

for x in Xs:
    for y in Ys:
        #print y, x, y+1, x+1
        os.system('Srtm2Osm/./Srtm2Osm.exe -step 20 -bounds1 '\
        + str(y) +' '+ str(x)+' '+ str(y+box)+' '+ str(x+box)+' -o tmp/'\
        + str(y) +'-'+ str(x)+'.osm')
        
wayid=1000000001
nodeid=2000000001

# re-open the files to ensure uniques id before merging
for x in Xs:
    for y in Ys:
        file='tmp/'+str(y) +'-'+ str(x)+'.osm'
        osmDoc=etree.parse(file)
        
        outFile=open('tmp/out-'+str(y) +'-'+ str(x)+'.osm','w')
        inRoot= osmDoc.getroot()
        ways = osmDoc.findall('way')
        nodes = osmDoc.findall('node')
        #print "nodes:", len (nodes)
        #print "ways:", len(ways)
        wayList={}
        nodeList={}
        # create a dict of nodes by id for faster processing
        for node in nodes:
            nodeList[node.get('id')]=node
        # create a dict of ways by id for faster processing
        for way in ways:
            wayList[way.get('id')]=way
        
        outRoot = etree.Element("osm",version="0.5", generator="Srtm2Osm + post-process by Yvecai")
        for way in ways:
            nds=way.findall('nd')
            newWay = etree.Element("way",id=str(wayid))
            # create nodes in the output tree
            for nd in nds:
                ref=nd.get('ref')
                try: 
                    newNode = etree.Element("node",id=str(nodeid), lon=nodeList[ref].get('lon'), \
                    lat=nodeList[ref].get('lat'))
                except:
                    newNode = etree.Element("node",id=str(nodeid), lon="0",\
                    lat=nodeList[ref].get('lat')) # Srtm2Osm bug on lon="0"
                newNd= etree.Element("nd",ref=str(nodeid))
                nodeid+=1
                outRoot.append(newNode)
                newWay.append(newNd)
                
            newTag= etree.Element("tag",k="contour", v="elevation")
            newWay.append(newTag)
            tags=way.findall('tag')
            for tag in tags:
                if tag.get('k')=='ele':
                    ele=tag.get('v')
            #pdb.set_trace()
            newTag= etree.Element("tag",k="ele", v=ele)
            newWay.append(newTag)
            #pdb.set_trace()
            # Add a 'name' tag to render elevation on 100m lines
            if int(ele) % 100 == 0:
                newTag= etree.Element("tag",k="name", v=ele)
                newWay.append(newTag)
            outRoot.append(newWay)
            wayid+=1
        
        outFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        outFile.write(etree.tostring(outRoot, pretty_print=True))
        outFile.close()
#print wayid, nodeid


outFile=open('tmp/out.osm','w')
outFile.write('<?xml version="1.0" encoding="utf-8"?>')
outFile.write('<osm version="0.5" generator="Srtm2Osm + post-process by Yvecai">')
for x in Xs:
    for y in Ys:
        f=open('tmp/out-'+str(y) +'-'+ str(x)+'.osm','r')
        lines=f.readlines()
        outFile.writelines(lines[2:-1])
        f.close()
outFile.write('</osm>')

outFile.close()


        
