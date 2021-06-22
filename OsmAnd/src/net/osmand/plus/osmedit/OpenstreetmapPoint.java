package net.osmand.plus.osmedit;

import net.osmand.data.LatLon;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.Algorithms;

import java.util.Set;

public class OpenstreetmapPoint extends OsmPoint {
	private static final long serialVersionUID = 729654300829771467L;
	private Entity entity;
	private String comment;

	public OpenstreetmapPoint() {
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
					!k.startsWith(Entity.REMOVE_TAG_PREFIX)) {
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


	public Entity getEntity() {
		return entity;
	}

	public String getComment() {
		return comment;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getTagsString() {
		StringBuilder sb = new StringBuilder();
		for (String tag : entity.getTagKeySet()) {
			String val = entity.getTag(tag);
			if (entity.isNotValid(tag)) {
				continue;
			}
			sb.append(tag).append(" : ");
			sb.append(val).append("; ");
		}
		return sb.toString();
	}

	public void trimChangedTagNamesValues() {
		Entity entity = getEntity();
		if (entity == null) {
			return;
		}
		Set<String> changedTags = entity.getChangedTags();
		if (changedTags != null) {
			for (String tag : changedTags) {
				if (tag == null || tag.trim().equals(tag)) {
					continue;
				}
				String trimmedTag = tag.trim();
				changedTags.remove(tag);
				changedTags.add(trimmedTag);

				if (entity.getTags().containsKey(trimmedTag)) {
					String tagValue = entity.getTag(trimmedTag);
					entity.putTag(trimmedTag, tagValue == null ? null : tagValue.trim());
				} else if (entity.getTags().containsKey(tag)) {
					String tagValue = entity.getTag(tag);
					entity.removeTag(tag);
					entity.putTag(trimmedTag, tagValue == null ? null : tagValue.trim());
				}
			}
		}
	}

	@Override
	public String toString() {
		return new StringBuffer("Openstreetmap Point ").append(this.getAction()).append(" ").append(this.getName())
			.append(" (").append(this.getId()).append("): [")
			.append(this.getType()).append("/").append(this.getSubtype())
			.append(" (").append(this.getLatitude()).append(", ").append(this.getLongitude())
			.append(")]").toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof OpenstreetmapPoint)) {
			return false;
		}
		OpenstreetmapPoint otherPoint = (OpenstreetmapPoint) other;
		boolean res = this.getName() != null && this.getName().equals(otherPoint.getName());
		LatLon thisLatLon = new LatLon(this.getLatitude(), this.getLongitude());
		LatLon otherLatLon = new LatLon(otherPoint.getLatitude(), otherPoint.getLongitude());
		res = res && thisLatLon.equals(otherLatLon);
		if (getType() != null)
			res = res && getType().equals(otherPoint.getType());
		if (getSubtype() != null)
			res = res && getSubtype().equals(otherPoint.getSubtype());
		if (getTagsString() != null)
			res = res && getTagsString().equals(otherPoint.getTagsString());
		res = res && getId() == otherPoint.getId();
		return res;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		int temp;
		temp = (int) Math.floor(getLatitude() * 10000);
		result = prime * result + temp;
		temp = (int) Math.floor(getLongitude() * 10000);
		result = prime * result + temp;
		result = prime * result + (getType() != null ? getType().hashCode() : 0);
		result = prime * result + (getSubtype() != null ? getSubtype().hashCode() : 0);
		result = prime * result + (getTagsString() != null ? getTagsString().hashCode() : 0);
		result = prime * result + Long.valueOf(getId()).hashCode();
		return result;
	}
}
