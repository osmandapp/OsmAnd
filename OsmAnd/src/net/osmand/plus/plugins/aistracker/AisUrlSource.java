package net.osmand.plus.plugins.aistracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A user-configured "pull data from this URL" source for the AIS layer - a name + URL + type
 * (PLANES/SHIPS), added/edited/removed one at a time through a dialog in the plugin's settings,
 * the same way an online routing engine or a custom raster map source is configured. Stored as a
 * JSON array in a single string preference (same storage pattern as
 * {@code OsmandSettings.ONLINE_ROUTING_ENGINES}).
 */
public class AisUrlSource {

	public enum Type {
		PLANES,
		SHIPS
	}

	@NonNull
	public final String id;
	@NonNull
	public final Type type;
	@NonNull
	public final String name;
	@NonNull
	public final String url;
	/**
	 * Optional credential. Substituted into {@link #url} in place of {@code {API_KEY}} when the
	 * URL carries that placeholder, otherwise sent as a header for hosts that expect one
	 * (see AisPlaneDataFetcher). May be empty - sources that need no key still work.
	 */
	@NonNull
	public final String apiKey;
	/** Per-source visibility, toggled from the "AIS sources" picker in Configure Map. */
	public final boolean enabled;

	public AisUrlSource(@NonNull String id, @NonNull Type type, @NonNull String name,
	                     @NonNull String url, @NonNull String apiKey, boolean enabled) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.url = url;
		this.apiKey = apiKey;
		this.enabled = enabled;
	}

	@NonNull
	public static AisUrlSource create(@NonNull Type type, @NonNull String name, @NonNull String url,
	                                   @NonNull String apiKey) {
		return new AisUrlSource(UUID.randomUUID().toString(), type, name, url, apiKey, true);
	}

	@NonNull
	public AisUrlSource withValues(@NonNull Type type, @NonNull String name, @NonNull String url,
	                                @NonNull String apiKey) {
		return new AisUrlSource(id, type, name, url, apiKey, enabled);
	}

	@NonNull
	public AisUrlSource withEnabled(boolean enabled) {
		return new AisUrlSource(id, type, name, url, apiKey, enabled);
	}

	@NonNull
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("type", type.name());
		json.put("name", name);
		json.put("url", url);
		json.put("apiKey", apiKey);
		json.put("enabled", enabled);
		return json;
	}

	@Nullable
	private static AisUrlSource fromJson(@NonNull JSONObject json) {
		String id = json.optString("id", "");
		String typeStr = json.optString("type", "");
		String name = json.optString("name", "");
		String url = json.optString("url", "");
		String apiKey = json.optString("apiKey", "");
		boolean enabled = json.optBoolean("enabled", true);
		if (Algorithms.isEmpty(id) || Algorithms.isEmpty(name) || Algorithms.isEmpty(url)) {
			return null;
		}
		Type type;
		try {
			type = Type.valueOf(typeStr);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return new AisUrlSource(id, type, name, url, apiKey, enabled);
	}

	@NonNull
	public static List<AisUrlSource> parseList(@Nullable String json) {
		List<AisUrlSource> result = new ArrayList<>();
		if (Algorithms.isEmpty(json)) {
			return result;
		}
		try {
			JSONArray array = new JSONArray(json);
			for (int i = 0; i < array.length(); i++) {
				AisUrlSource source = fromJson(array.getJSONObject(i));
				if (source != null) {
					result.add(source);
				}
			}
		} catch (JSONException e) {
			// ignore malformed preference value
		}
		return result;
	}

	@NonNull
	public static String serializeList(@NonNull List<AisUrlSource> sources) {
		JSONArray array = new JSONArray();
		for (AisUrlSource source : sources) {
			try {
				array.put(source.toJson());
			} catch (JSONException e) {
				// skip
			}
		}
		return array.toString();
	}
}
