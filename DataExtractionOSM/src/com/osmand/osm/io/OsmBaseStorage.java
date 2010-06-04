package com.osmand.osm.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.osmand.IProgress;
import com.osmand.osm.Entity;
import com.osmand.osm.Node;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;

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
	
	protected boolean parseStarted;
	
	protected Map<Long, Entity> entities = new LinkedHashMap<Long, Entity>();
	
	// this is used to show feedback to user
	protected IProgress progress;
	protected InputStream inputStream;
	protected InputStream streamForProgress;
	protected List<IOsmStorageFilter> filters = new ArrayList<IOsmStorageFilter>();
	protected boolean supressWarnings = true;
	
	
	public synchronized void parseOSM(InputStream stream, IProgress progress, InputStream streamForProgress) throws IOException, SAXException {
		this.inputStream = stream;
		this.progress = progress;
		if(streamForProgress == null){
			streamForProgress = inputStream;
		}
		this.streamForProgress = streamForProgress;
		SAXParser parser = initSaxParser();
		parseStarted = false;
		entities.clear();
		if(progress != null){
			progress.startWork(streamForProgress.available());
		}
		
		parser.parse(stream, this);
		if(progress != null){
			progress.finishTask();
		}
		completeReading();
	}
	
	/**
	 * @param stream
	 * @throws IOException
	 * @throws SAXException - could be
	 */
	public synchronized void parseOSM(InputStream stream, IProgress progress) throws IOException, SAXException {
		parseOSM(stream, progress, null);
		
	}
	
	public boolean isSupressWarnings() {
		return supressWarnings;
	}
	public void setSupressWarnings(boolean supressWarnings) {
		this.supressWarnings = supressWarnings;
	}
	
	protected SAXParser saxParser;
	public SAXParser initSaxParser(){
		if(saxParser != null){
			return saxParser;
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
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
	
	protected static final Set<String> supportedVersions = new HashSet<String>();
	static {
		supportedVersions.add("0.6");
		supportedVersions.add("0.5");
	}
	
	protected void initRootElement(String uri, String localName, String name, Attributes attributes) throws OsmVersionNotSupported{
		if(!ELEM_OSM.equals(name) || !supportedVersions.contains(attributes.getValue(ATTR_VERSION))){
			throw new OsmVersionNotSupported();
		}
		parseStarted = true;	
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		name = saxParser.isNamespaceAware() ? localName : name;
		if(!parseStarted){
			initRootElement(uri, localName, name, attributes);
		}
		if (currentParsedEntity == null) {
			if(progress != null && !progress.isIndeterminate() && streamForProgress != null){
				try {
					progress.remaining(streamForProgress.available());
				} catch (IOException e) {
					progress.startWork(-1);
				}
			}
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
		name = saxParser.isNamespaceAware() ? localName : name;
		if (ELEM_NODE.equals(name) || ELEM_WAY.equals(name) || ELEM_RELATION.equals(name)) {
			if(currentParsedEntity != null){
				if(acceptEntityToLoad(currentParsedEntity)){
					Entity oldEntity = entities.put(currentParsedEntity.getId(), currentParsedEntity);
					if(!supressWarnings && oldEntity!= null){
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
	
	
	protected boolean acceptEntityToLoad(Entity entity) {
		for(IOsmStorageFilter f : filters){
			if(!f.acceptEntityToLoad(this, entity)){
				return false;
			}
		}
		return true;
	}

	public void completeReading(){
		for(Entity e : entities.values()){
			e.initializeLinks(entities);
		}
	}
	

	public Map<Long, Entity> getRegisteredEntities() {
		return entities;
	}
	
	public List<IOsmStorageFilter> getFilters() {
		return filters;
	}
}
