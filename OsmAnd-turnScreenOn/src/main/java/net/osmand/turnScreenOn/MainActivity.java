package net.osmand.turnScreenOn;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.helpers.AndroidUtils;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;
import net.osmand.turnScreenOn.helpers.SensorHelper;

import java.util.List;

import static android.view.View.LAYER_TYPE_HARDWARE;

public class MainActivity extends AppCompatActivity {
    private FrameLayout flRootLayout;
    private LinearLayout llElementsList;
    private Toolbar tbPluginToolbar;
    private FrameLayout btnOpenOsmand;
    private SwitchCompat swPluginEnableSwitcher;
    private SwitchCompat swSensorEnableSwitcher;
    private TextView tvPluginStateDescription;
    private Spinner spTime;
    private LinearLayout llPluginPreferencesLayout;
    private LinearLayout llOsmandVersions;

    private List<PluginSettings.OsmandVersion> availableOsmandVersions;

    private Paint pGreyScale;

    private TurnScreenApp app;
    private OsmAndAidlHelper osmAndAidlHelper;
    private PluginSettings settings;
    private PluginState pluginState;
    private SensorHelper sensorHelper;

    private LayoutInflater inflater;

    interface PluginState {
        void createUI();

        void refreshUI();
    }

    private PluginState NO_INSTALLED_OSMAND_STATE = new PluginState() {
        @Override
        public void createUI() {
            llElementsList.addView(inflater.inflate(R.layout.main_el_plugin_switcher, null, false));
            llElementsList.addView(inflater.inflate(R.layout.main_install_desc, null, false));

            flRootLayout.addView(llElementsList);

            llPluginPreferencesLayout = findViewById(R.id.llPluginPreferencesLayout);
            swPluginEnableSwitcher = findViewById(R.id.swPluginEnableSwitcher);
            tvPluginStateDescription = findViewById(R.id.tvPluginStateDescription);
        }

        @Override
        public void refreshUI() {
            swPluginEnableSwitcher.setChecked(false);
            tvPluginStateDescription.setText(R.string.disabled);
            swPluginEnableSwitcher.setEnabled(false);
        }
    };

