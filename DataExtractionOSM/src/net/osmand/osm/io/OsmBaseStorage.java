package net.osmand.osm.io;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.IProgress;
import net.osmand.osm.Entity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.Osmformat.HeaderBlock;
import crosby.binary.Osmformat.Info;
import crosby.binary.Osmformat.Relation.MemberType;
import crosby.binary.file.BlockInputStream;


public class OsmBaseStorage extends DefaultHandler {

	protected static final String ELEM_OSM = "osm"; //$NON-NLS-1$
	protected static final String ELEM_NODE = "node"; //$NON-NLS-1$
	protected static final String ELEM_TAG = "tag"; //$NON-NLS-1$
	protected static final String ELEM_WAY = "way"; //$NON-NLS-1$
	protected static final String ELEM_ND = "nd"; //$NON-NLS-1$
	protected static final String ELEM_RELATION = "relation"; //$NON-NLS-1$
	protected static final String ELEM_MEMBER = "member"; //$NON-NLS-1$
	
	
	protected static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	protected static final String ATTR_ID = "id"; //$NON-NLS-1$
	protected static final String ATTR_LAT = "lat"; //$NON-NLS-1$
	protected static final String ATTR_LON = "lon"; //$NON-NLS-1$
	protected static final String ATTR_TIMESTAMP = "timestamp"; //$NON-NLS-1$
	protected static final String ATTR_UID = "uid"; //$NON-NLS-1$
	protected static final String ATTR_USER = "user"; //$NON-NLS-1$
	protected static final String ATTR_VISIBLE = "visible"; //$NON-NLS-1$
	protected static final String ATTR_CHANGESET = "changeset"; //$NON-NLS-1$
	protected static final String ATTR_K = "k"; //$NON-NLS-1$
	protected static final String ATTR_V = "v"; //$NON-NLS-1$
	
	protected static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	protected static final String ATTR_REF = "ref"; //$NON-NLS-1$
	protected static final String ATTR_ROLE = "role"; //$NON-NLS-1$
	
	protected Entity currentParsedEntity = null;
	protected EntityInfo currentParsedEntityInfo = null;
	
	protected boolean parseStarted;
	
	protected Map<EntityId, Entity> entities = new LinkedHashMap<EntityId, Entity>();
	protected Map<EntityId, EntityInfo> entityInfo = new LinkedHashMap<EntityId, EntityInfo>();
	
	// this is used to show feedback to user
	protected int progressEntity = 0;
	protected IProgress progress;
	protected InputStream inputStream;
	protected InputStream streamForProgress;
	protected List<IOsmStorageFilter> filters = new ArrayList<IOsmStorageFilter>();
	protected boolean supressWarnings = true;
	protected boolean parseEntityInfo;
	
	
	
