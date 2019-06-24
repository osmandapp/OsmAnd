package net.osmand.turnScreenOn;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.log.PlatformUtil;

import org.apache.commons.logging.Log;

public class OsmandInstallActivity extends AppCompatActivity {
    private static final Log LOG = PlatformUtil.getLog(OsmandInstallActivity.class);

    public static final int MODE_FIRST_OPEN = 1;
    public static final int MODE_LOST_OSMAND = 2;
    public static final String MODE_KEY = "mode";

    private TurnScreenApp app;
    private PluginSettings settings;
    private int currentMode;

    private Toolbar toolbar;
    private ImageView btnDownloadFromPlayMarket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osmand_install);

        app = (TurnScreenApp) getApplicationContext();
        settings = app.getSettings();

        currentMode = getIntent().getIntExtra(MODE_KEY, MODE_LOST_OSMAND);

        btnDownloadFromPlayMarket = findViewById(R.id.btnDownloadFromPlayMarket);
        btnDownloadFromPlayMarket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String appPackageName = PluginSettings.OsmandVersion.OSMAND.getPath();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

        toolbar = findViewById(R.id.tbOsmandInstallToolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settings.hasAvailableOsmandVersions()) {
            onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_install, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_skip) {
            if (currentMode == MODE_FIRST_OPEN) {
                if(!settings.isAdminDevicePermissionAvailable()) {
                    Intent intent = new Intent(OsmandInstallActivity.this, PermissionsSetUpActivity.class);
                    startActivity(intent);
                } else {
                    onBackPressed();
                }
            }
        } else if (id == android.R.id.home && currentMode == MODE_FIRST_OPEN) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
}
