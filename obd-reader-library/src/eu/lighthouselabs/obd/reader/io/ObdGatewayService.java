/*
 * TODO put header
 */
package eu.lighthouselabs.obd.reader.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import eu.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import eu.lighthouselabs.obd.commands.protocol.ObdResetCommand;
import eu.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import eu.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.ObdProtocols;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.IPostMonitor;
import eu.lighthouselabs.obd.reader.R;
import eu.lighthouselabs.obd.reader.activity.ConfigActivity;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob.ObdCommandJobState;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * 
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
public class ObdGatewayService extends Service {

	private static final String TAG = "ObdGatewayService";

	private static final String KEY_BLUETOOTH_DEVICE = "bluetooth_device";
	private static final String KEY_ENGINE_DISPLACEMENT = "engine_displacement";
	private static final String KEY_VOLUMETRIC_EFFICIENCY = "volumetric_efficiency";
	private static final String KEY_UPDATE_PERIOD = "update_period";
	private static final String KEY_MAX_FUEL_ECONOMY = "max_fuel_econ";
	private static final String KEY_VEHICLE_ID = "vehicle_id";

	private IPostListener _callback = null;
	private final Binder _binder = new LocalBinder();
	private AtomicBoolean _isRunning = new AtomicBoolean(false);
	private NotificationManager _notifManager;

	private BlockingQueue<ObdCommandJob> _queue = new LinkedBlockingQueue<ObdCommandJob>();
	private AtomicBoolean _isQueueRunning = new AtomicBoolean(false);
	private Long _queueCounter = 0L;

	private BluetoothDevice _dev = null;
	private BluetoothSocket _sock = null;

	private String remoteDevice;
        private int period;
        private double ve;
        private double ed;
        private boolean imperialUnits = false;
        private ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();

        /*
	 * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
	 * #createRfcommSocketToServiceRecord(java.util.UUID)
	 * 
	 * "Hint: If you are connecting to a Bluetooth serial board then try using
	 * the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if
	 * you are connecting to an Android peer then please generate your own
	 * unique UUID."
	 */
	private static final UUID MY_UUID = UUID
	        .fromString("00001101-0000-1000-8000-00805F9B34FB");

	/**
	 * As long as the service is bound to another component, say an Activity, it
	 * will remain alive.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onCreate() {
		_notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		//showNotification();
	}

	@Override
	public void onDestroy() {
		stopService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received start id " + startId + ": " + intent);

		getSettingsFromBundle(intent.getExtras());
		
		/*
		 * Register listener Start OBD connection
		 */
		startService();