	public synchronized void parseOSM(InputStream stream, IProgress progress, InputStream streamForProgress, 
			boolean entityInfo) throws IOException, SAXException {
		this.inputStream = stream;
		this.progress = progress;
		parseEntityInfo = entityInfo;
		if(streamForProgress == null){
			streamForProgress = inputStream;
		}
		this.streamForProgress = streamForProgress;
		SAXParser parser = initSaxParser();
		parseStarted = false;
		entities.clear();
		this.entityInfo.clear();
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
		parseOSM(stream, progress, null, true);
		
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
			factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false); //$NON-NLS-1$
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
		supportedVersions.add("0.6"); //$NON-NLS-1$
		supportedVersions.add("0.5"); //$NON-NLS-1$
	}
	
	protected void initRootElement(String uri, String localName, String name, Attributes attributes) throws OsmVersionNotSupported{
		if(!ELEM_OSM.equals(name) || !supportedVersions.contains(attributes.getValue(ATTR_VERSION))){
			throw new OsmVersionNotSupported();
		}
		parseStarted = true;	
	}
	
	private static final int moduleProgress = 1 << 10;
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		name = saxParser.isNamespaceAware() ? localName : name;
		if(!parseStarted){
			initRootElement(uri, localName, name, attributes);
		}
		if (currentParsedEntity == null) {
			progressEntity ++;
			if(progress != null && ((progressEntity % moduleProgress) == 0) && 
					!progress.isIndeterminate() && streamForProgress != null){
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
			if(parseEntityInfo && currentParsedEntity != null){
				currentParsedEntityInfo = new EntityInfo();
				currentParsedEntityInfo.setChangeset(attributes.getValue(ATTR_CHANGESET));
				currentParsedEntityInfo.setTimestamp(attributes.getValue(ATTR_TIMESTAMP));
				currentParsedEntityInfo.setUser(attributes.getValue(ATTR_USER));
				currentParsedEntityInfo.setVersion(attributes.getValue(ATTR_VERSION));
				currentParsedEntityInfo.setVisible(attributes.getValue(ATTR_VISIBLE));
				currentParsedEntityInfo.setUid(attributes.getValue(ATTR_UID));
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
					EntityType type = EntityType.valueOf(attributes.getValue(ATTR_TYPE).toUpperCase());
					((Relation)currentParsedEntity).addMember(id, type, attributes.getValue(ATTR_ROLE));
				}

			}  else {
				// this situation could be logged as unhandled
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		name = saxParser.isNamespaceAware() ? localName : name;
		EntityType type = null;
		if (ELEM_NODE.equals(name)){
			type = EntityType.NODE; 
		} else if (ELEM_WAY.equals(name)){
			type = EntityType.WAY;
		} else if (ELEM_RELATION.equals(name)){
			type = EntityType.RELATION;
		}
		if (type != null) {
			if(currentParsedEntity != null){
				EntityId entityId = new EntityId(type, currentParsedEntity.getId());
				if(acceptEntityToLoad(entityId, currentParsedEntity)){
					Entity oldEntity = entities.put(entityId, currentParsedEntity);
					if(parseEntityInfo && currentParsedEntityInfo != null){
						entityInfo.put(entityId, currentParsedEntityInfo);
					}
					if(!supressWarnings && oldEntity!= null){
						throw new UnsupportedOperationException("Entity with id=" + oldEntity.getId() +" is duplicated in osm map"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} else {
//					System.gc();
				}
				currentParsedEntity = null;
			}
		}
		super.endElement(uri, localName, name);
	}
	
	
	protected boolean acceptEntityToLoad(EntityId entityId, Entity entity) {
		for(IOsmStorageFilter f : filters){
			if(!f.acceptEntityToLoad(this, entityId, entity)){
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
	
	public Map<EntityId, EntityInfo> getRegisteredEntityInfo() {
		return entityInfo;
	} 

	public Map<EntityId, Entity> getRegisteredEntities() {
		return entities;
	}
	
	public List<IOsmStorageFilter> getFilters() {
		return filters;
	}
	
	public synchronized void parseOSMPbf(final InputStream stream, final IProgress progress, final boolean entityInfo) throws IOException {
		BinaryParser parser = new BinaryParser(){
			public void updateProgress(int count){
				progressEntity += count;
				if(progress != null && progressEntity > moduleProgress &&	!progress.isIndeterminate()){
					try {
						progressEntity = 0;
						progress.remaining(stream.available());
					} catch (IOException e) {
						progress.startWork(-1);
					}
				}
			}
			
			public void registerEntity(EntityType type, Entity e, EntityInfo info) {
				EntityId entityId = new EntityId(type, e.getId());
				if (acceptEntityToLoad(entityId, e)) {
					Entity oldEntity = entities.put(entityId, e);
					if (info != null) {
						OsmBaseStorage.this.entityInfo.put(entityId, info);
					}
					if (!supressWarnings && oldEntity != null) {
						throw new UnsupportedOperationException("Entity with id=" + oldEntity.getId() + " is duplicated in osm map"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			
			@Override
			protected void parse(HeaderBlock header) {
			}
			
			private DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$

			@Override
			protected void parseDense(DenseNodes n) {
				EntityInfo info = null;
				long changeset = 0;
				long timestamp = 0;
				int uid = 0;
				int user = 0;
				long id = 0;
				long lat = 0;
				long lon = 0;
				int keyInd = 0;
				boolean tagsEmpty = n.getKeysValsCount() == 0;
				for(int i=0; i<n.getIdCount(); i++){
					id += n.getId(i);
					lat += n.getLat(i);
					lon += n.getLon(i);
					Node node = new Node(parseLat(lat), parseLon(lon), id);
					
					if (entityInfo && n.getDenseinfo() != null) {
						info = new EntityInfo();
						changeset += n.getDenseinfo().getChangeset(i);
						timestamp += n.getDenseinfo().getTimestamp(i);
						uid += n.getDenseinfo().getUid(i);
						user += n.getDenseinfo().getUserSid(i);
						info.setChangeset((changeset) + ""); //$NON-NLS-1$
						info.setTimestamp(format.format(new Date(date_granularity * (timestamp))));
						info.setUser(getStringById(user));
						info.setUid(uid + ""); //$NON-NLS-1$
						info.setVersion(n.getDenseinfo().getVersion(i) + ""); //$NON-NLS-1$
						info.setVisible("true"); //$NON-NLS-1$
					}
					if (!tagsEmpty) {
						while (n.getKeysVals(keyInd) != 0) {
							String key = getStringById(n.getKeysVals(keyInd));
							String val = getStringById(n.getKeysVals(keyInd + 1));
							node.putTag(key, val);
							keyInd += 2;
						}
						keyInd++;
					}
					registerEntity(EntityType.NODE, node, info);
				}
				updateProgress(n.getIdCount());
			}
			
			protected EntityInfo parseEntityInfo(Info i){
				EntityInfo info = new EntityInfo();
				info.setChangeset(i.getChangeset()+""); //$NON-NLS-1$
				info.setTimestamp(format.format(getDate(i)));
				info.setUser(getStringById(i.getUserSid()));
				info.setUid(i.getUid()+""); //$NON-NLS-1$
				info.setVersion(i.getVersion()+""); //$NON-NLS-1$
				info.setVisible("true"); //$NON-NLS-1$
				return info;
			}

			@Override
			protected void parseNodes(List<crosby.binary.Osmformat.Node> n) {
				EntityInfo info = null;
				int nsize = n.size();
				for(int i=0; i<nsize; i++){
					crosby.binary.Osmformat.Node nod = n.get(i);
					Node e = new Node(parseLat(nod.getLat()), parseLon(nod.getLon()), nod.getId());
					for(int j=0; j<nod.getKeysCount(); j++){
						String key = getStringById(nod.getKeys(j));
						String val = getStringById(nod.getVals(j));
						e.putTag(key, val);
					}
					if(entityInfo){
						info = parseEntityInfo(nod.getInfo());
					}
					registerEntity(EntityType.NODE, e, info);
				}
				updateProgress(nsize);
			}

			@Override
			protected void parseRelations(List<crosby.binary.Osmformat.Relation> r) {
				EntityInfo info = null;
				int rsize = r.size();
				for(int i=0; i<rsize; i++){
					crosby.binary.Osmformat.Relation rel = r.get(i);
					Relation e = new Relation(rel.getId());
					long id = 0;
					for(int j=0; j<rel.getMemidsCount(); j++){
						id += rel.getMemids(j);
						String role = getStringById(rel.getRolesSid(j));
						MemberType t = rel.getTypes(j);
						EntityType ts = EntityType.NODE;
						switch(t){
						case NODE : ts = EntityType.NODE; break;
						case WAY : ts = EntityType.WAY; break;
						case RELATION : ts = EntityType.RELATION; break;
						}
						e.addMember(id, ts, role);
					}
					for(int j=0; j<rel.getKeysCount(); j++){
						String key = getStringById(rel.getKeys(j));
						String val = getStringById(rel.getVals(j));
						e.putTag(key, val);
					}
					if(entityInfo){
						info = parseEntityInfo(rel.getInfo());
					}
					registerEntity(EntityType.RELATION, e, info);
				}
				updateProgress(rsize);
			}

			@Override
			protected void parseWays(List<crosby.binary.Osmformat.Way> w) {
				EntityInfo info = null;
				int wsize = w.size();
				for(int i=0; i<wsize; i++){
					crosby.binary.Osmformat.Way way = w.get(i);
					Way e = new Way(way.getId());
					long id = 0;
					for(int j=0; j<way.getRefsCount(); j++){
						id += way.getRefs(j);
						e.addNode(id);
					}
					for(int j=0; j<way.getKeysCount(); j++){
						String key = getStringById(way.getKeys(j));
						String val = getStringById(way.getVals(j));
						e.putTag(key, val);
					}
					if(entityInfo){
						info = parseEntityInfo(way.getInfo());
					}
					registerEntity(EntityType.WAY, e, info);
				}
				updateProgress(wsize);
			}

			@Override
			public void complete() {
			}
			
		};
		
		this.progressEntity = 0;
		this.entities.clear();
		this.entityInfo.clear();
		if(progress != null){
			progress.startWork(stream.available());
		}
		
		BlockInputStream bis = new BlockInputStream(stream, parser);
		bis.process();
		
		if(progress != null){
			progress.finishTask();
		}
		completeReading();
	}
}