    private PluginState PLUGIN_PREFERENCE_STATE = new PluginState() {
        @Override
        public void createUI() {
            //add interface elements
            llElementsList.addView(inflater.inflate(R.layout.main_el_plugin_switcher, null, false));
            llElementsList.addView(inflater.inflate(R.layout.main_el_time_set_up, null, false));
            llElementsList.addView(inflater.inflate(R.layout.main_el_sensor, null, false));

            View btnOpenOsmandLayout = inflater.inflate(R.layout.main_el_btn_open_osmand, null, false);
            flRootLayout.addView(btnOpenOsmandLayout);

            llElementsList.setPadding(0, 0, 0, AndroidUtils.dpToPx(getApplicationContext(), 82));

            ScrollView sv = new ScrollView(MainActivity.this);
            sv.addView(llElementsList);
            //getting control elements
            flRootLayout.addView(sv);

            llPluginPreferencesLayout = findViewById(R.id.llPluginPreferencesLayout);
            swPluginEnableSwitcher = findViewById(R.id.swPluginEnableSwitcher);
            tvPluginStateDescription = findViewById(R.id.tvPluginStateDescription);
            btnOpenOsmand = findViewById(R.id.btnOpenOsmand);
            spTime = findViewById(R.id.spTime);
            swSensorEnableSwitcher = findViewById(R.id.swSensorEnableSwitcher);

            if (availableOsmandVersions.size() > 1) {
                llElementsList.addView(inflater.inflate(R.layout.main_el_osmand_versions, null, false));
                llOsmandVersions = findViewById(R.id.llOsmandVersions);
                createVersionUI();
            }


            btnOpenOsmand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(settings.getOsmandVersion().getPath());
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    }
                }
            });

            swPluginEnableSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (!settings.isPermissionAvailable()) {
                            Intent intent = new Intent(MainActivity.this, PermissionsSetUpActivity.class);
                            startActivity(intent);
                        } else {
                            settings.enablePlugin();
                        }
                    } else {
                        settings.disablePlugin();
                    }
                    refreshUI();
                }
            });

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    MainActivity.this,
                    android.R.layout.simple_spinner_item,
                    settings.getUnlockTimeDescriptionList()
            );

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spTime.setAdapter(adapter);
            spTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent,
                                           View itemSelected, int selectedItemPosition, long selectedId) {
                    settings.setTimeModePosition(selectedItemPosition);
                }

                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            swSensorEnableSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        settings.enableSensor();
                    } else {
                        settings.disableSensor();
                    }
                    refreshUI();
                }
            });

            btnOpenOsmandLayout.bringToFront();
        }

        @Override
        public void refreshUI() {
            boolean isPluginEnabled = settings.isPluginEnabled();

            if (isPluginEnabled) {
                setStatusBarColor(R.color.orange);
                osmAndAidlHelper.register();

                tvPluginStateDescription.setText(getString(R.string.enabled));
                tvPluginStateDescription.setTextColor(getResources().getColor(R.color.black));
                tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.orange));

                llPluginPreferencesLayout.setLayerType(LAYER_TYPE_HARDWARE, null);

                setEnableForElements(true);
            } else {
                setStatusBarColor(R.color.darkGrey);
//            osmAndAidlHelper.unregister();

                tvPluginStateDescription.setText(getString(R.string.disabled));
                tvPluginStateDescription.setTextColor(getResources().getColor(R.color.darkGrey));
                tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.darkGrey));

                llPluginPreferencesLayout.setLayerType(LAYER_TYPE_HARDWARE, pGreyScale);

                setEnableForElements(false);
            }

            swPluginEnableSwitcher.setChecked(isPluginEnabled);

            boolean isSensorEnabled = settings.isSensorEnabled();
            swSensorEnableSwitcher.setChecked(isSensorEnabled);

            if (isSensorEnabled) {
                sensorHelper.switchOnSensor();
            } else {
                sensorHelper.switchOffSensor();
            }

            int timePosition = settings.getTimeModePosition();
            spTime.setSelection(timePosition);

            PluginSettings.OsmandVersion version = settings.getOsmandVersion();
            int checkedId = version.getId();
            RadioButton checkedVersion = findViewById(checkedId);
            if (checkedVersion != null) {
                checkedVersion.setChecked(true);
            }
        }

        public void createVersionUI() {
            for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                ViewGroup container = (ViewGroup) inflater
                        .inflate(R.layout.osmand_version_item, null, false);

                container.setMinimumHeight(52);

                ImageView ivVersionImg = container.findViewById(R.id.ivVersionImg);
                ivVersionImg.setImageResource(version.getImgResId());

                TextView tvVersionName = container.findViewById(R.id.tvVersionName);
                tvVersionName.setText(getString(version.getNameId()));

                RadioButton rbVersionCheckButton = container.findViewById(R.id.rbVersion);
                rbVersionCheckButton.setId(version.getId());
                rbVersionCheckButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int id = v.getId();
                        settings.setOsmandVersion(id);

                        for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                            if (id != version.getId()) {
                                RadioButton rbCurrent = findViewById(version.getId());
                                rbCurrent.setChecked(false);
                            }
                        }
                    }
                });

                llOsmandVersions.addView(container);
            }
        }

        private void setEnableForElements(boolean enable) {
            swSensorEnableSwitcher.setEnabled(enable);
            for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                RadioButton rb = findViewById(version.getId());
                if (rb!=null) {
                    rb.setEnabled(enable);
                }
            }
            spTime.setEnabled(enable);
            btnOpenOsmand.setEnabled(enable);

            //todo delete
            osmAndAidlHelper.reconnectOsmand();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = new TurnScreenApp(this);

        osmAndAidlHelper = app.getOsmAndAidlHelper();
        settings = app.getSettings();

        inflater = getLayoutInflater();

        availableOsmandVersions = PluginSettings.OsmandVersion.getOnlyInstalledVersions();

        if (availableOsmandVersions == null || availableOsmandVersions.size() == 0) {
            pluginState = NO_INSTALLED_OSMAND_STATE;
        } else {
            pluginState = PLUGIN_PREFERENCE_STATE;
        }

        if (!settings.programWasOpenedEarlier()){
            settings.setOpened();
            Intent intent = new Intent(this, PluginDescriptionActivity.class);
            startActivity(intent);
        }

        //todo refactor
        sensorHelper = app.getSensorHelper();

        createUI();
    }

    public void createUI() {
        //grey filter preparation
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        pGreyScale = new Paint();
        pGreyScale.setColorFilter(new ColorMatrixColorFilter(cm));

        flRootLayout = findViewById(R.id.flRootLayout);
        llElementsList = new LinearLayout(this);
        llElementsList.setOrientation(LinearLayout.VERTICAL);
        llElementsList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup
                .LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tbPluginToolbar = findViewById(R.id.tbPluginToolbar);
        setSupportActionBar(tbPluginToolbar);

        pluginState.createUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_help) {
            Intent intent = new Intent(MainActivity.this, PluginDescriptionActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    public void refreshUI() {
        pluginState.refreshUI();
    }

    private void setStatusBarColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, colorResId));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
