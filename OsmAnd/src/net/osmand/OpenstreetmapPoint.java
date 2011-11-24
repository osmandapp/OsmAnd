package net.osmand;

import java.io.Serializable;

import net.osmand.OpenstreetmapRemoteUtil;
import net.osmand.data.AmenityType;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;

public class OpenstreetmapPoint implements Serializable {
	private static final long serialVersionUID = 729654300829771467L;
	private Node entity;
	private OpenstreetmapUtil.Action action;
	private String comment;
	private boolean stored = false;

	public OpenstreetmapPoint(){
	}

	public long getId() {
		return entity.getId();
	}

	public String getName() {
		return entity.getTag(OSMTagKey.NAME.getValue());
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

	public double getLatitude() {
		return entity.getLatitude();
	}

	public double getLongitude() {
		return entity.getLongitude();
	}

	public String getOpeninghours() {
		return entity.getTag(OSMTagKey.OPENING_HOURS.getValue());
	}

	public Node getEntity() {
		return entity;
	}

	public OpenstreetmapUtil.Action getAction() {
		return action;
	}

	public String getComment() {
		return comment;
	}

	public boolean isStored() {
		return stored;
	}

	public void setEntity(Node entity) {
		this.entity = entity;
	}

	public void setAction(String action) {
		this.action = OpenstreetmapRemoteUtil.actionString.get(action);
	}

	public void setAction(OpenstreetmapUtil.Action action) {
		this.action = action;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setStored(boolean stored) {
		this.stored = stored;
	}
}
