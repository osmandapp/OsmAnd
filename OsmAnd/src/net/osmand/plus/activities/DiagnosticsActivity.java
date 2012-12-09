package net.osmand.plus.activities;

import java.util.List;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.commands.temperature.EngineCoolantTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;
import eu.lighthouselabs.obd.enums.FuelTrim;
import eu.lighthouselabs.obd.enums.FuelType;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;
import eu.lighthouselabs.obd.reader.io.ObdGatewayService;
import eu.lighthouselabs.obd.reader.io.ObdGatewayServiceConnection;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.LogUtil;
import net.osmand.access.AccessibleActivity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

/*
 * TODO(natashaj): Look into whether this can be a plugin. Currently plugins
 *                 are too closely tied to MapActivity. 
 */
public class DiagnosticsActivity extends AccessibleActivity {

    private static final String KEY_BLUETOOTH_DEVICE = "bluetooth_device";
    private static final String KEY_ENGINE_DISPLACEMENT = "engine_displacement";
    private static final String KEY_VOLUMETRIC_EFFICIENCY = "volumetric_efficiency";
    private static final String KEY_UPDATE_PERIOD = "update_period";
    private static final String KEY_MAX_FUEL_ECONOMY = "max_fuel_econ";
    private static final String KEY_VEHICLE_ID = "vehicle_id";

    /*
     * TODO put description
     */
    static final int NO_BLUETOOTH_ID = 0;
    static final int BLUETOOTH_DISABLED = 1;
    static final int NO_GPS_ID = 2;
    static final int START_LIVE_DATA = 3;
    static final int STOP_LIVE_DATA = 4;
    static final int SETTINGS = 5;
    static final int COMMAND_ACTIVITY = 6;
    static final int TABLE_ROW_MARGIN = 7;
    static final int NO_ORIENTATION_SENSOR = 8;

    private Handler mHandler = new Handler();

    /**
     * Callback for ObdGatewayService to update UI.
     */
    private IPostListener mListener = null;
    private Intent mServiceIntent = null;
    private ObdGatewayServiceConnection mServiceConnection = null;

    private SensorManager sensorManager = null;
    private Sensor orientSensor = null;
    private SharedPreferences prefs = null;

    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;

    private boolean preRequisites = true;
    private OsmandSettings osmandSettings;
    private SavingTrackHelper savingTrackHelper;
    private int updatePeriod = 2000;

    private int speed = 1;
    private double maf = 1;
    private float ltft = 0;

