package net.osmand.turnScreenOn;

import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import net.osmand.turnScreenOn.app.TurnScreenApp;

public class PluginDescriptionActivity extends AppCompatActivity {
    public static final int MODE_FIRST_OPEN = 1;
    public static final int MODE_HELP = 2;
    public static final String MODE_KEY = "mode";

    private FrameLayout btnContinue;
    private PluginSettings settings;
    private int currentMode;

    private TurnScreenApp app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_description);

        setStatusBarColor(R.color.white);

        currentMode = getIntent().getIntExtra(MODE_KEY, MODE_HELP);

        app = (TurnScreenApp) getApplicationContext();
        settings = app.getSettings();

        btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!settings.hasAvailableOsmandVersions() && currentMode == MODE_FIRST_OPEN) {
                    Intent intent = new Intent(PluginDescriptionActivity.this, OsmandInstallActivity.class);
                    intent.putExtra(OsmandInstallActivity.MODE_KEY, OsmandInstallActivity.MODE_FIRST_OPEN);
                    startActivity(intent);
                }

                else if (!settings.isAdminDevicePermissionAvailable() && currentMode == MODE_FIRST_OPEN) {
                    Intent intent = new Intent(PluginDescriptionActivity.this, PermissionsSetUpActivity.class);
                    startActivity(intent);
                } else {
                    onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }

        if (settings.isAdminDevicePermissionAvailable()
                && settings.hasAvailableOsmandVersions() && currentMode == MODE_FIRST_OPEN) {
            onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void setStatusBarColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, colorResId));
        }
    }
}