		/*
		 * We want this service to continue running until it is explicitly
		 * stopped, so return sticky.
		 */
		return START_STICKY;
	}

	private void getSettingsFromBundle(Bundle bundle) {
	    remoteDevice = bundle.getString(KEY_BLUETOOTH_DEVICE);
	    ed = bundle.getDouble(KEY_ENGINE_DISPLACEMENT);
	    ve = bundle.getDouble(KEY_VOLUMETRIC_EFFICIENCY);
	    period = bundle.getInt(KEY_UPDATE_PERIOD);
	}
	
	private void startService() {
		Log.d(TAG, "Starting service..");

		/*
		 * Retrieve preferences
		 */
		SharedPreferences prefs = PreferenceManager
		        .getDefaultSharedPreferences(this);

		/*
		 * Let's get the remote Bluetooth device
		 */
		//String remoteDevice = prefs.getString(
		//        ConfigActivity.BLUETOOTH_LIST_KEY, null);
		if (remoteDevice == null || "".equals(remoteDevice)) {
			Toast.makeText(this, "No Bluetooth device selected",
			        Toast.LENGTH_LONG).show();

			// log error
			Log.e(TAG, "No Bluetooth device has been selected.");

			// TODO kill this service gracefully
			stopService();
			
			return;
		}

		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		_dev = btAdapter.getRemoteDevice(remoteDevice);

		/*
		 * TODO put this as deprecated Determine if upload is enabled
		 */
		// boolean uploadEnabled = prefs.getBoolean(
		// ConfigActivity.UPLOAD_DATA_KEY, false);
		// String uploadUrl = null;
		// if (uploadEnabled) {
		// uploadUrl = prefs.getString(ConfigActivity.UPLOAD_URL_KEY,
		// null);
		// }

		/*
		 * Get GPS
		 */
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		boolean gps = prefs.getBoolean(ConfigActivity.ENABLE_GPS_KEY, false);

		/*
		 * TODO clean
		 * 
		 * Get more preferences
		 */
		//int period = ConfigActivity.getUpdatePeriod(prefs);
		//double ve = ConfigActivity.getVolumetricEfficieny(prefs);
		//double ed = ConfigActivity.getEngineDisplacement(prefs);
		//boolean imperialUnits = prefs.getBoolean(
		//        ConfigActivity.IMPERIAL_UNITS_KEY, false);
		//ArrayList<ObdCommand> cmds = ConfigActivity.getObdCommands(prefs);

		/*
		 * Establish Bluetooth connection
		 * 
		 * Because discovery is a heavyweight procedure for the Bluetooth
		 * adapter, this method should always be called before attempting to
		 * connect to a remote device with connect(). Discovery is not managed
		 * by the Activity, but is run as a system service, so an application
		 * should always call cancel discovery even if it did not directly
		 * request a discovery, just to be sure. If Bluetooth state is not
		 * STATE_ON, this API will return false.
		 * 
		 * see
		 * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
		 * .html#cancelDiscovery()
		 */
		Log.d(TAG, "Stopping Bluetooth discovery.");
		btAdapter.cancelDiscovery();

		Toast.makeText(this, "Starting OBD connection..", Toast.LENGTH_SHORT);

		try {
			startObdConnection();
		} catch (Exception e) {
			Log.e(TAG, "There was an error while establishing connection. -> "
			        + e.getMessage());

			// in case of failure, stop this service.
			stopService();
		}
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 
	 * @throws IOException
	 */
	private void startObdConnection() throws IOException {
		Log.d(TAG, "Starting OBD connection..");

		// Instantiate a BluetoothSocket for the remote device and connect it.
		_sock = _dev.createRfcommSocketToServiceRecord(MY_UUID);
		_sock.connect();

		// Let's configure the connection.
		Log.d(TAG, "Queing jobs for connection configuration..");
		queueJob(new ObdCommandJob(new ObdResetCommand()));
		queueJob(new ObdCommandJob(new EchoOffObdCommand()));

		/*
		 * Will send second-time based on tests.
		 * 
		 * TODO this can be done w/o having to queue jobs by just issuing
		 * command.run(), command.getResult() and validate the result.
		 */
		queueJob(new ObdCommandJob(new EchoOffObdCommand()));
		queueJob(new ObdCommandJob(new LineFeedOffObdCommand()));
		queueJob(new ObdCommandJob(new TimeoutObdCommand(62)));

		// For now set protocol to AUTO
		queueJob(new ObdCommandJob(new SelectProtocolObdCommand(
		        ObdProtocols.AUTO)));
		
		// Job for returning dummy data
		queueJob(new ObdCommandJob(new AmbientAirTemperatureObdCommand()));

		Log.d(TAG, "Initialization jobs queued.");

		// Service is running..
		_isRunning.set(true);

		// Set queue execution counter
		_queueCounter = 0L;
	}

	/**
	 * Runs the queue until the service is stopped
	 */
	private void _executeQueue() {
		Log.d(TAG, "Executing queue..");

		_isQueueRunning.set(true);

		while (!_queue.isEmpty()) {
			ObdCommandJob job = null;
			try {
				job = _queue.take();

				// log job
				Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

				if (job.getState().equals(ObdCommandJobState.NEW)) {
					Log.d(TAG, "Job state is NEW. Run it..");

					job.setState(ObdCommandJobState.RUNNING);
					job.getCommand().run(_sock.getInputStream(),
					        _sock.getOutputStream());
				} else {
					// log not new job
					Log.e(TAG,
					        "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
				}
			} catch (Exception e) {
				job.setState(ObdCommandJobState.EXECUTION_ERROR);
				Log.e(TAG, "Failed to run command. -> " + e.getMessage());
			}

			if (job != null) {
				Log.d(TAG, "Job is finished.");
				job.setState(ObdCommandJobState.FINISHED);
				_callback.stateUpdate(job);
			}
		}

		_isQueueRunning.set(false);
	}

	/**
	 * This method will add a job to the queue while setting its ID to the
	 * internal queue counter.
	 * 
	 * @param job
	 * @return
	 */
	public Long queueJob(ObdCommandJob job) {
		_queueCounter++;
		Log.d(TAG, "Adding job[" + _queueCounter + "] to queue..");

		job.setId(_queueCounter);
		try {
			_queue.put(job);
		} catch (InterruptedException e) {
			job.setState(ObdCommandJobState.QUEUE_ERROR);
			// log error
			Log.e(TAG, "Failed to queue job.");
		}

		Log.d(TAG, "Job queued successfully.");
		return _queueCounter;
	}

	/**
	 * Stop OBD connection and queue processing.
	 */
	public void stopService() {
		Log.d(TAG, "Stopping service..");

		clearNotification();
		_queue.removeAll(_queue); // TODO is this safe?
		_isQueueRunning.set(false);
		_callback = null;
		_isRunning.set(false);

		// close socket
		try {
			if (_sock != null)
			    _sock.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		// kill service
		stopSelf();
	}

	/**
	 * Show a notification while this service is running.
	 */
	/*
	private void showNotification() {
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon,
		        getText(R.string.service_started), System.currentTimeMillis());

		// Launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        new Intent(this, MainActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
		        getText(R.string.notification_label),
		        getText(R.string.service_started), contentIntent);

		// Send the notification.
		_notifManager.notify(R.string.service_started, notification);
	}
	*/

	/**
	 * Clear notification.
	 */
	private void clearNotification() {
		_notifManager.cancel(R.string.service_started);
	}

	/**
	 * TODO put description
	 */
	public class LocalBinder extends Binder implements IPostMonitor {
		public void setListener(IPostListener callback) {
			_callback = callback;
		}

		public boolean isRunning() {
			return _isRunning.get();
		}

		public void executeQueue() {
			_executeQueue();
		}

		public void addJobToQueue(ObdCommandJob job) {
			Log.d(TAG, "Adding job [" + job.getCommand().getName() + "] to queue.");
			_queue.add(job);

			if (!_isQueueRunning.get())
				_executeQueue();
		}
	}

}