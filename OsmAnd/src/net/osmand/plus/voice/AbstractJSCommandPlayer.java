package net.osmand.plus.voice;

import android.content.Context;
import android.media.AudioManager;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.AudioFocusHelper;

import org.apache.commons.logging.Log;

import java.util.List;

import alice.tuprolog.InvalidLibraryException;
import alice.tuprolog.Prolog;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;

public abstract class AbstractJSCommandPlayer implements CommandPlayer, StateChangedListener<ApplicationMode> {
    private static final Log log = PlatformUtil.getLog(AbstractJSCommandPlayer.class);

    protected String language = "";
    protected OsmandApplication ctx;
    protected int streamType;
    private ApplicationMode applicationMode;

    public static boolean btScoStatus = false;
    private static AudioFocusHelper mAudioFocusHelper;
    public static String btScoInit = "";

    protected AbstractJSCommandPlayer(OsmandApplication ctx, ApplicationMode applicationMode,
                                      String voiceProvider) {
        this.ctx = ctx;
        this.applicationMode = applicationMode;
        long time = System.currentTimeMillis();
            this.ctx = ctx;

        if (log.isInfoEnabled()) {
            log.info("Initializing prolog system : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
        }
        this.streamType = ctx.getSettings().AUDIO_STREAM_GUIDANCE.getModeValue(applicationMode);
        language = voiceProvider.substring(0, voiceProvider.indexOf("-tts"));
    }

    @Override
    public String getCurrentVoice() {
        return null;
    }

    @Override
    public CommandBuilder newCommandBuilder() {
        JSCommandBuilder commandBuilder = new JSCommandBuilder(this);
        commandBuilder.setParameters("km-m", true);
        return commandBuilder;
    }

    @Override
    public void playCommands(CommandBuilder builder) {

    }

    @Override
    public void clear() {
        if(ctx != null && ctx.getSettings() != null) {
            ctx.getSettings().APPLICATION_MODE.removeListener(this);
        }
        abandonAudioFocus();
        ctx = null;
    }

    @Override
    public List<String> execute(List<Struct> listStruct) {
        return null;
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

    protected synchronized void abandonAudioFocus() {
        log.debug("abandonAudioFocus");
        if ((ctx != null && ctx.getSettings().AUDIO_STREAM_GUIDANCE.getModeValue(applicationMode) == 0) || (btScoStatus == true)) {
            toggleBtSco(false);
        }
        if (ctx != null && mAudioFocusHelper != null) {
            mAudioFocusHelper.abandonFocus(ctx, applicationMode, streamType);
        }
        mAudioFocusHelper = null;
    }

    private synchronized boolean toggleBtSco(boolean on) {
        // Hardy, 2016-07-03: Establish a low quality BT SCO (Synchronous Connection-Oriented) link to interrupt e.g. a car stereo FM radio
        if (on) {
            try {
                AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (mAudioManager == null || !mAudioManager.isBluetoothScoAvailableOffCall()) {
                    btScoInit = "Reported not available.";
                    return false;
                }
                mAudioManager.setMode(0);
                mAudioManager.startBluetoothSco();
                mAudioManager.setBluetoothScoOn(true);
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                btScoStatus = true;
            } catch (Exception e) {
                System.out.println("Exception starting BT SCO " + e.getMessage() );
                btScoStatus = false;
                btScoInit = "Available, but not initializad.\n(" + e.getMessage() + ")";
                return false;
            }
            btScoInit = "Available, initialized OK.";
            return true;
        } else {
            AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (mAudioManager == null) {
                return false;
            }
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            btScoStatus = false;
            return true;
        }
    }

    public ApplicationMode getApplicationMode() {
        return applicationMode;
    }

    @Override
    public void stateChanged(ApplicationMode change) {
//
//        if(prologSystem != null) {
//            try {
//                prologSystem.getTheoryManager().retract(new Struct("appMode", new Var()));
//            } catch (Exception e) {
//                log.error("Retract error: ", e);
//            }
//            prologSystem.getTheoryManager()
//                    .assertA(
//                            new Struct("appMode", new Struct(ctx.getSettings().APPLICATION_MODE.get().getStringKey()
//                                    .toLowerCase())), true, "", true);
//        }
    }
}
