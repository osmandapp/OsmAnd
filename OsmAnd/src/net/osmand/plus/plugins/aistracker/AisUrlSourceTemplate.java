package net.osmand.plus.plugins.aistracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * A ready-made "add source" starting point offered when creating a new {@link AisUrlSource}
 * (name/URL/type prefilled, still fully editable before saving), the same idea as picking a
 * known online routing engine type before filling in your own API key.
 */
public class AisUrlSourceTemplate {

	@NonNull
	public final String displayName;
	@NonNull
	public final AisUrlSource.Type type;
	@NonNull
	public final String urlTemplate;
	/** Null when the template needs no key; otherwise the label for an extra "key" input whose
	 * value replaces {@code {API_KEY}} in {@link #urlTemplate} before the source is saved. */
	@Nullable
	public final String apiKeyLabel;
	@NonNull
	public final String description;

	public AisUrlSourceTemplate(@NonNull String displayName, @NonNull AisUrlSource.Type type,
	                             @NonNull String urlTemplate, @Nullable String apiKeyLabel,
	                             @NonNull String description) {
		this.displayName = displayName;
		this.type = type;
		this.urlTemplate = urlTemplate;
		this.apiKeyLabel = apiKeyLabel;
		this.description = description;
	}

	@NonNull
	public static List<AisUrlSourceTemplate> all() {
		return Arrays.asList(
				new AisUrlSourceTemplate(
						"OpenSky Network - bounding box (free, anonymous)",
						AisUrlSource.Type.PLANES,
						"https://opensky-network.org/api/states/all?lamin=40&lomin=-10&lamax=55&lomax=20",
						null,
						"No key needed. ~400 requests/day anonymously. Edit lamin/lomin/lamax/lomax "
								+ "in the URL below to your region before saving."),
				new AisUrlSourceTemplate(
						"OpenSky Network - whole world (free, anonymous)",
						AisUrlSource.Type.PLANES,
						"https://opensky-network.org/api/states/all",
						null,
						"No key needed, but a global query costs more of the daily anonymous quota "
								+ "per request than a bounding-box one - prefer the bounding-box template."),
				new AisUrlSourceTemplate(
						"Private aggregator (key in URL)",
						AisUrlSource.Type.PLANES,
						"https://example.com/api/planes?key={API_KEY}",
						"API key",
						"Template for your own server/aggregator that expects the key as a query "
								+ "parameter. Edit the URL below (host, path, params) to match your server."),
				new AisUrlSourceTemplate(
						"Custom",
						AisUrlSource.Type.PLANES,
						"",
						null,
						"Start blank and fill in everything yourself.")
		);
	}
}
