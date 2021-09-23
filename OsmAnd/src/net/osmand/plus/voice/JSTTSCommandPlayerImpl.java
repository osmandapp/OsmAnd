package net.osmand.plus.voice;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.mozilla.javascript.ScriptableObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class JSTTSCommandPlayerImpl extends TTSCommandPlayerImpl {
    private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(JSTTSCommandPlayerImpl.class);

    private ScriptableObject jsScope;

    public JSTTSCommandPlayerImpl(OsmandApplication app, ApplicationMode applicationMode, VoiceRouter vrt,
                                  String voiceProvider) throws CommandPlayerException {
        super(app, applicationMode, vrt, voiceProvider);
        org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
        context.setOptimizationLevel(-1);
        jsScope = context.initSafeStandardObjects();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(
                    app.getAppPath(IndexConstants.VOICE_INDEX_DIR).getAbsolutePath() + "/" + voiceProvider + "/"
                            + voiceProvider.replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "_tts") + ".js")));
            context.evaluateReader(jsScope, br, "JS", 1, null);
            br.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    @Override
    public JsCommandBuilder newCommandBuilder() {
        JsCommandBuilder commandBuilder = new JsCommandBuilder(this);
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
        String langName = voiceDir.getName().replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "");
        return new File(voiceDir, langName + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS).exists();
    }
}
