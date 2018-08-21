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

public class JSTTSCommandPlayerImpl extends TTSCommandPlayerImpl {
    private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(JSTTSCommandPlayerImpl.class);

    private ScriptableObject jsScope;
    private OsmandApplication app;

    public JSTTSCommandPlayerImpl(Activity ctx, ApplicationMode applicationMode, VoiceRouter vrt, String voiceProvider) throws CommandPlayerException {
        super(ctx, applicationMode, vrt, voiceProvider);
        this.app = (OsmandApplication) ctx.getApplication();
        org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
        context.setOptimizationLevel(-1);
        jsScope = context.initSafeStandardObjects();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(
                    app.getAppPath(IndexConstants.VOICE_INDEX_DIR).getAbsolutePath() +
                            "/" + voiceProvider + "/" + voiceProvider.replace("-tts", "_tts") + ".js")));
            context.evaluateReader(jsScope, br, "JS", 1, null);
            br.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    @Override
    public JSCommandBuilder newCommandBuilder() {
        JSCommandBuilder commandBuilder = new JSCommandBuilder(this);
        commandBuilder.setJSContext(jsScope);
        commandBuilder.setParameters(app.getSettings().METRIC_SYSTEM.get().toTTSString(), true);
        return commandBuilder;
    }

    @Override
    public boolean supportsStructuredStreetNames() {
        return true;
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
}
