package net.osmand.turnScreenOn.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import net.osmand.aidl.IOsmAndAidlCallback;
import net.osmand.aidl.IOsmAndAidlInterface;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.navigation.ADirectionInfo;
import net.osmand.aidl.navigation.ANavigationVoiceRouterMessageParams;
import net.osmand.aidl.search.SearchResult;
import net.osmand.turnScreenOn.PluginSettings;
import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.listener.Observable;
import net.osmand.turnScreenOn.listener.OnMessageListener;
import net.osmand.turnScreenOn.log.PlatformUtil;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class OsmAndAidlHelper implements Observable {
    private static final Log LOG = PlatformUtil.getLog(OsmAndAidlHelper.class);

    private final static String OSMAND_AIDL_SERVICE_PATH = "net.osmand.aidl.OsmandAidlService";

    private List<OnMessageListener> messageListeners;

    private TurnScreenApp app;
    private PluginSettings settings;
    private String connectedOsmandVersionPath;

    private long osmandUpdatesCallbackId = -1;

    private boolean attemptedRegister = false;
    private boolean isRegistered = false;

    private IOsmAndAidlInterface mIOsmAndAidlInterface;
    private IOsmAndAidlCallback mIOsmAndAidlCallbackInterface = new IOsmAndAidlCallback.Stub() {

        @Override
        public void onSearchComplete(List<SearchResult> resultSet) throws RemoteException {

        }

        @Override
        public void onUpdate() throws RemoteException {

        }

        @Override
        public void onAppInitialized() throws RemoteException {

        }

        @Override
        public void onGpxBitmapCreated(AGpxBitmap bitmap) throws RemoteException {

        }

        @Override
        public void updateNavigationInfo(ADirectionInfo directionInfo) throws RemoteException {

        }

        @Override
        public void onContextMenuButtonClicked(int buttonId, String pointId, String layerId) throws RemoteException {

        }

        @Override
        public void onVoiceRouterNotify() throws RemoteException {
            notifyListeners();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service);
            if (mIOsmAndAidlInterface != null && attemptedRegister) {
                attemptedRegister = false;
                registerForVoiceRouterMessages();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIOsmAndAidlInterface = null;
            isRegistered = false;
        }
    };

    public OsmAndAidlHelper(TurnScreenApp app) {
        this.app = app;
        settings = app.getSettings();
        messageListeners = new ArrayList<>();
    }

    public void registerForVoiceRouterMessages() {
        try {
            if (mIOsmAndAidlInterface != null && !isRegistered) {
                ANavigationVoiceRouterMessageParams params = new ANavigationVoiceRouterMessageParams();
                params.setSubscribeToUpdates(true);
                params.setCallbackId(osmandUpdatesCallbackId);
                osmandUpdatesCallbackId = mIOsmAndAidlInterface.registerForVoiceRouterMessages(params, mIOsmAndAidlCallbackInterface);
                isRegistered = true;
            } else {
                attemptedRegister = true;
            }
        } catch (RemoteException e) {
        }
    }

    public void unregisterFromVoiceRouterMessages() {
        try {
            if (mIOsmAndAidlInterface != null && isRegistered) {
                ANavigationVoiceRouterMessageParams params = new ANavigationVoiceRouterMessageParams();
                params.setSubscribeToUpdates(false);
                params.setCallbackId(osmandUpdatesCallbackId);
                mIOsmAndAidlInterface.registerForVoiceRouterMessages(params, mIOsmAndAidlCallbackInterface);
                isRegistered = false;
            }
        } catch (RemoteException e) {
        }
    }

    public void reconnectOsmand() {
        PluginSettings.OsmandVersion versionToConnect = settings.getOsmandVersion();
        if (versionToConnect != null) {
            String newOsmandVersionPath = versionToConnect.getPath();
            if (connectedOsmandVersionPath == null
                    || !connectedOsmandVersionPath.equals(newOsmandVersionPath)
                    || mIOsmAndAidlInterface == null) {
                cleanupResources();
                connectOsmand();
            }
            connectedOsmandVersionPath = newOsmandVersionPath;
        }
    }

    public void connectOsmand() {
        bindService(settings.getOsmandVersion().getPath());
    }

    private void bindService(String packageName) {
        if (mIOsmAndAidlInterface == null) {
            Intent intent = new Intent(OSMAND_AIDL_SERVICE_PATH);
            intent.setPackage(packageName);
            app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void cleanupResources() {
        try {
            if (mIOsmAndAidlInterface != null) {
                mIOsmAndAidlInterface = null;
                unregisterFromVoiceRouterMessages();
                isRegistered = false;
                app.unbindService(mConnection);
            }
        } catch (Throwable e) {
        }
    }

    @Override
    public void addListener(OnMessageListener listener) {
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    @Override
    public void removeListener(OnMessageListener listener) {
        if (listener != null) {
            messageListeners.remove(listener);
        }
    }

    @Override
    public void notifyListeners() {
        if (messageListeners != null) {
            for (OnMessageListener listener : messageListeners) {
                listener.onMessageReceive();
            }
        }
    }
}
