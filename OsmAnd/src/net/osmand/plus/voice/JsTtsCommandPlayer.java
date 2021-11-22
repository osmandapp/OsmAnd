package net.osmand.plus.voice;

import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_JS;
import static net.osmand.IndexConstants.VOICE_PROVIDER_SUFFIX;

import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.AudioFocusHelperImpl;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandPreference;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JsTtsCommandPlayer extends CommandPlayer {

	private static final Log log = PlatformUtil.getLog(JsTtsCommandPlayer.class);

	private final static String PEBBLE_ALERT = "PEBBLE_ALERT";

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
	private boolean speechAllowed = false;

	// Only for debugging
	private static String ttsVoiceStatus = "-";
	private static String ttsVoiceUsed = "-";

	protected JsTtsCommandPlayer(OsmandApplication app,
	                             ApplicationMode applicationMode,
	                             VoiceRouter voiceRouter,
	                             File voiceProviderDir) throws CommandPlayerException {
		super(app, applicationMode, voiceRouter, voiceProviderDir);

		if (app.accessibilityEnabled()) {
			cSpeechRate = settings.SPEECH_RATE.get();
		}
		initializeEngine();
		params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, settings.AUDIO_MANAGER_STREAM
				.getModeValue(applicationMode).toString());
	}

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
			final float speechRate = cSpeechRate;

			final String[] lsplit = (language + "____.").split("[\\_\\-]");
			// As per BCP 47: well formed scripts: [a-zA-Z]{4}, variants: [0-9][0-9a-zA-Z]{3} | [0-9a-zA-Z]{5,8}, countries/regions: [a-zA-Z]{2} | [0-9]{3}
			String lregion = "";
			String lvariant = "";
			String lscript = "";
			for (int i = 3; i > 0; i--) {
				if (lsplit[i].length() == 4 && !(lsplit[i] + "A").substring(0, 1).matches("[0-9]")) {
					lscript = lsplit[i];
				} else if (lsplit[i].length() >= 4) {
					lvariant = lsplit[i];
				} else {
					lregion = lsplit[i];
				}
			}
			// Locale constructor supports 'language, region, variant'
			//Locale newLocale0 = new Locale(lsplit[0], lregion, lvariant); (Setting variant here seems to cause errors on some systems)
			Locale newLocale0 = new Locale(lsplit[0], lregion);
			// #3344: Try Locale builder instead (only available from API 21), also supports script (we support as 4 letters)
			try {
				newLocale0 = new Locale.Builder().setLanguage(lsplit[0]).setScript(lscript).setRegion(lregion).setVariant(lvariant).build();
			} catch (RuntimeException e) {
				// Falls back to constructor
			}
			final Locale newLocale = newLocale0;

			mTts = new TextToSpeech(app, new OnInitListener() {
				@Override
				public void onInit(int status) {
					if (status != TextToSpeech.SUCCESS) {
						ttsVoiceStatus = "NO INIT SUCCESS";
						internalClear();
						app.showToastMessage(app.getString(R.string.tts_initialization_error));
					} else if (mTts != null) {
						speechAllowed = true;
						switch (mTts.isLanguageAvailable(newLocale)) {
							case TextToSpeech.LANG_MISSING_DATA:
//								if (isSettingsActivity(act)) {
//									AlertDialog.Builder builder = createAlertDialog(
//										R.string.tts_missing_language_data_title,
//										R.string.tts_missing_language_data,
//										new IntentStarter(
//												act,
//												TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
//										act);
//									builder.show();
//								}
								ttsVoiceStatus = newLocale.getDisplayName() + ": LANG_MISSING_DATA";
								ttsVoiceUsed = getVoiceUsed();
								break;
							case TextToSpeech.LANG_AVAILABLE:
								ttsVoiceStatus = newLocale.getDisplayName() + ": LANG_AVAILABLE";
							case TextToSpeech.LANG_COUNTRY_AVAILABLE:
								ttsVoiceStatus = "-".equals(ttsVoiceStatus) ? newLocale.getDisplayName() + ": LANG_COUNTRY_AVAILABLE" : ttsVoiceStatus;
							case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
								try {
									mTts.setLanguage(newLocale);
								} catch (Exception e) {
									e.printStackTrace();
									if (mTts.isLanguageAvailable(Locale.getDefault()) > 0) {
										mTts.setLanguage(Locale.getDefault());
									} else {
										app.showToastMessage("TTS language not available");
									}
								}
								if (speechRate != 1) {
									mTts.setSpeechRate(speechRate);
								}
								ttsVoiceStatus = "-".equals(ttsVoiceStatus) ? newLocale.getDisplayName() + ": LANG_COUNTRY_VAR_AVAILABLE" : ttsVoiceStatus;
								ttsVoiceUsed = getVoiceUsed();
								break;
							case TextToSpeech.LANG_NOT_SUPPORTED:
								//maybe weird, but I didn't want to introduce parameter in around 5 methods just to do this if condition
//								if (isSettingsActivity(act)) {
//									AlertDialog.Builder builder = createAlertDialog(
//											R.string.tts_language_not_supported_title,
//											R.string.tts_language_not_supported,
//											new IntentStarter(
//													act,
//													Intent.ACTION_VIEW, Uri.parse("market://search?q=text to speech engine"
//														)),
//											act);
//									builder.show();
//								}
								ttsVoiceStatus = newLocale.getDisplayName() + ": LANG_NOT_SUPPORTED";
								ttsVoiceUsed = getVoiceUsed();
								break;
						}
					}
				}

				private String getVoiceUsed() {
					try {
						if (mTts.getVoice() != null) {
							return mTts.getVoice().toString() + " (API " + Build.VERSION.SDK_INT + ")";
						}
					} catch (RuntimeException e) {
					}
					return "-";
				}
			});
			mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
				// The call back is on a binder thread.
				@Override
				public synchronized void onUtteranceCompleted(String utteranceId) {
					if (--ttsRequests <= 0)
						abandonAudioFocus();
					log.debug("ttsRequests=" + ttsRequests);
					if (ttsRequests < 0) {
						ttsRequests = 0;
					}
				}
			});
		}
	}

	// Called from the calculating route thread.
	@Override
	public synchronized List<String> playCommands(CommandBuilder builder) {
		final List<String> execute = builder.execute(); //list of strings, the speech text, play it
		StringBuilder bld = new StringBuilder();
		for (String s : execute) {
			bld.append(s).append(' ');
		}
		sendAlertToPebble(bld.toString());
		if (mTts != null && !voiceRouter.isMute() && speechAllowed) {
			if (ttsRequests++ == 0) {
				requestAudioFocus();
				mTts.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(settings.AUDIO_USAGE.get())
						.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
						.build());
				// Delay first prompt of each batch to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
				if (app != null) {
					Integer streamModeValue = settings.AUDIO_MANAGER_STREAM.getModeValue(applicationMode);
					OsmandPreference<Integer> pref = settings.VOICE_PROMPT_DELAY[streamModeValue];
					int vpd = pref == null ? 0 : pref.getModeValue(applicationMode);
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

	private void sendAlertToPebble(String bld) {
		final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");
		final Map<String, Object> data = new HashMap<>();
		data.put("title", "Voice");
		data.put("body", bld);
		final JSONObject jsonData = new JSONObject(data);
		final String notificationData = new JSONArray().put(jsonData).toString();
		i.putExtra("messageType", PEBBLE_ALERT);
		i.putExtra("sender", "OsmAnd");
		i.putExtra("notificationData", notificationData);
		if (app != null) {
			app.sendBroadcast(i);
			log.info("Send message to pebble " + bld);
		}
	}

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

	public static String getTtsVoiceStatus() {
		return ttsVoiceStatus;
	}

	public static String getTtsVoiceUsed() {
		return ttsVoiceUsed;
	}

	public static boolean isMyData(File voiceDir) {
		if (!voiceDir.getName().contains("tts")) {
			return false;
		}
		String langName = voiceDir.getName().replace(VOICE_PROVIDER_SUFFIX, "");
		return new File(voiceDir, langName + "_" + TTSVOICE_INDEX_EXT_JS).exists();
	}
}