    private final SensorEventListener orientListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                String dir = "";
                if (x >= 337.5 || x < 22.5) {
                    dir = "N";
                } else if (x >= 22.5 && x < 67.5) {
                    dir = "NE";
                } else if (x >= 67.5 && x < 112.5) {
                    dir = "E";
                } else if (x >= 112.5 && x < 157.5) {
                    dir = "SE";
                } else if (x >= 157.5 && x < 202.5) {
                    dir = "S";
                } else if (x >= 202.5 && x < 247.5) {
                    dir = "SW";
                } else if (x >= 247.5 && x < 292.5) {
                    dir = "W";
                } else if (x >= 292.5 && x < 337.5) {
                    dir = "NW";
                }
                // TODO(natashaj): Should we use this data to indicate compass direction?
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // TODO Auto-generated method stub
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.diagnostics_fragment);

        osmandSettings = getMyApplication().getSettings();
        savingTrackHelper = getMyApplication().getSavingTrackHelper();

        View backToMainMenuButton = findViewById(R.id.diagnosticsToMenuButton);
        backToMainMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DiagnosticsActivity.this.stopLiveData();
                Intent newIntent = new Intent(DiagnosticsActivity.this, MainMenuActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
            }
        });
        
        // Hack up data for now to make diagnostics screen look right
        // Overall Driving Score
        TextView drivingScoreText = (TextView) findViewById(R.id.drivingScoreText);
        drivingScoreText.setText("86 %");
        RatingBar drivingScoreRating = (RatingBar) findViewById(R.id.drivingScoreRating);
        drivingScoreRating.setRating(3);
        
        /*
         * Validate GPS service.
         */
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) == null) {
                /*
                 * TODO for testing purposes we'll not make GPS a pre-requisite.
                 */
                // preRequisites = false;
                showDialog(NO_GPS_ID);
        }
    
        /*
         * Validate Bluetooth service.
         */
        // Bluetooth device exists?
        final BluetoothAdapter mBtAdapter = BluetoothAdapter
                        .getDefaultAdapter();
        if (mBtAdapter == null) {
                preRequisites = false;
                showDialog(NO_BLUETOOTH_ID);
        } else {
                // Bluetooth device is enabled?
                if (!mBtAdapter.isEnabled()) {
                        preRequisites = false;
                        showDialog(BLUETOOTH_DISABLED);
                }
        }
    
        /*
         * Get Orientation sensor.
         */
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sens = sensorManager
                        .getSensorList(Sensor.TYPE_ORIENTATION);
        if (sens.size() <= 0) {
                showDialog(NO_ORIENTATION_SENSOR);
        } else {
                orientSensor = sens.get(0);
        }

        /*
         * Get update period
         */
        try {
            updatePeriod = (int) (Double.parseDouble(osmandSettings.UPDATE_PERIOD.get()) * 1000);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Couldn't parse update_period '" + osmandSettings.UPDATE_PERIOD.get() + "' as a number.", Toast.LENGTH_LONG).show();
            updatePeriod = 2000;
        }
        
        mListener = new IPostListener() {
            @Override
            public void stateUpdate(ObdCommandJob job) {
                ObdCommand command = job.getCommand();
                String cmdName = command.getName();
                String cmdResult = command.getFormattedResult();

                Log.d(LogUtil.TAG, "cmd=" + cmdName + " result=" + cmdResult);

                // TODO(natashaj): replace fuel level progress bar with some other value
                if (AvailableCommandNames.SPEED.getValue().equals(cmdName)) {
                    ((TextView) findViewById(R.id.speedText)).setText(cmdResult);
                    speed = ((SpeedObdCommand) job.getCommand()).getMetricSpeed();
                } else if (AvailableCommandNames.ENGINE_COOLANT_TEMP.getValue().equals(cmdName)) {
                    ((TextView) findViewById(R.id.coolantTempText)).setText(cmdResult);
                } else if (AvailableCommandNames.FUEL_ECONOMY.getValue().equals(cmdName)) {
                    ((TextView) findViewById(R.id.currFuelEconomyText)).setText(cmdResult);
                    ((TextView) findViewById(R.id.currMileageText)).setText(cmdResult);
                } else if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {
                    ((TextView) findViewById(R.id.currFuelEconomyText)).setText(cmdResult);
                } else if (AvailableCommandNames.MAF.getValue().equals(cmdName)) {
                    maf = ((MassAirFlowObdCommand) job.getCommand()).getMAF();
                } else if (FuelTrim.LONG_TERM_BANK_1.getBank().equals(cmdName)) {
                    ltft = ((FuelTrimObdCommand) job.getCommand()).getValue();
                }
                
                long currentTime = System.currentTimeMillis();
                savingTrackHelper.insertDiagnosticData(command.getCommand(), command.getResult(), currentTime, osmandSettings);

                TextView obdStats = ((TextView) findViewById(R.id.obdStatsText));
                obdStats.append(cmdName + "\t\t:\t" + cmdResult + "\n");
            }
        };

        try {
            // validate app pre-requisites
            if (preRequisites) {
                /*
                 * Prepare service and its connection
                 */
                mServiceIntent = new Intent("eu.lighthouselabs.obd.reader.io.ObdGatewayService");
                //mServiceIntent = new Intent(this, ObdGatewayService.class);
                addObdSettingsToBundle(mServiceIntent);
                mServiceConnection = new ObdGatewayServiceConnection();
                mServiceConnection.setServiceListener(mListener);
    
                // bind service
                Log.d(LogUtil.TAG, "Binding service..");
                boolean bindResult = bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                Log.d(LogUtil.TAG, "Binding result " + bindResult);
            }
        } catch (Exception e) {
            Log.d(LogUtil.TAG, "exception binding service: " + e);
        }
        
    }

    private OsmandApplication getMyApplication() {
        return ((OsmandApplication) getApplication());
    }
    
    private void addObdSettingsToBundle(Intent intent) {
        Bundle bundle = new Bundle();
        
        String bluetoothDevice = osmandSettings.BLUETOOTH_LIST.get();
        bundle.putString(KEY_BLUETOOTH_DEVICE, bluetoothDevice);
        String vehicleId = osmandSettings.VEHICLE_ID.get();
        bundle.putString(KEY_VEHICLE_ID, vehicleId);
        
        addDoubleToBundle(bundle, KEY_ENGINE_DISPLACEMENT, osmandSettings.ENGINE_DISPLACEMENT.get());
        addDoubleToBundle(bundle, KEY_VOLUMETRIC_EFFICIENCY, osmandSettings.VOLUMETRIC_EFFICIENCY.get());
        addDoubleToBundle(bundle, KEY_UPDATE_PERIOD, osmandSettings.UPDATE_PERIOD.get());
        addDoubleToBundle(bundle, KEY_MAX_FUEL_ECONOMY, osmandSettings.MAX_FUEL_ECONOMY.get());

        intent.putExtras(bundle);
    }
    
    private boolean addDoubleToBundle(Bundle bundle, String key, String text) {
        try {
            bundle.putDouble(key, Double.parseDouble(text));
            return true;
        } catch (Exception e) {
            Toast.makeText(this,
                    "Couldn't parse '" + text.toString() + "' as a number.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    protected void onDestroy() {
            super.onDestroy();

            releaseWakeLockIfHeld();
            mServiceIntent = null;
            mServiceConnection = null;
            mListener = null;
            mHandler = null;
    }

    @Override
    protected void onPause() {
            super.onPause();
            Log.d(LogUtil.TAG, "Pausing..");
            releaseWakeLockIfHeld();
    }

    /**
     * If lock is held, release. Lock will be held when the service is running.
     */
    private void releaseWakeLockIfHeld() {
            if (wakeLock.isHeld()) {
                    wakeLock.release();
            }
    }

    @Override
    protected void onResume() {
            super.onResume();

            Log.d(LogUtil.TAG, "Resuming..");

            sensorManager.registerListener(orientListener, orientSensor,
                            SensorManager.SENSOR_DELAY_UI);
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                            "ObdReader");
            
            startLiveData();
    }

    private void startLiveData() {
        Log.d(LogUtil.TAG, "Starting live data..");

        if (!mServiceConnection.isRunning()) {
                Log.d(LogUtil.TAG, "Service is not running. Going to start it..");
                startService(mServiceIntent);
        }

        // start command execution
        mHandler.post(mQueueCommands);

        // screen won't turn off until wakeLock.release()
        wakeLock.acquire();
    }
    
    private void stopLiveData() {
            Log.d(LogUtil.TAG, "Stopping live data..");
    
            if (mServiceConnection != null && mServiceConnection.isRunning())
                    stopService(mServiceIntent);
    
            // remove runnable
            mHandler.removeCallbacks(mQueueCommands);
    
            releaseWakeLockIfHeld();
    }
    
    /**
     * 
     */
    private Runnable mQueueCommands = new Runnable() {
        @Override
        public void run() {
            /*
             * If values are not default, then we have values to calculate MPG
             */
            Log.d(LogUtil.TAG, "SPEED:" + speed + ", MAF:" + maf + ", LTFT:" + ltft);
            if (speed > 1 && maf > 1 && ltft != 0) {
                FuelEconomyWithMAFObdCommand fuelEconCmd = new FuelEconomyWithMAFObdCommand(
                        FuelType.DIESEL, speed, maf, ltft, false); // TODO
                TextView mileage = (TextView) findViewById(R.id.currMileageText);
                String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
                mileage.setText("" + liters100km);
                Log.d(LogUtil.TAG, "FUELECON:" + liters100km);
            }

            if (mServiceConnection.isRunning())
                queueCommands();

            mHandler.postDelayed(mQueueCommands, updatePeriod);
        }
    };

    private void addJobToQueue(ObdCommand command) {
        final ObdCommandJob job = new ObdCommandJob(command);
        mServiceConnection.addJobToQueue(job);
    }
    
    private void queueCommands() {
        addJobToQueue(new SpeedObdCommand());
        addJobToQueue(new FuelEconomyObdCommand());
        addJobToQueue(new FuelLevelObdCommand());
        addJobToQueue(new EngineCoolantTemperatureObdCommand());
        addJobToQueue(new EngineRPMObdCommand());

        addJobToQueue(new AmbientAirTemperatureObdCommand());
        addJobToQueue(new MassAirFlowObdCommand());
        addJobToQueue(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_1));
        addJobToQueue(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_2));
        addJobToQueue(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_1));
        addJobToQueue(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_2));
        addJobToQueue(new CommandEquivRatioObdCommand());
    }

}
