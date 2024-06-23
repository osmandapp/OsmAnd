package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;

import androidx.annotation.NonNull;

import net.osmand.ColorPalette;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;

import org.json.JSONException;
import org.json.JSONObject;

public class PaletteGradientColor extends PaletteColor {

	public static String DEFAULT_NAME = "default";

	private String typeName;
	private String paletteName;
	private ColorPalette colorPalette;

	public PaletteGradientColor(@NonNull String paletteName, @NonNull String typeName, @NonNull ColorPalette colorPalette, long creationTime) {
		super(typeName + GRADIENT_ID_SPLITTER + paletteName, 0, creationTime);
		this.paletteName = paletteName;
		this.typeName = typeName;
		this.colorPalette = colorPalette;
	}

	public PaletteGradientColor(@NonNull String paletteName, @NonNull String typeName, @NonNull ColorPalette colorPalette, long creationTime, long lastUsedTime) {
		super(typeName + GRADIENT_ID_SPLITTER + paletteName, 0, creationTime);
		this.colorPalette = colorPalette;
		this.paletteName = paletteName;
		this.typeName = typeName;
		this.lastUsedTime = lastUsedTime;
	}

	public PaletteGradientColor(@NonNull JSONObject jsonObject) throws JSONException {
		super(jsonObject.getString(ID), 0, 0);
		if (jsonObject.has(CREATION_TIME)) {
			creationTime = jsonObject.getLong(CREATION_TIME);
		} else {
			creationTime = 0;
		}
		if (jsonObject.has(LAST_USED_TIME)) {
			lastUsedTime = jsonObject.getLong(LAST_USED_TIME);
		}
	}

	@NonNull
	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(ID, id);
		if (creationTime > 0) {
			jsonObject.put(CREATION_TIME, creationTime);
		}
		if (lastUsedTime > 0) {
			jsonObject.put(LAST_USED_TIME, lastUsedTime);
		}
		return jsonObject;
	}

	@NonNull
	public String getPaletteName() {
		return paletteName;
	}

	@NonNull
	public String getTypeName() {
		return typeName;
	}

	@NonNull
	public ColorPalette getColorPalette() {
		return colorPalette;
	}
}
