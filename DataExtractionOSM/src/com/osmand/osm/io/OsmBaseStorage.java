package com.osmand.osm.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.osmand.osm.Entity;
import com.osmand.osm.Node;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.xml.internal.stream.writers.XMLStreamWriterImpl;

public class OsmBaseStorage extends DefaultHandler {

	protected static final String ELEM_OSM = "osm";
	protected static final String ELEM_NODE = "node";
	protected static final String ELEM_TAG = "tag";
	protected static final String ELEM_WAY = "way";
	protected static final String ELEM_ND = "nd";
	protected static final String ELEM_RELATION = "relation";
	protected static final String ELEM_MEMBER = "member";
	
	
	protected static final String ATTR_VERSION = "version";
	protected static final String ATTR_ID = "id";
	protected static final String ATTR_LAT = "lat";
	protected static final String ATTR_LON = "lon";
	protected static final String ATTR_K = "k";
	protected static final String ATTR_V = "v";
	
	protected static final String ATTR_TYPE = "type";
	protected static final String ATTR_REF = "ref";
	protected static final String ATTR_ROLE = "role";
	
	protected Entity currentParsedEntity = null;
	
	private boolean parseStarted;
	
	protected Map<Long, Entity> entities = new LinkedHashMap<Long, Entity>();
	
	
	
	/**
	 * @param stream
	 * @throws IOException
	 * @throws SAXException - could be
	 */
	public synchronized void parseOSM(InputStream stream) throws IOException, SAXException {
		SAXParser parser = initSaxParser();
		parseStarted = false;
		entities.clear();
		parser.parse(stream, this);
		completeReading();
	}
	
	private SAXParser saxParser;
	public SAXParser initSaxParser(){
		if(saxParser != null){
			return saxParser;
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			return saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}	
	}
	
	protected Long parseId(Attributes a, String name, long defId){
		long id = defId; 
		String value = a.getValue(name);
		try {
			id = Long.parseLong(value);
		} catch (NumberFormatException e) {
		}
		return id;
	}
	
	protected double parseDouble(Attributes a, String name, double defVal){
		double ret = defVal; 
		String value = a.getValue(name);
		try {
			ret = Double.parseDouble(value);
		} catch (NumberFormatException e) {
		}
		return ret;
	}
	
	private static final Set<String> supportedVersions = new HashSet<String>();
	static {
		supportedVersions.add("0.6");
		supportedVersions.add("0.5");
	}
	
	private final String INDENT = "    ";
	private final String INDENT2 = INDENT + INDENT;
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if(!parseStarted){
			if(!ELEM_OSM.equals(name) || !supportedVersions.contains(attributes.getValue(ATTR_VERSION))){
				throw new OsmVersionNotSupported();
			}
			parseStarted = true;
		}
		if (currentParsedEntity == null) {
			if (ELEM_NODE.equals(name)) {
				currentParsedEntity = new Node(parseDouble(attributes, ATTR_LAT, 0), parseDouble(attributes, ATTR_LON, 0),
						parseId(attributes, ATTR_ID, -1));
			} else if (ELEM_WAY.equals(name)) {
				currentParsedEntity = new Way(parseId(attributes, ATTR_ID, -1));
			} else if (ELEM_RELATION.equals(name)) {
				currentParsedEntity = new Relation(parseId(attributes, ATTR_ID, -1));
			} else {
				// this situation could be logged as unhandled
			}
		} else {
			if (ELEM_TAG.equals(name)) {
				String key = attributes.getValue(ATTR_K);
				if(key != null){
					currentParsedEntity.putTag(key, attributes.getValue(ATTR_V));
				}
			} else if (ELEM_ND.equals(name)) {
				Long id = parseId(attributes, ATTR_REF, -1);
				if(id != -1 && currentParsedEntity instanceof Way){
					((Way)currentParsedEntity).addNode(id);
				}
			} else if (ELEM_MEMBER.equals(name)) {
				Long id = parseId(attributes, ATTR_REF, -1);
				if(id != -1 && currentParsedEntity instanceof Relation){
					((Relation)currentParsedEntity).addMember(id, attributes.getValue(ATTR_ROLE));
				}

			}  else {
				// this situation could be logged as unhandled
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if (ELEM_NODE.equals(name) || ELEM_WAY.equals(name) || ELEM_RELATION.equals(name)) {
			if(currentParsedEntity != null){
				if(acceptEntityToLoad(currentParsedEntity)){
					Entity oldEntity = entities.put(currentParsedEntity.getId(), currentParsedEntity);
					if(oldEntity!= null){
						throw new UnsupportedOperationException("Entity with id=" + oldEntity.getId() +" is duplicated in osm map");
					}
				} else {
//					System.gc();
				}
				currentParsedEntity = null;
			}
		}
		super.endElement(uri, localName, name);
	}
	
	
	public void completeReading(){
		for(Entity e : entities.values()){
			e.initializeLinks(entities);
		}
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
				toResolve.addAll(((Way)entities.get(l)).getNodeIds());
			} else if(entities.get(l) instanceof Relation){
				relations.add((Relation) entities.get(l));
				toResolve.addAll(((Relation)entities.get(l)).getMemberIds());
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
	
	
	
	public boolean acceptEntityToLoad(Entity e){
		if(e instanceof Way){
			return acceptWayToLoad((Way) e);
		} else if(e instanceof Relation){
			return acceptRelationToLoad((Relation) e);
		} else if(e instanceof Node){
			return acceptNodeToLoad((Node) e);
		}
		return false;
	}
		
	public boolean acceptWayToLoad(Way w){
		return true;
	}
	
	public boolean acceptRelationToLoad(Relation w){
		return true;
	}
	
	public boolean acceptNodeToLoad(Node n){
		return true;
	}
	

}
