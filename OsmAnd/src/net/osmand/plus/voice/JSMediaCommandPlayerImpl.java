package net.osmand.plus.voice;


import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.VoiceRouter;

import org.mozilla.javascript.ScriptableObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JSMediaCommandPlayerImpl extends MediaCommandPlayerImpl {

    private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(JSMediaCommandPlayerImpl.class);

    private ScriptableObject jsScope;
    private OsmandApplication app;

    public JSMediaCommandPlayerImpl(OsmandApplication ctx, ApplicationMode applicationMode, VoiceRouter vrt, String voiceProvider) throws CommandPlayerException {
        super(ctx, applicationMode, vrt, voiceProvider);
        app = ctx;
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

    @Override
    public synchronized List<String> playCommands(CommandBuilder builder) {
        if(vrt.isMute()) {
            return Collections.emptyList();
        }
        List<String> lst = splitAnnouncements(builder.execute());
        filesToPlay.addAll(lst);

        // If we have not already started to play audio, start.
        if (mediaPlayer == null) {
            requestAudioFocus();
            // Delay first prompt of each batch to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
            if (ctx != null) {
                int vpd = ctx.getSettings().VOICE_PROMPT_DELAY[ctx.getSettings().AUDIO_MANAGER_STREAM.getModeValue(getApplicationMode())].get();
                if (vpd > 0) {
                    try {
                        Thread.sleep(vpd);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        playQueue();
        return lst;
    }

    private List<String> splitAnnouncements(List<String> execute) {
        List<String> result = new ArrayList<>();
        for (String files : execute) {
            result.addAll(Arrays.asList(files.split(" ")));
        }
        return result;
    }

    @Override
    public JSCommandBuilder newCommandBuilder() {
        JSCommandBuilder commandBuilder = new JSCommandBuilder(this);
        commandBuilder.setJSContext(jsScope);
        commandBuilder.setParameters(app.getSettings().METRIC_SYSTEM.get().toTTSString(), false);
        return commandBuilder;
    }

    public static boolean isMyData(File voiceDir) {
        if (voiceDir.getName().contains("tts")) {
            return false;
        }
        File[] files = voiceDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS)) {
                    return true;
                }
            }
        }
        return false;
    }

}
