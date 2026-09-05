package net.osmand.plus.voice;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Locale;

import androidx.annotation.NonNull;

public class LocaleBuilder {

	private static final Log log = PlatformUtil.getLog(LocaleBuilder.class);

	private final TextToSpeech tts;
	private final Locale defaultDeviceLocale;
	private final String localeParams;

	public LocaleBuilder(@NonNull OsmandApplication app, @NonNull TextToSpeech tts, @NonNull String localeParams) {
		this.tts = tts;
		this.defaultDeviceLocale = app.getLocaleHelper().getDefaultLocale();
		this.localeParams = localeParams;
	}

	@NonNull
	public Locale buildLocale() {
		String[] params = (localeParams + "____.").split("[\\_\\-]");
		String language = params[0];
		                        // As per BCP 47:
		String region = "";     // [a-zA-Z]{2} | [0-9]{3}
		String variant = "";    // [0-9][0-9a-zA-Z]{3} | [0-9a-zA-Z]{5,8}
		String script = "";     // [a-zA-Z]{4}
		for (int i = 3; i > 0; i--) {
			String param = params[i];
			if (param.matches("[a-zA-Z]{4}")) {
				script = param;
			} else if (param.length() >= 4) {
				variant = param;
			} else {
				region = param;
			}
		}

		return buildLocaleInternal(language, getSuitableRegion(region), variant, script);
	}

	@NonNull
	private String getSuitableRegion(@NonNull String regionFromFile) {
		if (!Algorithms.isEmpty(regionFromFile)) {
			return regionFromFile;
		}

		Voice defaultVoice = tts.getDefaultVoice();
		if (defaultVoice == null) {
			return defaultDeviceLocale.getCountry();
		}

		Locale defaultEngineLocale = defaultVoice.getLocale();
		if (defaultEngineLocale == null) {
			return defaultDeviceLocale.getCountry();
		}

		return defaultDeviceLocale.getLanguage().equals(defaultEngineLocale.getLanguage())
				? defaultEngineLocale.getCountry()
				: "";
	}

	@NonNull
	private Locale buildLocaleInternal(@NonNull String language, @NonNull String region,
	                                   @NonNull String variant, @NonNull String script) {
		try {
			return new Locale.Builder()
					.setLanguage(language)
					.setRegion(region)
					.setVariant(variant)
					.setScript(script)
					.build();
		} catch (RuntimeException e) {
			log.error("Trying to build locale with ill-formed param", e);
			return new Locale(language, region); // Variant not passed to prevent errors on some devices
		}
	}
}