package net.osmand.osm;

/**
 * Additional entity info
 */
public class EntityInfo {
	String timestamp;
	String uid;
	String user;
	String visible;
	String version;
	String changeset;
	String action;
	
	
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getVisible() {
		return visible;
	}
	public void setVisible(String visible) {
		this.visible = visible;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getChangeset() {
		return changeset;
	}
	public void setChangeset(String changeset) {
		this.changeset = changeset;
	}

}
