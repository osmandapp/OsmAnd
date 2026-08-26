package net.osmand.plus.backup;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteFile {

	private int userid;
	private long id;
	private int deviceid;
	private int filesize;
	private String type;
	private String name;
	private long updatetimems;
	private long clienttimems;
	private int zipSize;

	public SettingsItem item;

	public RemoteFile(@NonNull JSONObject json) throws JSONException {
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
		}
		if (json.has("clienttimems")) {
			clienttimems = json.getLong("clienttimems");
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

	public boolean isDeleted() {
		return filesize < 0;
	}

	public boolean isInfoFile() {
		return name != null && name.endsWith(BackupHelper.INFO_EXT);
	}

	public boolean isRecordedVoiceFile() {
		return name != null
				&& name.startsWith(FileSettingsItem.FileSubtype.VOICE.getSubtypeFolder())
				&& !name.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS);
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getTypeNamePath() {
		if (!Algorithms.isEmpty(name)) {
			return type + (name.charAt(0) == '/' ? name : "/" + name);
		} else {
			return type;
		}
	}

	public long getUpdatetimems() {
		return updatetimems;
	}

	public long getClienttimems() {
		return clienttimems;
	}

	public int getZipSize() {
		return zipSize;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RemoteFile that = (RemoteFile) o;
		return id == that.id &&
				userid == that.userid &&
				deviceid == that.deviceid &&
				filesize == that.filesize &&
				updatetimems == that.updatetimems &&
				clienttimems == that.clienttimems &&
				Algorithms.objectEquals(type, that.type) &&
				Algorithms.objectEquals(name, that.name);
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + Long.hashCode(id);
		result = 31 * result + userid;
		result = 31 * result + deviceid;
		result = 31 * result + filesize;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + Long.hashCode(updatetimems);
		result = 31 * result + Long.hashCode(updatetimems);
		result = 31 * result + Long.hashCode(clienttimems);
		result = 31 * result + Long.hashCode(clienttimems);
		return result;
	}

	@NonNull
	@Override
	public String toString() {
		return type + "/" + name + " (" + filesize + ") clientTime=" + clienttimems
				+ " updateTime=" + updatetimems + " settingsItem=" + item;
	}
}
