package net.osmand.turn_screen_on;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.osmand.turn_screen_on.app.TurnScreenOnApplication;
import net.osmand.turn_screen_on.helpers.LockHelper;
import net.osmand.turn_screen_on.helpers.OsmAndAidlHelper;
import net.osmand.turn_screen_on.helpers.WakeLockHelper;
import net.osmand.turn_screen_on.receiver.DeviceAdminRecv;

public class Some {
    private Button button;
    private Button btnShow;
    private Button btnConnect;
    private Button btnRegister;
    private ComponentName mDeviceAdmin;
    private WakeLockHelper wakeLockHelper;
    private LockHelper lockHelper;
    private OsmAndAidlHelper helper;

    private static final int DEVICE_ADMIN_REQUEST = 5;

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //todo refactor
        wakeLockHelper = new WakeLockHelper(TurnScreenOnApplication.getAppContext());
        lockHelper = new LockHelper(TurnScreenOnApplication.getAppContext());

        mDeviceAdmin = new ComponentName(getApplicationContext(),
                DeviceAdminRecv.class);
        final DevicePolicyManager deviceManger = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        helper = new OsmAndAidlHelper();

        button = findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "locked", Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                lockHelper.lock();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        lockHelper.timedUnlock(5000);
                    }
                }, 5000);
            }
        });

        btnShow = findViewById(R.id.btnShow);
        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.show();
            }
        });

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.bindService("net.osmand.plus", getApplication());
            }
        });

        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.register();
            }
        });
    }

    private void requestLockScreenAdmin() {
        mDeviceAdmin = new ComponentName(getApplicationContext(),
                DeviceAdminRecv.class);

        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        if (!mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
            // request permission from user
            Intent intent = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    mDeviceAdmin);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION, "lock_screen_request_explanation");
            startActivityForResult(intent, DEVICE_ADMIN_REQUEST);
        }
    }*/
}
