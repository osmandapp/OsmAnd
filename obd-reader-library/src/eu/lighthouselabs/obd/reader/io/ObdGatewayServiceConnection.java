/*
 * TODO put header
 */
package eu.lighthouselabs.obd.reader.io;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.IPostMonitor;

/**
 * Service connection for ObdGatewayService.
 */
public class ObdGatewayServiceConnection implements ServiceConnection {

	private static final String TAG = "ObdGatewayServiceConnection";

	private IPostMonitor _service = null;
	private IPostListener _listener = null;

	public void onServiceConnected(ComponentName name, IBinder binder) {
		_service = (IPostMonitor) binder;
		_service.setListener(_listener);
	}

	public void onServiceDisconnected(ComponentName name) {
		_service = null;
		Log.d(TAG, "Service is disconnected.");
	}

	/**
	 * @return true if service is running, false otherwise.
	 */
	public boolean isRunning() {
		if (_service == null) {
			return false;
		}

		return _service.isRunning();
	}

	/**
	 * Queue JobObdCommand.
	 * 
	 * @param the
	 *            job
	 */
	public void addJobToQueue(ObdCommandJob job) {
		if (null != _service)
			_service.addJobToQueue(job);
	}

	/**
	 * Sets a callback in the service.
	 * 
	 * @param listener
	 */
	public void setServiceListener(IPostListener listener) {
		_listener = listener;
	}

}