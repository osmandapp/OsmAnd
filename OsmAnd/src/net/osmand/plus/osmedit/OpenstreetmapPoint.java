package net.osmand.plus.osmedit;

import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.Algorithms;

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
			if (!OSMTagKey.NAME.getValue().equals(k) && !OSMTagKey.OPENING_HOURS.getValue().equals(k) && 
					!k.startsWith(EditPoiData.REMOVE_TAG_PREFIX)) {
				type = k;
				break;
			}
		}
		return type;
	}

	public String getSubtype() {
		if(Algorithms.isEmpty(getType())) {
			return "";
		}
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
		return "Openstreetmap Point " + this.getAction() + " " + this.getName() + " (" + this.getId() + "): [" + this.getType() + "/" + this.getSubtype() + " (" + this.getLatitude() + ", " + this.getLongitude() + ")]";
	}
}
