package net.osmand.turnScreenOn;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import net.osmand.turnScreenOn.app.TurnScreenApp;

public class PluginDescriptionActivity extends AppCompatActivity {
    private FrameLayout btnContinue;
    private PluginSettings settings;

    private TurnScreenApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_description);

        app = new TurnScreenApp(this);

        settings = app.getSettings();

        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (settings.isAdminPermissionAvailable()) {
                    Intent intent = new Intent(PluginDescriptionActivity.this, PermissionsSetUpActivity.class);
                    startActivity(intent);

                } else {
                    onBackPressed();

                }
            }
        });
    }
}
