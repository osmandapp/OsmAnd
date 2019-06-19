package net.osmand.turnScreenOn;

import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import net.osmand.turnScreenOn.app.TurnScreenApp;

public class PluginDescriptionActivity extends AppCompatActivity {
    private FrameLayout btnContinue;
    private PluginSettings settings;

    private final static String PERMISSION_WINDOW_OPENED = "permissionWindowOpened";

    private TurnScreenApp app;

    private boolean isPermissionWindowOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_description);

        if (savedInstanceState != null) {
            isPermissionWindowOpened = savedInstanceState.getBoolean(PERMISSION_WINDOW_OPENED, false);
        }

        app = (TurnScreenApp) getApplicationContext();

        settings = app.getSettings();

        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!settings.isAdminPermissionAvailable()) {
                    Intent intent = new Intent(PluginDescriptionActivity.this, PermissionsSetUpActivity.class);
                    startActivity(intent);
                    isPermissionWindowOpened = true;
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

        if (isPermissionWindowOpened) {
            onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(PERMISSION_WINDOW_OPENED, isPermissionWindowOpened);
    }
}
