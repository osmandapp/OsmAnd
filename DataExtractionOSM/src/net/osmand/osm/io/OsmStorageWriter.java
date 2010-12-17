package net.osmand.osm.io;

import static net.osmand.osm.io.OsmBaseStorage.ATTR_CHANGESET;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_ID;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_K;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_LAT;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_LON;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_REF;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_ROLE;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_TIMESTAMP;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_TYPE;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_UID;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_USER;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_V;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_VERSION;
import static net.osmand.osm.io.OsmBaseStorage.ATTR_VISIBLE;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_MEMBER;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_ND;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_NODE;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_OSM;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_RELATION;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_TAG;
import static net.osmand.osm.io.OsmBaseStorage.ELEM_WAY;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;

import net.osmand.Algoritms;
import net.osmand.data.MapObject;
import net.osmand.osm.Entity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;

public class OsmStorageWriter {

	private final String INDENT = "    ";
	private final String INDENT2 = INDENT + INDENT;


	public OsmStorageWriter(){
	}
	
	
	public void saveStorage(OutputStream output, OsmBaseStorage storage, Collection<EntityId> interestedObjects, boolean includeLinks) throws XMLStreamException, IOException {
		Map<EntityId, Entity> entities = storage.getRegisteredEntities();
		Map<EntityId, EntityInfo> entityInfo = storage.getRegisteredEntityInfo();
//		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        String indent = "{http://xml.apache.org/xslt}indent-amount";
//        transformer.setOutputProperty(indent, "4");
                XMLOutputFactory xof = XMLOutputFactory.newInstance();
                XMLStreamWriter streamWriter = xof.createXMLStreamWriter(new OutputStreamWriter(output));
 
		List<Node> nodes = new ArrayList<Node>();
		List<Way> ways = new ArrayList<Way>();
		List<Relation> relations = new ArrayList<Relation>();
		if(interestedObjects == null){
			interestedObjects = entities.keySet();
		}
		Stack<EntityId> toResolve = new Stack<EntityId>();
		toResolve.addAll(interestedObjects);
		while(!toResolve.isEmpty()){
			EntityId l = toResolve.pop();
			if(entities.get(l) instanceof Node){
				nodes.add((Node) entities.get(l));
			} else if(entities.get(l) instanceof Way){
				ways.add((Way) entities.get(l));
				if(includeLinks){
					toResolve.addAll(((Way)entities.get(l)).getEntityIds());
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
			writeEntityAttributes(streamWriter, n, entityInfo.get(EntityId.valueOf(n)));
			writeTags(streamWriter, n);
			writeEndElement(streamWriter, INDENT);
		}
		
		for(Way w : ways){
			writeStartElement(streamWriter, ELEM_WAY, INDENT);
			streamWriter.writeAttribute(ATTR_ID, w.getId()+"");
			writeEntityAttributes(streamWriter, w, entityInfo.get(EntityId.valueOf(w)));
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
			writeEntityAttributes(streamWriter, r, entityInfo.get(EntityId.valueOf(r)));
			for(Entry<EntityId, String> e : r.getMembersMap().entrySet()){
				writeStartElement(streamWriter, ELEM_MEMBER, INDENT2);
				streamWriter.writeAttribute(ATTR_REF, e.getKey().getId()+"");
				String s = e.getValue();
				if(s == null){
					s = ""; 
				}
				streamWriter.writeAttribute(ATTR_ROLE, s);
				streamWriter.writeAttribute(ATTR_TYPE, e.getKey().getType().toString().toLowerCase());
				writeEndElement(streamWriter, INDENT2);
			}
			writeTags(streamWriter, r);
			writeEndElement(streamWriter, INDENT);
		}
		
		writeEndElement(streamWriter, ""); // osm
		streamWriter.writeEndDocument();
		streamWriter.flush();
	}
	
	private void writeEntityAttributes(XMLStreamWriter writer, Entity i, EntityInfo info) throws XMLStreamException{
		if(i.getId() < 0 && (info == null || info.getAction() == null)){
			writer.writeAttribute("action", "modify");
		}
		if(info != null){
			// for josm editor
			if(info.getAction() != null){
				writer.writeAttribute("action", info.getAction());
			}
			if(info.getChangeset() != null){
				writer.writeAttribute(ATTR_CHANGESET, info.getChangeset());
			}
			if(info.getTimestamp() != null){
				writer.writeAttribute(ATTR_TIMESTAMP, info.getTimestamp());
			}
			if(info.getUid() != null){
				writer.writeAttribute(ATTR_UID, info.getUid());
			}
			if(info.getUser() != null){
				writer.writeAttribute(ATTR_USER, info.getUser());
			}
			if(info.getVisible() != null){
				writer.writeAttribute(ATTR_VISIBLE, info.getVisible());
			}
			if(info.getVersion() != null){
				writer.writeAttribute(ATTR_VERSION, info.getVersion());
			}
		}
	}
	
	public boolean couldBeWrited(MapObject e){
		if(!Algoritms.isEmpty(e.getName()) && e.getLocation() != null){
			return true;
		}
		return false;
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

