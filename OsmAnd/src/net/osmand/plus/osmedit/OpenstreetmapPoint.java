package net.osmand.plus.osmedit;

import java.io.Serializable;

import net.osmand.data.AmenityType;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;

public class OpenstreetmapPoint extends OsmPoint implements Serializable {
	private static final long serialVersionUID = 729654300829771467L;
	private Node entity;
	private String comment;

	public OpenstreetmapPoint(){
	}

	@Override
	public long getId() {
		return entity.getId();
	}
	
	@Override
	public void updateID(long newID) {
		if (getId()  < 0) {
			entity = new Node(entity, newID);
		}
	}

	public String getName() {
		String ret = entity.getTag(OSMTagKey.NAME.getValue());
		if (ret == null)
			return "";
		return ret;
	}

	public String getType() {
		String type = AmenityType.valueToString(AmenityType.OTHER);
		for(String k : entity.getTagKeySet()){
			if (!OSMTagKey.NAME.getValue().equals(k) &&
				!OSMTagKey.OPENING_HOURS.getValue().equals(k)) {
				type = k;
				break;
			}
		}
		return type;
	}

	public String getSubtype() {
		return entity.getTag(this.getType());
	}

	@Override
	public double getLatitude() {
		return entity.getLatitude();
	}

	@Override
	public double getLongitude() {
		return entity.getLongitude();
	}

	@Override
	public Group getGroup() {
		return Group.POI;
	}


	public Node getEntity() {
		return entity;
	}

	public String getComment() {
		return comment;
	}

	public void setEntity(Node entity) {
		this.entity = entity;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public String toString() {
		return new StringBuffer("Openstreetmap Point ").append(this.getAction()).append(" ").append(this.getName())
			.append(" (").append(this.getId()).append("): [")
			.append(this.getType()).append("/").append(this.getSubtype())
			.append(" (").append(this.getLatitude()).append(", ").append(this.getLongitude())
			.append(")]").toString();
	}
}
