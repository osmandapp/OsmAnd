package net.osmand.turnScreenOn.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import net.osmand.aidl.IOsmAndAidlCallback;
import net.osmand.aidl.IOsmAndAidlInterface;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.navigation.ADirectionInfo;
import net.osmand.aidl.navigation.ANavigationVoiceRouterMessageParams;
import net.osmand.aidl.search.SearchResult;
import net.osmand.turnScreenOn.PluginSettings;
import net.osmand.turnScreenOn.app.TurnScreenApp;

import java.util.List;

public class OsmAndAidlHelper {
    private final static String AIDL_SERVICE_PATH = "net.osmand.aidl.OsmandAidlService";

    private LockHelper lockHelper;
    private PluginSettings settings;

    private TurnScreenApp app;

    private int osmandUpdatesCallbackId;
    private boolean bound;
    private boolean initialized;

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
            Log.d("ttpl", "take message from vr");
            if (settings.isPluginEnabled()) {
                lockHelper.timedUnlock(app.getSettings().getTimeLikeSeconds() * 1000L);
            }
        }
    };

    private boolean isConnected = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service);
            if (mIOsmAndAidlInterface != null) {
                isConnected = true;
                try {
                    Log.d("ttpl", "onServiceConnected: connected to " + service.getInterfaceDescriptor());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("ttpl", "no connection set up");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIOsmAndAidlInterface = null;
            isConnected = false;
            Log.d("ttpl", "onServiceDisconnected: disconnect");
        }
    };

    public OsmAndAidlHelper(TurnScreenApp app) {
        this.app = app;
        settings = app.getSettings();
        lockHelper = app.getLockHelper();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void register() {
        try {
            if (mIOsmAndAidlInterface != null) {
                Log.d("ttpl", "register for voice router messages");
                ANavigationVoiceRouterMessageParams params = new ANavigationVoiceRouterMessageParams();
                mIOsmAndAidlInterface.registerForVoiceRouterMessages(params, mIOsmAndAidlCallbackInterface);
            } else {
                Log.d("ttpl", "not registered for messages");
            }
        } catch (RemoteException e) {
            Log.d("ttpl", "some exception");
            e.printStackTrace();
        }
    }

    /*private boolean bindService(String appToConnectPackage, Context context) {
        Intent intent = new Intent(AIDL_SERVICE_PATH);
        intent.setPackage(appToConnectPackage);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    public boolean connect(String appToConnectPackage) {
        bindService(appToConnectPackage, TurnScreenOnApplication.getAppContext());
        Log.d("ttpl", "connecting...");
        return true;
    }*/

    public void reconnectOsmand() {
        cleanupResources();
        connectOsmand();
    }

    public void connectOsmand() {
        if (bindService(settings.getOsmandVersion().getPath())) {
            Log.d("ttpl", "connecting... to Osmand");
            bound = true;
        } else {
            bound = false;
            initialized = true;
        }
    }

    private boolean bindService(String packageName) {
        if (mIOsmAndAidlInterface == null) {
            Intent intent = new Intent("net.osmand.aidl.OsmandAidlService");
            intent.setPackage(packageName);
            app.getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } else {

        }
        return true;
    }

    private void cleanupResources() {
        try {
            if (mIOsmAndAidlInterface != null) {
//                unregisterFromUpdates();
                mIOsmAndAidlInterface = null;
                Log.d("ttpl", "disconnecting from Osmand");
                app.getContext().unbindService(mConnection);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean unregisterFromUpdates() {
        if (mIOsmAndAidlInterface != null) {
            try {
                boolean unregistered = mIOsmAndAidlInterface.unregisterFromUpdates(osmandUpdatesCallbackId);
                if (unregistered) {
                    osmandUpdatesCallbackId = 0;
                }
                return true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    boolean registerForUpdates() {
        if (mIOsmAndAidlInterface != null) {
            try {
                osmandUpdatesCallbackId = (int) mIOsmAndAidlInterface.
                        registerForUpdates(5, mIOsmAndAidlCallbackInterface);
                return osmandUpdatesCallbackId > 0;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
