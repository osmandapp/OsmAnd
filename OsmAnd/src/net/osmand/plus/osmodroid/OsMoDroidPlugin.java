package net.osmand.plus.osmodroid;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import org.apache.commons.logging.Log;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.OsMoDroid.IRemoteOsMoDroidService;

public class OsMoDroidPlugin extends OsmandPlugin {

	public static final String ID = "osmand.osmodroid";
	private static final Log log = PlatformUtil.getLog(OsMoDroidPlugin.class);
	private OsmandApplication app;
	IRemoteOsMoDroidService mIRemoteService;
	private ServiceConnection mConnection;
	private int OSMODROID_SUPPORTED_VERSION_MIN = 0;

	@Override
	public String getId() {
		return ID;
	}

	public OsMoDroidPlugin(OsmandApplication app) {
		this.app = app;

	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmodroid_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmodroid_plugin_name);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		mConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mIRemoteService = IRemoteOsMoDroidService.Stub.asInterface(service);
				try {
					System.out.println(mIRemoteService.getVersion());
					if(mIRemoteService.getVersion() < OSMODROID_SUPPORTED_VERSION_MIN) {
						app.showToastMessage(R.string.osmodroid_plugin_old_ver_not_supported);
						shutdown(app);
					}
				} catch (RemoteException e) {
					log.error(e.getMessage(), e);
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mIRemoteService = null;
			}
		};
		Intent serviceIntent = (new Intent("OsMoDroid.remote"));
		app.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {

	}
	
	@Override
	public void disable(OsmandApplication app) {
		shutdown(app);
	}

	private void shutdown(OsmandApplication app) {
		if (mIRemoteService != null) {
			app.unbindService(mConnection);
			mIRemoteService = null;
		}
	}

}
