package com.osmand.osm.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.osmand.osm.Entity;
import com.osmand.osm.Node;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.xml.internal.stream.writers.XMLStreamWriterImpl;

import static com.osmand.osm.io.OsmBaseStorage.*;

public class OSMStorageWriter {

	private final Map<Long, Entity> entities;
	private final String INDENT = "    ";
	private final String INDENT2 = INDENT + INDENT;


	public OSMStorageWriter(Map<Long, Entity> entities){
		this.entities = entities;
	}
	
	
	public void saveStorage(OutputStream output, Collection<Long> interestedObjects, boolean includeLinks) throws XMLStreamException, IOException {
		PropertyManager propertyManager = new PropertyManager(PropertyManager.CONTEXT_WRITER);
//		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        String indent = "{http://xml.apache.org/xslt}indent-amount";
//        transformer.setOutputProperty(indent, "4");
        
        
		XMLStreamWriter streamWriter = new XMLStreamWriterImpl(output, propertyManager);
		List<Node> nodes = new ArrayList<Node>();
		List<Way> ways = new ArrayList<Way>();
		List<Relation> relations = new ArrayList<Relation>();
		if(interestedObjects == null){
			interestedObjects = entities.keySet();
		}
		Stack<Long> toResolve = new Stack<Long>();
		toResolve.addAll(interestedObjects);
		while(!toResolve.isEmpty()){
			Long l = toResolve.pop();
			if(entities.get(l) instanceof Node){
				nodes.add((Node) entities.get(l));
			} else if(entities.get(l) instanceof Way){
				ways.add((Way) entities.get(l));
				if(includeLinks){
					toResolve.addAll(((Way)entities.get(l)).getNodeIds());
				}
			} else if(entities.get(l) instanceof Relation){
				relations.add((Relation) entities.get(l));
				if(includeLinks){
					toResolve.addAll(((Relation)entities.get(l)).getMemberIds());
				}
			}
		}
		
		
		streamWriter.writeStartDocument();
		
		writeStartElement(streamWriter, ELEM_OSM, "");
		streamWriter.writeAttribute(ATTR_VERSION, "0.6");
		for(Node n : nodes){
			writeStartElement(streamWriter, ELEM_NODE, INDENT);
			streamWriter.writeAttribute(ATTR_LAT, n.getLatitude()+"");
			streamWriter.writeAttribute(ATTR_LON, n.getLongitude()+"");
			streamWriter.writeAttribute(ATTR_ID, n.getId()+"");
			writeTags(streamWriter, n);
			streamWriter.writeEndElement();
		}
		
		for(Way w : ways){
			writeStartElement(streamWriter, ELEM_WAY, INDENT);
			streamWriter.writeAttribute(ATTR_ID, w.getId()+"");
			for(Long r : w.getNodeIds()){
				writeStartElement(streamWriter, ELEM_ND, INDENT2);
				streamWriter.writeAttribute(ATTR_REF, r+"");
				streamWriter.writeEndElement();
			}
			writeTags(streamWriter, w);
			streamWriter.writeEndElement();
		}
		
		for(Relation r : relations){
			writeStartElement(streamWriter, ELEM_RELATION, INDENT);
			streamWriter.writeAttribute(ATTR_ID, r.getId()+"");
			for(Entry<Long, String> e : r.getMembersMap().entrySet()){
				writeStartElement(streamWriter, ELEM_MEMBER, INDENT2);
				streamWriter.writeAttribute(ATTR_REF, e.getKey()+"");
				String s = e.getValue();
				if(s == null){
					s = ""; 
				}
				streamWriter.writeAttribute(ATTR_ROLE, s);
				streamWriter.writeAttribute(ATTR_TYPE, getEntityType(e.getKey()));
				streamWriter.writeEndElement();
			}
			writeTags(streamWriter, r);
			streamWriter.writeEndElement();
		}
		
		streamWriter.writeEndElement(); // osm
		streamWriter.writeEndDocument();
		streamWriter.flush();
	}
	
	private String getEntityType(Long id){
		Entity e = entities.get(id);
		if(e instanceof Way){
			return "way";
		} else if(e instanceof Relation){
			return "relation";
		} 
		return "node";
	}
	
	private void writeStartElement(XMLStreamWriter writer, String name, String indent) throws XMLStreamException{
		writer.writeCharacters("\n"+indent);
		writer.writeStartElement(name);
	}
	
	
	private void writeTags(XMLStreamWriter writer, Entity e) throws XMLStreamException{
		for(Entry<String, String> en : e.getTags().entrySet()){
			writeStartElement(writer, ELEM_TAG, INDENT2);
			writer.writeAttribute(ATTR_K, en.getKey());
			writer.writeAttribute(ATTR_V, en.getValue());
			writer.writeEndElement();
		}
	}
}
