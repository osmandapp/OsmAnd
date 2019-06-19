package net.osmand.turnScreenOn;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.receiver.DeviceAdminRecv;

public class PermissionsSetUpActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private FrameLayout btnContinue;
    private ComponentName mDeviceAdmin;
    private PluginSettings settings;
    private static final int DEVICE_ADMIN_REQUEST = 5;

    private TurnScreenApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_device_permission_set_up);

        toolbar = findViewById(R.id.tbPermissionActivityToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        app = (TurnScreenApp) getApplicationContext();

        settings = app.getSettings();

        mDeviceAdmin = new ComponentName(getApplicationContext(), DeviceAdminRecv.class);

        btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLockScreenAdmin();
                onBackPressed();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DEVICE_ADMIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                settings.enablePlugin();
            } else {
                settings.disablePlugin();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestLockScreenAdmin() {
        // request permission from user
        Intent intent = new Intent(
                DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                mDeviceAdmin);
        intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.lock_screen_request_explanation,
                        ""));
        startActivityForResult(intent, DEVICE_ADMIN_REQUEST);
    }
}
