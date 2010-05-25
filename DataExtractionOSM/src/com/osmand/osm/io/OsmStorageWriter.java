package com.osmand.osm.io;

import static com.osmand.osm.io.OsmBaseStorage.ATTR_ID;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_K;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_LAT;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_LON;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_REF;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_ROLE;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_TYPE;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_V;
import static com.osmand.osm.io.OsmBaseStorage.ATTR_VERSION;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_MEMBER;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_ND;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_NODE;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_OSM;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_RELATION;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_TAG;
import static com.osmand.osm.io.OsmBaseStorage.ELEM_WAY;
import static com.osmand.osm.io.OsmIndexStorage.ATTR_CITYTYPE;
import static com.osmand.osm.io.OsmIndexStorage.ATTR_NAME;
import static com.osmand.osm.io.OsmIndexStorage.ATTR_SUBTYPE;
import static com.osmand.osm.io.OsmIndexStorage.ELEM_AMENITY;
import static com.osmand.osm.io.OsmIndexStorage.ELEM_BUILDING;
import static com.osmand.osm.io.OsmIndexStorage.ELEM_CITY;
import static com.osmand.osm.io.OsmIndexStorage.ELEM_OSMAND;
import static com.osmand.osm.io.OsmIndexStorage.ELEM_STREET;
import static com.osmand.osm.io.OsmIndexStorage.OSMAND_VERSION;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import com.osmand.Algoritms;
import com.osmand.LogUtil;
import com.osmand.data.Amenity;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.MapObject;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.Amenity.AmenityType;
import com.osmand.data.City.CityType;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.Node;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.xml.internal.stream.writers.XMLStreamWriterImpl;

public class OsmStorageWriter {

	private final String INDENT = "    ";
	private final String INDENT2 = INDENT + INDENT;
	private final String INDENT3 = INDENT + INDENT + INDENT;
	private static final Log log = LogUtil.getLog(OsmStorageWriter.class);
	
	private static final Version VERSION = Version.LUCENE_30;


	public OsmStorageWriter(){
	}
	
	
	public void saveStorage(OutputStream output, OsmBaseStorage storage, Collection<Long> interestedObjects, boolean includeLinks) throws XMLStreamException, IOException {
		Map<Long, Entity> entities = storage.getRegisteredEntities();
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
			writeEndElement(streamWriter, INDENT);
		}
		
