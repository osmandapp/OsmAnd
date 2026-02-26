package net.osmand.plus.voice;

import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.AudioFocusHelperImpl;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;

import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_JS;
import static net.osmand.IndexConstants.VOICE_PROVIDER_SUFFIX;

public class JsTtsCommandPlayer extends CommandPlayer {

	private static final Log log = PlatformUtil.getLog(JsTtsCommandPlayer.class);

	private static final String PEBBLE_ALERT = "PEBBLE_ALERT";

	private static TextToSpeech mTts;

	private final HashMap<String, String> params = new HashMap<>();

	/**
	 * Since TTS requests are asynchronous, playCommands() can be called before
	 * the TTS engine is done. We use this field to keep track of concurrent tts
	 * activity. Where tts activity is defined as the time between tts.speak()
	 * and the call back to onUtteranceCompletedListener().  This allows us to
	 * optimize use of requesting and abandoning audio focus.
	 */
	private static int ttsRequests;
	private float cSpeechRate = 1;
	private boolean speechAllowed;

	// Only for debugging
	private static String ttsVoiceStatus = "-";
	private static String ttsVoiceUsed = "-";

	protected JsTtsCommandPlayer(@NonNull OsmandApplication app,
	                             @NonNull ApplicationMode applicationMode,
	                             @NonNull VoiceRouter voiceRouter,
	                             @NonNull File voiceProviderDir) throws CommandPlayerException {
		super(app, applicationMode, voiceRouter, voiceProviderDir);

		if (app.accessibilityEnabled()) {
			cSpeechRate = settings.SPEECH_RATE.get();
		}
		initializeEngine();
		params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, settings.AUDIO_MANAGER_STREAM
				.getModeValue(app.getRoutingHelper().getAppMode()).toString());
	}

	@NonNull
	@Override
	public File getTtsFileFromDir(@NonNull File voiceProviderDir) {
		String fileName = voiceProviderDir.getName().replace(VOICE_PROVIDER_SUFFIX, "_" + TTSVOICE_INDEX_EXT_JS);
		return new File(voiceProviderDir, fileName);
	}

	private void initializeEngine() {
		internalClear();

		if (mTts == null) {
			ttsVoiceStatus = "-";
			ttsVoiceUsed = "-";
			ttsRequests = 0;

			mTts = new TextToSpeech(app, status -> {
 				if (status != TextToSpeech.SUCCESS) {
					ttsVoiceStatus = "NO INIT SUCCESS";
					internalClear();
					app.showToastMessage(app.getString(R.string.tts_initialization_error));
				} else if (mTts != null) {
					Locale locale = new LocaleBuilder(app, mTts, language).buildLocale();
					onSuccessfulTtsInit(locale, cSpeechRate);
				}
			});
			mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
				// The call back is on a binder thread.
				@Override
				public synchronized void onUtteranceCompleted(String utteranceId) {
					if (--ttsRequests <= 0) {
						abandonAudioFocus();
					}
					log.debug("ttsRequests=" + ttsRequests);
					if (ttsRequests < 0) {
						ttsRequests = 0;
					}
				}
			});
		}
	}

	private void onSuccessfulTtsInit(@NonNull Locale locale, float speechRate) {
		speechAllowed = true;
		switch (mTts.isLanguageAvailable(locale)) {
			case TextToSpeech.LANG_NOT_SUPPORTED:
				ttsVoiceStatus = locale.getDisplayName() + ": LANG_NOT_SUPPORTED";
				ttsVoiceUsed = getVoiceUsed();
				break;
			case TextToSpeech.LANG_MISSING_DATA:
				ttsVoiceStatus = locale.getDisplayName() + ": LANG_MISSING_DATA";
				ttsVoiceUsed = getVoiceUsed();
				break;
			case TextToSpeech.LANG_AVAILABLE:
				ttsVoiceStatus = locale.getDisplayName() + ": LANG_AVAILABLE";
			case TextToSpeech.LANG_COUNTRY_AVAILABLE:
				ttsVoiceStatus = "-".equals(ttsVoiceStatus)
						? locale.getDisplayName() + ": LANG_COUNTRY_AVAILABLE"
						: ttsVoiceStatus;
			case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
				try {
					mTts.setLanguage(locale);
				} catch (Exception e) {
					log.error(e);
					if (mTts.isLanguageAvailable(Locale.getDefault()) > 0) {
						mTts.setLanguage(Locale.getDefault());
					} else {
						app.showToastMessage("TTS language not available");
					}
				}
				if (speechRate != 1) {
					mTts.setSpeechRate(speechRate);
				}
				ttsVoiceStatus = "-".equals(ttsVoiceStatus)
						? locale.getDisplayName() + ": LANG_COUNTRY_VAR_AVAILABLE"
						: ttsVoiceStatus;
				ttsVoiceUsed = getVoiceUsed();
				break;
		}
	}

	@NonNull
	private String getVoiceUsed() {
		try {
			if (mTts.getVoice() != null) {
				return mTts.getVoice().toString() + " (API " + Build.VERSION.SDK_INT + ")";
			}
		} catch (Exception e) {
			log.error(e);
		}
		return "-";
	}

	// Called from the calculating route thread.
	@NonNull
	@Override
	public synchronized List<String> playCommands(@NonNull CommandBuilder builder) {
		List<String> execute = builder.execute(); //list of strings, the speech text, play it
		StringBuilder bld = new StringBuilder();
		for (String s : execute) {
			bld.append(s).append(' ');
		}
		sendAlertToPebble(bld.toString());
		if (mTts != null && !voiceRouter.isMute() && speechAllowed) {
			if (ttsRequests++ == 0) {
				requestAudioFocus();
				mTts.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(settings.AUDIO_USAGE[settings.AUDIO_MANAGER_STREAM.getModeValue(app.getRoutingHelper().getAppMode())].get())
						.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
						.build());
				// Delay first prompt of each batch to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
				if (app != null) {
					Integer streamModeValue = settings.AUDIO_MANAGER_STREAM.getModeValue(app.getRoutingHelper().getAppMode());
					OsmandPreference<Integer> pref = settings.VOICE_PROMPT_DELAY[streamModeValue];
					int vpd = pref == null ? 0 : pref.getModeValue(app.getRoutingHelper().getAppMode());
					if (vpd > 0) {
						ttsRequests++;
						mTts.playSilentUtterance(vpd, TextToSpeech.QUEUE_ADD, "" + System.currentTimeMillis());
					}
				}
			}
			log.debug("ttsRequests=" + ttsRequests);
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "" + System.currentTimeMillis());
			if (AudioFocusHelperImpl.playbackAuthorized) {
				mTts.speak(bld.toString(), TextToSpeech.QUEUE_ADD, params);
			} else {
				stop();
			}
			// Audio focus will be released when onUtteranceCompleted() completed is called by the TTS engine.
		}
		// #5966: TTS Utterance for debugging
		if (app != null && settings.DISPLAY_TTS_UTTERANCE.get()) {
			app.showToastMessage(bld.toString());
		}
		return execute;
	}

	private void sendAlertToPebble(@NonNull String bld) {
		Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");
		Map<String, Object> data = new HashMap<>();
		data.put("title", "Voice");
		data.put("body", bld);
		JSONObject jsonData = new JSONObject(data);
		String notificationData = new JSONArray().put(jsonData).toString();
		i.putExtra("messageType", PEBBLE_ALERT);
		i.putExtra("sender", "OsmAnd");
		i.putExtra("notificationData", notificationData);
		if (app != null) {
			app.sendBroadcast(i);
			log.info("Send message to pebble " + bld);
		}
	}

	@NonNull
	@Override
	public CommandBuilder newCommandBuilder() {
		JsCommandBuilder commandBuilder = new JsCommandBuilder(this);
		commandBuilder.setJSContext(jsScope);
		commandBuilder.setParameters(settings.METRIC_SYSTEM.get().toTTSString(), true);
		return commandBuilder;
	}

	@Override
	public void updateAudioStream(int streamType) {
		super.updateAudioStream(streamType);
		params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType + "");
	}

	@Override
	public void stop() {
		ttsRequests = 0;
		if (mTts != null) {
			mTts.stop();
		}
		abandonAudioFocus();
	}

	@Override
	public void clear() {
		super.clear();
		internalClear();
	}

	private void internalClear() {
		ttsRequests = 0;
		speechAllowed = false;
		if (mTts != null) {
			mTts.shutdown();
			mTts = null;
		}
		abandonAudioFocus();
		ttsVoiceStatus = "-";
		ttsVoiceUsed = "-";
	}

	@Override
	public boolean supportsStructuredStreetNames() {
		return true;
	}

	@NonNull
	public static String getTtsVoiceStatus() {
		return ttsVoiceStatus;
	}

	@NonNull
	public static String getTtsVoiceUsed() {
		return ttsVoiceUsed;
	}

	public static boolean isMyData(@NonNull File voiceDir) {
		if (!voiceDir.getName().contains("tts")) {
			return false;
		}
		String langName = voiceDir.getName().replace(VOICE_PROVIDER_SUFFIX, "");
		return new File(voiceDir, langName + "_" + TTSVOICE_INDEX_EXT_JS).exists();
	}
}
