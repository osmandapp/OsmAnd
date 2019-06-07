package net.osmand.turn_screen_on;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import net.osmand.turn_screen_on.receiver.DeviceAdminRecv;

public class AdminDevicePermissionSetUpActivity extends AppCompatActivity
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private ComponentName mDeviceAdmin;
    private PluginSettings settings;
    private static final int DEVICE_ADMIN_REQUEST = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_device_permission_set_up);

        settings = PluginSettings.getInstance();

        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        mDeviceAdmin = new ComponentName(getApplicationContext(),
                DeviceAdminRecv.class);

        if (mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
            settings.enablePlugin();
        } else {
            requestLockScreenAdmin();
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DEVICE_ADMIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                settings.enablePlugin();
                Log.d("ttpl", "Lock screen permission approved.");
            } else {
                settings.disablePlugin();
                //todo refactor
//                settings.WAKE_ON_VOICE_INT.set(0);
                Log.d("ttpl", "Lock screen permission refused.");
            }
            return;
        }
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

    //TODO CHANGE_PREFERENCE
    //TODO WAKE_ON_VOICE
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String id = preference.getKey();
//        if (id.equals(settings.WAKE_ON_VOICE_INT.getId())) {
//            Integer value;
//            try {
//                value = Integer.parseInt(newValue.toString());
//            } catch (NumberFormatException e) {
//                value = 0;
//            }
//            if (value > 0) {
//                requestLockScreenAdmin();
//            }
//        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
