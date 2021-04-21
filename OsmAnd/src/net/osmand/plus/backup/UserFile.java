package net.osmand.plus.backup;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class UserFile {

	private int userid;
	private long id;
	private int deviceid;
	private int filesize;
	private String type;
	private String name;
	private Date updatetime;
	private long updatetimems;
	private Date clienttime;
	private long clienttimems;
	private int zipSize;

	public UserFile(@NonNull JSONObject json) throws JSONException, ParseException {
		if (json.has("userid")) {
			userid = json.getInt("userid");
		}
		if (json.has("id")) {
			id = json.getLong("id");
		}
		if (json.has("deviceid")) {
			deviceid = json.getInt("deviceid");
		}
		if (json.has("filesize")) {
			filesize = json.getInt("filesize");
		}
		if (json.has("type")) {
			type = json.getString("type");
		}
		if (json.has("name")) {
			name = json.getString("name");
		}
		if (json.has("updatetimems")) {
			updatetimems = json.getLong("updatetimems");
			updatetime = new Date(updatetimems);
		}
		if (json.has("clienttimems")) {
			clienttimems = json.getLong("clienttimems");
			clienttime = new Date(clienttimems);
		}
		if (json.has("zipSize")) {
			zipSize = json.getInt("zipSize");
		}
	}

	public int getUserid() {
		return userid;
	}

	public long getId() {
		return id;
	}

	public int getDeviceid() {
		return deviceid;
	}

	public int getFilesize() {
		return filesize;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Date getUpdatetime() {
		return updatetime;
	}

	public long getUpdatetimems() {
		return updatetimems;
	}

	public Date getClienttime() {
		return clienttime;
	}

	public long getClienttimems() {
		return clienttimems;
	}

	public int getZipSize() {
		return zipSize;
	}
}
