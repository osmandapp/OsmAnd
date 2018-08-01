package net.osmand.plus.voice;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import net.osmand.IndexConstants;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.VoiceRouter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class JSTTSCommandPlayerImpl extends AbstractJSCommandPlayer {

    private static final String TAG = JSTTSCommandPlayerImpl.class.getSimpleName();
    private static TextToSpeech mTts;

    private OsmandApplication app;
    private ApplicationMode appMode;
    private  VoiceRouter vrt;
    private String voiceProvider;

    private HashMap<String, String> params = new HashMap<String, String>();

    private static int ttsRequests = 0;

    public JSTTSCommandPlayerImpl(OsmandApplication ctx, ApplicationMode applicationMode, VoiceRouter vrt, String voiceProvider) {
        this.app = ctx;
        this.appMode = applicationMode;
        this.vrt = vrt;
        this.voiceProvider = voiceProvider;
        mTts = new TextToSpeech(ctx, null);
    }

    @Override
    public String getCurrentVoice() {
        return null;
    }

    @Override
    public JSCommandBuilder newCommandBuilder() {
        JSCommandBuilder commandBuilder = new JSCommandBuilder(this);
        commandBuilder.setJSContext(app.getAppPath(IndexConstants.VOICE_INDEX_DIR).getAbsolutePath() + "/" + voiceProvider + "/en_tts.js");
        commandBuilder.setParameters(app.getSettings().METRIC_SYSTEM.get().toHumanString(app), true);
        return commandBuilder;
    }

    @Override
    public void playCommands(CommandBuilder builder) {
        final List<String> execute = builder.execute(); //list of strings, the speech text, play it
        StringBuilder bld = new StringBuilder();
        for (String s : execute) {
            bld.append(s).append(' ');
        }
        if (mTts != null && !vrt.isMute()) {
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
    public void clear() {

    }

    @Override
    public void updateAudioStream(int streamType) {

    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public boolean supportsStructuredStreetNames() {
        return true;
    }

    @Override
    public void stop() {

    }
}
