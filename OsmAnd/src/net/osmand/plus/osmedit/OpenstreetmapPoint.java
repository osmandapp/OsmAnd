package net.osmand.plus.osmedit;

import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;

public class OpenstreetmapPoint extends OsmPoint {
	private static final long serialVersionUID = 729654300829771467L;
	private Node entity;
	private String comment;

	public OpenstreetmapPoint(){
	}

	@Override
	public long getId() {
		return entity.getId();
	}

	public String getName() {
		String ret = entity.getTag(OSMTagKey.NAME.getValue());
		if (ret == null)
			return "";
		return ret;
	}

	public String getType() {
		String type = "amenity";
		for (String k : entity.getTagKeySet()) {
			if (!OSMTagKey.NAME.getValue().equals(k) && !OSMTagKey.OPENING_HOURS.getValue().equals(k)) {
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