		for(Way w : ways){
			writeStartElement(streamWriter, ELEM_WAY, INDENT);
			streamWriter.writeAttribute(ATTR_ID, w.getId()+"");
			for(Long r : w.getNodeIds()){
				writeStartElement(streamWriter, ELEM_ND, INDENT2);
				streamWriter.writeAttribute(ATTR_REF, r+"");
				writeEndElement(streamWriter, INDENT2);
			}
			writeTags(streamWriter, w);
			writeEndElement(streamWriter, INDENT);
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
				streamWriter.writeAttribute(ATTR_TYPE, getEntityType(entities, e.getKey()));
				writeEndElement(streamWriter, INDENT2);
			}
			writeTags(streamWriter, r);
			writeEndElement(streamWriter, INDENT);
		}
		
		writeEndElement(streamWriter, ""); // osm
		streamWriter.writeEndDocument();
		streamWriter.flush();
	}
	
	private String getEntityType(Map<Long, Entity> entities , Long id){
		Entity e = entities.get(id);
		if(e instanceof Way){
			return "way";
		} else if(e instanceof Relation){
			return "relation";
		} 
		return "node";
	}
	
	public void saveLuceneIndex(File dir, Region region) throws CorruptIndexException, LockObtainFailedException, IOException{
		long now = System.currentTimeMillis();
		IndexWriter writer = null;
		try {
			// Make a lucene writer and create new Lucene index with arg3 = true
			writer = new IndexWriter(FSDirectory.open(dir), new StandardAnalyzer(VERSION), true, MaxFieldLength.LIMITED);
			for (Amenity a : region.getAmenityManager().getAllObjects()) {
				index(a, writer);
			}
			writer.optimize();
		} finally {
			try {
				writer.close();
			} catch (Exception t) {
				log.error("Failed to close index.", t);
				throw new RuntimeException(t);
			}
			log.info(String.format("Indexing done in %s ms.", System.currentTimeMillis() - now));
		}

	}
	
	// TODO externalize strings
	protected void index(Amenity amenity, IndexWriter writer) throws CorruptIndexException, IOException {
		Document document = new Document();
		document.add(new Field("id",""+amenity.getEntity().getId(),Store.YES, Index.NOT_ANALYZED));
		LatLon latLon = amenity.getEntity().getLatLon();
		document.add(new Field("name",amenity.getName(),Store.YES, Index.NOT_ANALYZED));
		document.add(new Field("longitude",OsmLuceneRepository.formatLongitude(latLon.getLongitude()),Store.YES, Index.ANALYZED));
		document.add(new Field("latitude",OsmLuceneRepository.formatLatitude(latLon.getLatitude()),Store.YES, Index.ANALYZED));
		document.add(new Field("type",amenity.getType().name(),Store.YES, Index.ANALYZED));
		document.add(new Field("subtype",amenity.getSubType(),Store.YES, Index.ANALYZED));
		//for (Entry<String, String> entry:amenity.getNode().getTags().entrySet()) {
		//	document.add(new Field(entry.getKey(),entry.getValue(),Store.YES, Index.NOT_ANALYZED));
		//}
		writer.addDocument(document);
	}
	
	
	public void saveSQLLitePOIIndex(File file, Region region) throws  SQLException{
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e);
			throw new IllegalStateException(e);
		}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());

        final int batchSize = 500;
		try {
			Statement stat = conn.createStatement();
	        stat.executeUpdate("create table poi (id long, latitude double, longitude double, name, type, subtype);");
	        stat.executeUpdate("create index LatLonIndex ON poi (latitude, longitude);");
	        PreparedStatement prep = conn.prepareStatement(
	            "insert into poi values (?, ?, ?, ?, ? ,? );");
	        conn.setAutoCommit(false);
	        int currentCount = 0;
			for (Amenity a : region.getAmenityManager().getAllObjects()) {
				prep.setLong(1, a.getId());
				prep.setDouble(2, a.getLocation().getLatitude());
				prep.setDouble(3, a.getLocation().getLongitude());
				prep.setString(4, a.getName());
				prep.setString(5, AmenityType.valueToString(a.getType()));
				prep.setString(6, a.getSubType());
				prep.addBatch();
				currentCount++;
				if(currentCount >= batchSize){
					prep.executeBatch();
					currentCount = 0;
				}
				
			}
			if(currentCount > 0){
				prep.executeBatch();
			}
			conn.setAutoCommit(true);
		} finally {
			conn.close();
			log.info(String.format("Indexing sqllite done in %s ms.", System.currentTimeMillis() - now));
		}

	}
	
	
	public void savePOIIndex(OutputStream output, Region region) throws XMLStreamException, IOException {
		PropertyManager propertyManager = new PropertyManager(PropertyManager.CONTEXT_WRITER);
		XMLStreamWriter streamWriter = new XMLStreamWriterImpl(output, propertyManager);

		writeStartElement(streamWriter, ELEM_OSMAND, "");
		streamWriter.writeAttribute(ATTR_VERSION, OSMAND_VERSION);
		List<Amenity> amenities = region.getAmenityManager().getAllObjects();
		for(Amenity n : amenities){
			if (couldBeWrited(n)) {
				writeStartElement(streamWriter, ELEM_AMENITY, INDENT);
				writeAttributesMapObject(streamWriter, n);
				streamWriter.writeAttribute(ATTR_TYPE, AmenityType.valueToString(n.getType()));
				streamWriter.writeAttribute(ATTR_SUBTYPE, n.getSubType());
				writeEndElement(streamWriter, INDENT);
			}
		}
		writeEndElement(streamWriter, ""); // osmand
		streamWriter.writeEndDocument();
		streamWriter.flush();
	}
	
	private void writeCity(XMLStreamWriter streamWriter, City c) throws XMLStreamException{
		writeStartElement(streamWriter, ELEM_CITY, INDENT);
		writeAttributesMapObject(streamWriter, c);
		streamWriter.writeAttribute(ATTR_CITYTYPE, CityType.valueToString(c.getType()));
		for(Street s : c.getStreets()){
			if (couldBeWrited(s)) {
				writeStartElement(streamWriter, ELEM_STREET, INDENT2);
				writeAttributesMapObject(streamWriter, s);
				for(Building b : s.getBuildings()) {
					if (couldBeWrited(b)) {
						writeStartElement(streamWriter, ELEM_BUILDING, INDENT3);
						writeAttributesMapObject(streamWriter, b);
						writeEndElement(streamWriter, INDENT3);
					}
				}
				writeEndElement(streamWriter, INDENT2);
			}
		}
		writeEndElement(streamWriter, INDENT);
	}
	
	public void saveAddressIndex(OutputStream output, Region region) throws XMLStreamException, IOException {
		PropertyManager propertyManager = new PropertyManager(PropertyManager.CONTEXT_WRITER);
		XMLStreamWriter streamWriter = new XMLStreamWriterImpl(output, propertyManager);

		writeStartElement(streamWriter, ELEM_OSMAND, "");
		streamWriter.writeAttribute(ATTR_VERSION, OSMAND_VERSION);
		for(CityType t : CityType.values()){
			Collection<City> cities = region.getCitiesByType(t);
			if(cities != null){
				for(City c : cities){
					if (couldBeWrited(c)) {
						writeCity(streamWriter, c);
					}
				}
			}
		}
		writeEndElement(streamWriter, ""); // osmand
		streamWriter.writeEndDocument();
		streamWriter.flush();
	}
	
	public boolean couldBeWrited(MapObject<? extends Entity> e){
		if(!Algoritms.isEmpty(e.getName()) && e.getLocation() != null){
			return true;
		}
		return false;
	}
	
	public void writeAttributesMapObject(XMLStreamWriter streamWriter, MapObject<? extends Entity> e) throws XMLStreamException{
		LatLon location = e.getLocation();
		streamWriter.writeAttribute(ATTR_LAT, location.getLatitude()+"");
		streamWriter.writeAttribute(ATTR_LON, location.getLongitude()+"");
		streamWriter.writeAttribute(ATTR_NAME, e.getName());
		streamWriter.writeAttribute(ATTR_ID, e.getId()+"");
	}
	

	
	private void writeStartElement(XMLStreamWriter writer, String name, String indent) throws XMLStreamException{
		writer.writeCharacters("\n"+indent);
		writer.writeStartElement(name);
	}
	
	private void writeEndElement(XMLStreamWriter writer, String indent) throws XMLStreamException{
		writer.writeCharacters("\n"+indent);
		writer.writeEndElement();
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
