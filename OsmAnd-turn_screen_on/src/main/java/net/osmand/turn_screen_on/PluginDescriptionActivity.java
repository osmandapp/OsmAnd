package net.osmand.turn_screen_on;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

public class PluginDescriptionActivity extends AppCompatActivity {
    private FrameLayout btnContinue;
    private PluginSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_description);

        settings = PluginSettings.getInstance();

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
