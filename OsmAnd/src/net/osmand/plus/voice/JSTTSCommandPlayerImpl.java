package net.osmand.plus.voice;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.util.Algorithms;

import org.mozilla.javascript.ScriptableObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class JSTTSCommandPlayerImpl extends AbstractJSCommandPlayer {
    private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(JSTTSCommandPlayerImpl.class);

    private static final String TAG = JSTTSCommandPlayerImpl.class.getSimpleName();
    private static TextToSpeech mTts;
    private static String ttsVoiceStatus = "";
    private static String ttsVoiceUsed = "";

    private boolean speechAllowed = false;
    private Context mTtsContext;

    private ScriptableObject jsScope;

    private float cSpeechRate = 1;

    private static final class IntentStarter implements
            DialogInterface.OnClickListener {
        private final Context ctx;
        private final String intentAction;
        private final Uri intentData;

        private IntentStarter(Context ctx, String intentAction) {
            this(ctx,intentAction, null);
        }

        private IntentStarter(Context ctx, String intentAction, Uri intentData) {
            this.ctx = ctx;
            this.intentAction = intentAction;
            this.intentData = intentData;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent installIntent = new Intent();
            installIntent.setAction(intentAction);
            if (intentData != null) {
                installIntent.setData(intentData);
            }
            ctx.startActivity(installIntent);
        }
    }
    private OsmandApplication app;
    private ApplicationMode appMode;
    private  VoiceRouter vrt;

    private HashMap<String, String> params = new HashMap<String, String>();

    private static int ttsRequests = 0;

    public JSTTSCommandPlayerImpl(Activity ctx, ApplicationMode applicationMode, VoiceRouter vrt, String voiceProvider) throws CommandPlayerException {
        super((OsmandApplication) ctx.getApplication(), applicationMode, voiceProvider);
        this.app = (OsmandApplication) ctx.getApplicationContext();
        this.appMode = applicationMode;
        this.vrt = vrt;
        if (Algorithms.isEmpty(language)) {
            throw new CommandPlayerException(
                    ctx.getString(R.string.voice_data_corrupted));
        }
        OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
        if(app.accessibilityEnabled()) {
            cSpeechRate = app.getSettings().SPEECH_RATE.get();
        }
        initializeEngine(app, ctx);
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, app.getSettings().AUDIO_STREAM_GUIDANCE
                .getModeValue(getApplicationMode()).toString());
        org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
        context.setOptimizationLevel(-1);
        jsScope = context.initSafeStandardObjects();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(
                    app.getAppPath(IndexConstants.VOICE_INDEX_DIR).getAbsolutePath() +
                            "/" + voiceProvider + "/" + language + "_tts.js")));
            context.evaluateReader(jsScope, br, "JS", 1, null);
            br.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    private void initializeEngine(final Context ctx, final Activity act) {
        if (mTtsContext != ctx) {
            internalClear();
        }
        if (mTts == null) {
            mTtsContext = ctx;
            ttsVoiceStatus = "";
            ttsVoiceUsed = "";
            ttsRequests = 0;
            final float speechRate = cSpeechRate;

            final String[] lsplit = (language + "____.").split("[_\\-]");
            // constructor supports lang_country_variant
            Locale newLocale0 = new Locale(lsplit[0], lsplit[1], lsplit[2]);
            // #3344: Try Locale builder instead of constructor (only available from API 21). Also supports script (for now supported as trailing x_x_x_Scrp)
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    newLocale0 = new Locale.Builder().setLanguage(lsplit[0]).setScript(lsplit[3]).setRegion(lsplit[1]).setVariant(lsplit[2]).build();
                } catch (RuntimeException e) {
                    // Falls back to constructor
                }
            }
            final Locale newLocale = newLocale0;

            mTts = new TextToSpeech(ctx, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        ttsVoiceStatus = "NO INIT SUCCESS";
                        internalClear();
                    } else if (mTts != null) {
                        speechAllowed = true;
                        switch (mTts.isLanguageAvailable(newLocale)) {
                            case TextToSpeech.LANG_MISSING_DATA:
                                if (isSettingsActivity(act)) {
                                    AlertDialog.Builder builder = createAlertDialog(
                                            R.string.tts_missing_language_data_title,
                                            R.string.tts_missing_language_data,
                                            new JSTTSCommandPlayerImpl.IntentStarter(
                                                    act,
                                                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                                            act);
                                    builder.show();
                                }
                                ttsVoiceStatus = newLocale.getDisplayName() + ": LANG_MISSING_DATA";
                                ttsVoiceUsed = getVoiceUsed();
                                break;
                            case TextToSpeech.LANG_AVAILABLE:
                                ttsVoiceStatus = newLocale.getDisplayName() + ": LANG_AVAILABLE";
                            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                                ttsVoiceStatus = "".equals(ttsVoiceStatus) ? newLocale.getDisplayName() + ": LANG_COUNTRY_AVAILABLE" : ttsVoiceStatus;
                            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                                try {
                                    mTts.setLanguage(newLocale);
                                } catch(Exception e) {
                                    e.printStackTrace();
                                    mTts.setLanguage(Locale.getDefault());
                                }
                                if(speechRate != 1) {
                                    mTts.setSpeechRate(speechRate);
                                }
                                ttsVoiceStatus = "".equals(ttsVoiceStatus) ? newLocale.getDisplayName() + ": LANG_COUNTRY_VAR_AVAILABLE" : ttsVoiceStatus;
                                ttsVoiceUsed = getVoiceUsed();
                                break;
                            case TextToSpeech.LANG_NOT_SUPPORTED:
                                //maybe weird, but I didn't want to introduce parameter in around 5 methods just to do this if condition
                                if (isSettingsActivity(act)) {
                                    AlertDialog.Builder builder = createAlertDialog(
                                            R.string.tts_language_not_supported_title,
                                            R.string.tts_language_not_supported,
                                            new JSTTSCommandPlayerImpl.IntentStarter(
                                                    act,
                                                    Intent.ACTION_VIEW, Uri.parse("market://search?q=text to speech engine"
                                            )),
                                            act);
                                    builder.show();
                                }
                                ttsVoiceStatus = newLocale.getDisplayName() + ": LANG_NOT_SUPPORTED";
                                ttsVoiceUsed = getVoiceUsed();
                                break;
                        }
                    }
                }

                private boolean isSettingsActivity(final Context ctx) {
                    return ctx instanceof SettingsActivity;
                }

                private String getVoiceUsed() {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= 21) {
                            if (mTts.getVoice() != null) {
                                return mTts.getVoice().toString() + " (API " + android.os.Build.VERSION.SDK_INT + ")";
                            }
                        } else {
                            return mTts.getLanguage() + " (API " + android.os.Build.VERSION.SDK_INT + " only reports language)";
                        }
                    } catch (RuntimeException e) {
                        // mTts.getVoice() might throw NPE
                    }
                    return "-";
                }
            });
            mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                // The call back is on a binder thread.
                @Override
                public synchronized void onUtteranceCompleted(String utteranceId) {
                    if (--ttsRequests <= 0)
                        abandonAudioFocus();
                    log.debug("ttsRequests="+ttsRequests);
                    if (ttsRequests < 0) {
                        ttsRequests = 0;
                    }
                }
            });
        }
    }

    @Override
    public String getCurrentVoice() {
        return null;
    }

    @Override
    public JSCommandBuilder newCommandBuilder() {
        JSCommandBuilder commandBuilder = new JSCommandBuilder(this);
        commandBuilder.setJSContext(jsScope);
        commandBuilder.setParameters(app.getSettings().METRIC_SYSTEM.get().toTTSString(), true);
        return commandBuilder;
    }

    @Override
    public void playCommands(CommandBuilder builder) {
        final List<String> execute = builder.execute(); //list of strings, the speech text, play it
        StringBuilder bld = new StringBuilder();
        for (String s : execute) {
            bld.append(s).append(' ');
        }
        if (mTts != null && !vrt.isMute() && speechAllowed) {
            if (ttsRequests++ == 0) {
                // Delay first prompt of each batch to allow BT SCO connection being established
                if (app.getSettings().AUDIO_STREAM_GUIDANCE.getModeValue(appMode) == 0) {
                    ttsRequests++;
                    if (android.os.Build.VERSION.SDK_INT < 21) {
                        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,""+System.currentTimeMillis());
                        mTts.playSilence(app.getSettings().BT_SCO_DELAY.get(), TextToSpeech.QUEUE_ADD, params);
                    } else {
                        mTts.playSilentUtterance(app.getSettings().BT_SCO_DELAY.get(), TextToSpeech.QUEUE_ADD, ""+System.currentTimeMillis());
                    }
                }
            }
            Log.d(TAG, "ttsRequests= "+ttsRequests);
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,""+System.currentTimeMillis());
            mTts.speak(bld.toString(), TextToSpeech.QUEUE_ADD, params);
            // Audio focus will be released when onUtteranceCompleted() completed is called by the TTS engine.
        } else if (app != null && vrt.isMute()) {
            // sendAlertToAndroidWear(ctx, bld.toString());
        }
    }

    @Override
    public void stop(){
        ttsRequests = 0;
        if (mTts != null){
            mTts.stop();
        }
        abandonAudioFocus();
    }

    public static String getTtsVoiceStatus() {
        return ttsVoiceStatus;
    }

    public static String getTtsVoiceUsed() {
        return ttsVoiceUsed;
    }

    @Override
    public void clear() {
        super.clear();
        internalClear();
    }

    @Override
    public void updateAudioStream(int streamType) {
        super.updateAudioStream(streamType);
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType+"");
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public boolean supportsStructuredStreetNames() {
        return true;
    }


    private void internalClear() {
        ttsRequests = 0;
        speechAllowed = false;
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
        abandonAudioFocus();
        mTtsContext = null;
        ttsVoiceStatus = "";
        ttsVoiceUsed = "";
    }

    public static boolean isMyData(File voiceDir) {
        if (!voiceDir.getName().contains("tts")) {
            return false;
        }
        for (File f : voiceDir.listFiles()) {
            if (f.getName().endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS)) {
                return true;
            }
        }
        return false;
    }

    private AlertDialog.Builder createAlertDialog(int titleResID, int messageResID,
                                                  JSTTSCommandPlayerImpl.IntentStarter intentStarter, final Activity ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.shared_string_no, null);
        builder.setPositiveButton(R.string.shared_string_yes, intentStarter);
        builder.setTitle(titleResID);
        builder.setMessage(messageResID);
        return builder;
    }
}
