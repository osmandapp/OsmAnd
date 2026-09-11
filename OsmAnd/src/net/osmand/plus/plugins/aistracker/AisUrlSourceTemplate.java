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
	/** Label for the key input, or null when this source needs no key. The value is stored on the
	 * source and applied at request time - substituted into the URL when it carries
	 * {@code {API_KEY}}, sent as a header otherwise (see AisPlaneDataFetcher). */
	@Nullable
	public final String apiKeyLabel;
	/** One line on cost/access, shown next to the name while picking a template. */
	@NonNull
	public final String note;
	/** What to adjust in the prefilled URL, shown once the template is picked. */
	@NonNull
	public final String description;
	/** Project page to read up on the service, opened from the source dialog. */
	@Nullable
	public final String projectUrl;

	public AisUrlSourceTemplate(@NonNull String displayName, @NonNull AisUrlSource.Type type,
	                             @NonNull String urlTemplate, @Nullable String apiKeyLabel,
	                             @NonNull String note, @NonNull String description,
	                             @Nullable String projectUrl) {
		this.displayName = displayName;
		this.type = type;
		this.urlTemplate = urlTemplate;
		this.apiKeyLabel = apiKeyLabel;
		this.note = note;
		this.description = description;
		this.projectUrl = projectUrl;
	}

	@NonNull
	public static List<AisUrlSourceTemplate> all() {
		return Arrays.asList(
				new AisUrlSourceTemplate(
						"adsb.lol",
						AisUrlSource.Type.PLANES,
						"https://api.adsb.lol/v2/lat/50.03/lon/8.56/dist/50",
						null,
						"Free, no key - easiest way to try planes out",
						"Community feed in the ADS-B Exchange format. Edit lat/lon/dist in the URL "
								+ "below (dist is in nautical miles).",
						"https://adsb.lol"),
				new AisUrlSourceTemplate(
						"adsb.fi",
						AisUrlSource.Type.PLANES,
						"https://opendata.adsb.fi/api/v2/lat/50.03/lon/8.56/dist/50/",
						null,
						"Free, no key",
						"Community feed in the same format as ADS-B Exchange. Edit lat/lon/dist in "
								+ "the URL below (dist is in nautical miles).",
						"https://adsb.fi"),
				new AisUrlSourceTemplate(
						"ADS-B Exchange",
						AisUrlSource.Type.PLANES,
						"https://adsbexchange-com1.p.rapidapi.com/v2/lat/50.03/lon/8.56/dist/50/",
						"RapidAPI key",
						"Paid RapidAPI subscription - the keyless endpoint returns NO_AUTH",
						"The key is sent as a header. Edit lat/lon/dist in the URL below (dist is "
								+ "in nautical miles). Feeding your own receiver to ADS-B Exchange "
								+ "is what gets you free API access.",
						"https://www.adsbexchange.com/data/"),
				new AisUrlSourceTemplate(
						"airplanes.live",
						AisUrlSource.Type.PLANES,
						"https://api.airplanes.live/v2/point/50.03/8.56/50",
						null,
						"Needs approval - answers 403 until access is granted",
						"Same format as ADS-B Exchange, but the API asks projects to request access "
								+ "by email first. Edit lat/lon/radius in the URL below (radius is "
								+ "in nautical miles, max 250).",
						"https://airplanes.live/api-guide/"),
				new AisUrlSourceTemplate(
						"OpenSky Network - bounding box",
						AisUrlSource.Type.PLANES,
						"https://opensky-network.org/api/states/all?lamin=45&lomin=5&lamax=55&lomax=20",
						null,
						"Free, no key - about 400 requests a day anonymously",
						"Edit lamin/lomin/lamax/lomax in the URL below to your region. Registering "
								+ "a free account raises the daily allowance.",
						"https://openskynetwork.github.io/opensky-api/rest.html"),
				new AisUrlSourceTemplate(
						"OpenSky Network - whole world",
						AisUrlSource.Type.PLANES,
						"https://opensky-network.org/api/states/all",
						null,
						"Free, no key - costs more of the daily allowance per request",
						"A global query spends several times the allowance of a bounding-box one, "
								+ "so prefer the bounding-box template unless you really need it.",
						"https://openskynetwork.github.io/opensky-api/rest.html"),
				new AisUrlSourceTemplate(
						"Private aggregator",
						AisUrlSource.Type.PLANES,
						"https://example.com/api/planes?key={API_KEY}",
						"API key",
						"Your own server, key passed as a query parameter",
						"Edit the URL below (host, path, params) to match your server. The response "
								+ "must be in OpenSky or ADS-B Exchange JSON format.",
						null),
				new AisUrlSourceTemplate(
						"Custom",
						AisUrlSource.Type.PLANES,
						"",
						null,
						"Start blank",
						"Fill in everything yourself. The response must be in OpenSky or ADS-B "
								+ "Exchange JSON format.",
						null)
		);
	}
}
