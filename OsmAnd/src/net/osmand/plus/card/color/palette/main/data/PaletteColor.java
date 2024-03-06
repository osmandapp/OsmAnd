package net.osmand.plus.card.color.palette.main.data;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

public class PaletteColor {

	public static final String CUSTOM_COLOR_PREFIX = "custom_";

	// JSON tags
	private static final String ID = "id";
	private static final String COLOR_HEX = "color_hex";
	private static final String CREATION_TIME = "creation_time";
	private static final String LAST_USED_TIME = "last_used_time";

	@ColorInt
	private int color;
	private final String id;
	private final long creationTime;
	private long lastUsedTime;

	public PaletteColor(@NonNull String id, @ColorInt int color, long creationTime) {
		this.id = id;
		this.color = color;
		this.creationTime = creationTime;
	}

	public PaletteColor(@NonNull JSONObject jsonObject) throws JSONException {
		id = jsonObject.getString(ID);
		String colorHex = jsonObject.getString(COLOR_HEX);
		color = Algorithms.parseColor(colorHex);
		if (jsonObject.has(CREATION_TIME)) {
			creationTime = jsonObject.getLong(CREATION_TIME);
		} else {
			creationTime = 0;
		}
		if (jsonObject.has(LAST_USED_TIME)) {
			lastUsedTime = jsonObject.getLong(LAST_USED_TIME);
		}
	}

	public String getId() {
		return id;
	}

	@ColorInt
	public int getColor() {
		return color;
	}

	public void setColor(@ColorInt int color) {
		this.color = color;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getLastUsedTime() {
		return lastUsedTime;
	}

	public void setLastUsedTime(long lastUsedTime) {
		this.lastUsedTime = lastUsedTime;
	}

	public boolean isDefault() {
		return !isCustom();
	}

	public boolean isCustom() {
		return creationTime > 0;
	}

	@NonNull
	public PaletteColor duplicate() {
		long now = System.currentTimeMillis();
		return new PaletteColor(generateId(now), color, now);
	}

	@NonNull
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.custom_color);
	}

	@NonNull
	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(ID, id);
		jsonObject.put(COLOR_HEX, Algorithms.colorToString(color));
		if (creationTime > 0) {
			jsonObject.put(CREATION_TIME, creationTime);
		}
		if (lastUsedTime > 0) {
			jsonObject.put(LAST_USED_TIME, lastUsedTime);
		}
		return jsonObject;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PaletteColor)) return false;

		PaletteColor that = (PaletteColor) o;

		return getId().equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@NonNull
	public static String generateId(long creationTime) {
		return CUSTOM_COLOR_PREFIX + creationTime;
	}
}
