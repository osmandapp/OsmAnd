package net.osmand.osm.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityId;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Relation;
import net.osmand.osm.edit.Way;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class OsmBaseStorage {

	protected static final String ELEM_OSM = "osm"; //$NON-NLS-1$
	protected static final String ELEM_OSMCHANGE = "osmChange"; //$NON-NLS-1$
	protected static final String ELEM_NODE = "node"; //$NON-NLS-1$
	protected static final String ELEM_TAG = "tag"; //$NON-NLS-1$
	protected static final String ELEM_WAY = "way"; //$NON-NLS-1$
	protected static final String ELEM_ND = "nd"; //$NON-NLS-1$
	protected static final String ELEM_RELATION = "relation"; //$NON-NLS-1$
	protected static final String ELEM_MEMBER = "member"; //$NON-NLS-1$
	protected static final String ELEM_MODIFY = "modify"; //$NON-NLS-1$
	protected static final String ELEM_CREATE = "create"; //$NON-NLS-1$
	protected static final String ELEM_DELETE = "delete"; //$NON-NLS-1$
	
	
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
	protected int currentModify = 0;
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
	protected boolean convertTagsToLC = true;
	protected boolean parseEntityInfo;
	
	
	
	public static void main(String[] args) throws IOException, SAXException, XmlPullParserException {
		GZIPInputStream is = new GZIPInputStream(
				new FileInputStream("/Users/victorshcherb/osmand/temp/m.m001508233.osc.gz"));
		new OsmBaseStorage().parseOSM(is, IProgress.EMPTY_PROGRESS);
	}
	
	public synchronized void parseOSM(InputStream stream, IProgress progress, InputStream streamForProgress, 
			boolean entityInfo) throws IOException, XmlPullParserException {
		this.inputStream = stream;
		this.progress = progress;
		parseEntityInfo = entityInfo;
		if(streamForProgress == null){
			streamForProgress = inputStream;
		}
		this.streamForProgress = streamForProgress;
		parseStarted = false;
		entities.clear();
		this.entityInfo.clear();
		if(progress != null){
			progress.startWork(streamForProgress.available());
		}
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(stream, "UTF-8");
		int tok;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				startElement(parser, parser.getName());
			} else if (tok == XmlPullParser.END_TAG) {
				endElement(parser, parser.getName());
			}
		}
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
	public synchronized void parseOSM(InputStream stream, IProgress progress) throws IOException, XmlPullParserException {
		parseOSM(stream, progress, null, true);
		
	}
	
	public void setConvertTagsToLC(boolean convertTagsToLC) {
		this.convertTagsToLC = convertTagsToLC;
	}
	
	public boolean isSupressWarnings() {
		return supressWarnings;
	}
	public void setSupressWarnings(boolean supressWarnings) {
		this.supressWarnings = supressWarnings;
	}
	
	private boolean osmChange;
	
	public boolean isOsmChange() {
		return osmChange;
	}
	
	protected Long parseId(XmlPullParser parser, String name, long defId){
		long id = defId; 
		String value = parser.getAttributeValue("",name);
		try {
			id = Long.parseLong(value);
		} catch (NumberFormatException e) {
		}
		return id;
	}
	
	protected double parseDouble(XmlPullParser parser, String name, double defVal){
		double ret = defVal; 
		String value = parser.getAttributeValue("", name);
		if(value == null) {
			return defVal;
		}
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
	
	protected void initRootElement(XmlPullParser parser, String name) throws OsmVersionNotSupported {
		if ((!ELEM_OSM.equals(name) && !ELEM_OSMCHANGE.equals(name))
				|| !supportedVersions.contains(parser.getAttributeValue("", ATTR_VERSION))) {
			throw new OsmVersionNotSupported();
		}
		osmChange = ELEM_OSMCHANGE.equals(name);
		parseStarted = true;
	}
	
	protected static final int moduleProgress = 1 << 10;
	
	public void startElement(XmlPullParser parser, String name)  {
		if (!parseStarted) {
			initRootElement(parser, name);
		}
		if (ELEM_MODIFY.equals(name) ) {
			currentModify = Entity.MODIFY_MODIFIED;
		} else if (ELEM_CREATE.equals(name) ) {
			currentModify = Entity.MODIFY_CREATED;
		} else if (ELEM_DELETE.equals(name) ) {
			currentModify = Entity.MODIFY_DELETED;
		} else if (currentParsedEntity == null) {
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
				currentParsedEntity = new Node(parseDouble(parser, ATTR_LAT, 0), parseDouble(parser, ATTR_LON, 0),
						parseId(parser, ATTR_ID, -1));
				currentParsedEntity.setVersion(parseVersion(parser));
			} else if (ELEM_WAY.equals(name)) {
				currentParsedEntity = new Way(parseId(parser, ATTR_ID, -1));
				currentParsedEntity.setVersion(parseVersion(parser));
			} else if (ELEM_RELATION.equals(name)) {
				currentParsedEntity = new Relation(parseId(parser, ATTR_ID, -1));
			} else {
				// this situation could be logged as unhandled
			}
			if (currentParsedEntity != null) {
				currentParsedEntity.setModify(currentModify);
				if (parseEntityInfo) {
					currentParsedEntityInfo = new EntityInfo();
					currentParsedEntityInfo.setChangeset(parser.getAttributeValue("",ATTR_CHANGESET));
					currentParsedEntityInfo.setTimestamp(parser.getAttributeValue("",ATTR_TIMESTAMP));
					currentParsedEntityInfo.setUser(parser.getAttributeValue("",ATTR_USER));
					currentParsedEntityInfo.setVersion(parser.getAttributeValue("",ATTR_VERSION));
					currentParsedEntityInfo.setVisible(parser.getAttributeValue("",ATTR_VISIBLE));
					currentParsedEntityInfo.setUid(parser.getAttributeValue("",ATTR_UID));
				}
			}
		} else {
			if (ELEM_TAG.equals(name)) {
				String key = parser.getAttributeValue("",ATTR_K);
				if(key != null){
					if(convertTagsToLC) {
						currentParsedEntity.putTag(key, parser.getAttributeValue("",ATTR_V));
					} else {
						currentParsedEntity.putTagNoLC(key, parser.getAttributeValue("",ATTR_V));
					}
				}
			} else if (ELEM_ND.equals(name)) {
				Long id = parseId(parser, ATTR_REF, -1);
				if(id != -1 && currentParsedEntity instanceof Way){
					((Way)currentParsedEntity).addNode(id);
				}
			} else if (ELEM_MEMBER.equals(name)) {
				try {
					Long id = parseId(parser, ATTR_REF, -1);
					if (id != -1 && currentParsedEntity instanceof Relation) {
						EntityType type = EntityType.valueOf(parser.getAttributeValue("",ATTR_TYPE).toUpperCase());
						((Relation) currentParsedEntity).addMember(id, type, parser.getAttributeValue("",ATTR_ROLE));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}  else {
				// this situation could be logged as unhandled
			}
		}
	}
	
	private int parseVersion(XmlPullParser parser) {
		if (parser.getAttributeName(parser.getAttributeCount() - 1).equals(ATTR_VERSION)) {
			return Integer.valueOf(parser.getAttributeValue(parser.getAttributeCount() - 1));
		}
		return 0;
	}

	public void endElement(XmlPullParser parser, String name) {
		EntityType type = null;
		if (ELEM_NODE.equals(name)){
			type = EntityType.NODE; 
		} else if (ELEM_WAY.equals(name)){
			type = EntityType.WAY;
		} else if (ELEM_RELATION.equals(name)){
			type = EntityType.RELATION;
		} else if (ELEM_MODIFY.equals(name) ) {
			currentModify = 0;
		} else if (ELEM_CREATE.equals(name) ) {
			currentModify = 0;
		} else if (ELEM_DELETE.equals(name) ) {
			currentModify = 0;
		}
		if (type != null) {
			if(currentParsedEntity != null){
				EntityId entityId = new EntityId(type, currentParsedEntity.getId());
				if (acceptEntityToLoad(entityId, currentParsedEntity)) {
					Entity oldEntity = entities.put(entityId, currentParsedEntity);
					if (parseEntityInfo && currentParsedEntityInfo != null) {
						entityInfo.put(entityId, currentParsedEntityInfo);
					}
					if (!supressWarnings && oldEntity != null) {
						throw new UnsupportedOperationException(
								"Entity with id=" + oldEntity.getId() + " is duplicated in osm map"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} else {
//					System.gc();
				}
				currentParsedEntity = null;
			}
		}
    }

    public void registerEntity(Entity entity, EntityInfo info) {
        entities.put(EntityId.valueOf(entity), entity);
        if (info != null) {
            entityInfo.put(EntityId.valueOf(entity), info);
        }
    }
	
	
	protected boolean acceptEntityToLoad(EntityId entityId, Entity entity) {
		for (IOsmStorageFilter f : filters) {
			if (!f.acceptEntityToLoad(this, entityId, entity)) {
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
	

	/**
	 * Thrown when version is not supported
	 */
	public static class OsmVersionNotSupported extends RuntimeException {
		private static final long serialVersionUID = -127558215143984838L;

	}

}
