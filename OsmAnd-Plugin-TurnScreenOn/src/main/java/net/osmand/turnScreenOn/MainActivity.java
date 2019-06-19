package net.osmand.turnScreenOn;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.dialog.PluginStandardDialog;
import net.osmand.turnScreenOn.helpers.AndroidUtils;
import net.osmand.turnScreenOn.helpers.LockHelper;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;
import net.osmand.turnScreenOn.helpers.SensorHelper;
import net.osmand.turnScreenOn.listener.UnlockMessageListener;
import net.osmand.turnScreenOn.listener.OnMessageListener;

import java.util.List;

import static android.view.View.LAYER_TYPE_HARDWARE;

public class MainActivity extends AppCompatActivity {
    private FrameLayout flRootLayout;
    private LinearLayout llElementsList;
    private Toolbar tbPluginToolbar;
    private FrameLayout btnOpenOsmand;
    private FrameLayout flPanelPluginEnable;
    private SwitchCompat swPluginEnableSwitcher;
    private FrameLayout flPanelSensor;
    private SwitchCompat swSensorEnableSwitcher;
    private TextView tvPluginStateDescription;
    private FrameLayout flPanelTime;
    private TextView tvTime;
    private LinearLayout llPluginPreferencesLayout;
    private LinearLayout llOsmandVersions;

    private List<PluginSettings.OsmandVersion> availableOsmandVersions;

    private Paint pGreyScale;

    private TurnScreenApp app;
    private OsmAndAidlHelper osmAndAidlHelper;
    private PluginSettings settings;
    private PluginState pluginState;
    private SensorHelper sensorHelper;
    private LockHelper lockHelper;
    private OnMessageListener unlockMessageListener;

    private LayoutInflater inflater;

    interface PluginState {
        void createUI();

        void refreshUI();
    }

    /*private PluginState NO_INSTALLED_OSMAND_STATE = new PluginState() {
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
    };*/

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
            flPanelPluginEnable = findViewById(R.id.flPluginEnabled);
            tvPluginStateDescription = findViewById(R.id.tvPluginStateDescription);
            btnOpenOsmand = findViewById(R.id.btnOpenOsmand);
            flPanelTime = findViewById(R.id.flPanelTime);
            tvTime = findViewById(R.id.tvTime);
            flPanelSensor = findViewById(R.id.flPanelSensor);
            swSensorEnableSwitcher = findViewById(R.id.swSensorEnableSwitcher);

            if (availableOsmandVersions.size() > 1) {
                llElementsList.addView(inflater.inflate(R.layout.main_el_osmand_versions, null, false));
                llOsmandVersions = findViewById(R.id.llOsmandVersions);
                prepareOsmandVersionsPanel();
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
                            Intent intent = new Intent(MainActivity.this, PluginDescriptionActivity.class);
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

            flPanelPluginEnable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swPluginEnableSwitcher.setChecked(!swPluginEnableSwitcher.isChecked());
                }
            });

            flPanelTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PluginStandardDialog dialog = new PluginStandardDialog(MainActivity.this) {

                        @Override
                        public ViewGroup prepareElements() {
                            LinearLayout llTimeList = new LinearLayout(MainActivity.this);
                            llTimeList.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            llTimeList.setOrientation(LinearLayout.VERTICAL);

                            PluginSettings.UnlockTime[] unlockTimes = PluginSettings.UnlockTime.values();

                            for (final PluginSettings.UnlockTime unlockTime : unlockTimes) {
                                final View timeViewListItem = inflater.inflate(R.layout.item_time, null, false);
                                timeViewListItem.setId(unlockTime.getId());

                                TextView tvTime = timeViewListItem.findViewById(R.id.tvTime);
                                final RadioButton rbTime = timeViewListItem.findViewById(R.id.rbTime);

                                View.OnClickListener listener = new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        int id = v.getId();

                                        rbTime.setChecked(true);

                                        PluginSettings.UnlockTime checkedTime = PluginSettings.UnlockTime.getTimeById(unlockTime.getId());
                                        int timeLikeSeconds = checkedTime != null ? checkedTime.getSeconds() : 0;

                                        settings.setTimeLikeSeconds(timeLikeSeconds);

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                closeDialog();
                                                refreshUI();
                                            }
                                        }, 5);
                                    }
                                };

                                rbTime.setOnClickListener(listener);

                                tvTime.setText(String.valueOf(unlockTime.getSeconds()));

                                timeViewListItem.setOnClickListener(listener);

                                llTimeList.addView(timeViewListItem);
                            }
                            return llTimeList;
                        }
                    };

                    dialog.setHeader(getString(R.string.select_time));
                    dialog.showDialog();
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

            flPanelSensor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swSensorEnableSwitcher.setChecked(!swSensorEnableSwitcher.isChecked());
                }
            });

            connectOsmand();
            btnOpenOsmandLayout.bringToFront();
        }

        @Override
        public void refreshUI() {
            boolean isPluginEnabled = settings.isPluginEnabled();

            if (isPluginEnabled) {
                setStatusBarColor(R.color.orange);

                osmAndAidlHelper.registerForVoiceRouterMessages();
                osmAndAidlHelper.addListener(unlockMessageListener);
                sensorHelper.addListener(unlockMessageListener);

                tvPluginStateDescription.setText(getString(R.string.enabled));
                tvPluginStateDescription.setTextColor(getResources().getColor(R.color.black));
                tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.orange));

                llPluginPreferencesLayout.setLayerType(LAYER_TYPE_HARDWARE, null);

                setEnableForElements(true);
            } else {
                setStatusBarColor(R.color.darkGrey);

                osmAndAidlHelper.unregisterFromVoiceRouterMessages();
                osmAndAidlHelper.removeListener(unlockMessageListener);
                sensorHelper.removeListener(unlockMessageListener);
                lockHelper.cancelLock();

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

            String time = String.valueOf(settings.getTimeLikeSeconds()) + " "
                    + getString(R.string.secondsShort);
            tvTime.setText(time);

            PluginSettings.OsmandVersion version = settings.getOsmandVersion();
            int checkedId = version.getId();
            FrameLayout checkedVersion = findViewById(checkedId);
            RadioButton rbVersion = checkedVersion.findViewById(R.id.rbVersion);
            if (rbVersion != null) {
                rbVersion.setChecked(true);
            }
        }

        public void prepareOsmandVersionsPanel() {
            for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                final ViewGroup container = (ViewGroup) inflater
                        .inflate(R.layout.item_osmand_version, null, false);

                container.setMinimumHeight((int) getResources().getDimension(R.dimen.basic_element_height));

                final FrameLayout flOsmandVersion = container.findViewById(R.id.flOsmandVersion);
                flOsmandVersion.setId(version.getId());
                flOsmandVersion.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int id = v.getId();
                        settings.setOsmandVersion(id);

                        RadioButton rb = flOsmandVersion.findViewById(R.id.rbVersion);
                        if (rb != null) {
                            rb.setChecked(true);
                        }

                        rb.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                settings.setOsmandVersion(id);

                                for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                                    if (id != version.getId()) {
                                        FrameLayout flVersion = findViewById(version.getId());
                                        RadioButton rbCurrent = flVersion.findViewById(R.id.rbVersion);
                                        rbCurrent.setChecked(false);
                                    }
                                }

                                connectOsmand();
                            }
                        });

                        for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                            if (id != version.getId()) {
                                FrameLayout flVersion = findViewById(version.getId());
                                RadioButton rbCurrent = flVersion.findViewById(R.id.rbVersion);
                                rbCurrent.setChecked(false);
                            }
                        }

                        connectOsmand();
                    }
                });

                ImageView ivVersionImg = container.findViewById(R.id.ivVersionImg);
                ivVersionImg.setImageResource(version.getImgResId());

                TextView tvVersionName = container.findViewById(R.id.tvVersionName);
                tvVersionName.setText(getString(version.getNameId()));

                llOsmandVersions.addView(container);
            }
        }

        private void throwRadioButtons(int checkedId, int[] other, ViewGroup container, int rbId) {
            for (int i = 0; i < other.length; i++) {
                if (checkedId != other[i]) {
                    ViewGroup item = container.findViewById(other[i]);
                    if (item != null) {
                        RadioButton rb = item.findViewById(rbId);
                        rb.setChecked(false);
                    }
                }
            }
        }

        private void setEnableForElements(boolean enable) {
            swSensorEnableSwitcher.setEnabled(enable);
            flPanelSensor.setEnabled(enable);
            for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
                FrameLayout flVersion = findViewById(version.getId());
                flVersion.setEnabled(enable);
                RadioButton rb = flVersion.findViewById(R.id.rbVersion);
                if (rb != null) {
                    rb.setEnabled(enable);
                }
            }
            flPanelTime.setEnabled(enable);
            tvTime.setEnabled(enable);
            btnOpenOsmand.setEnabled(enable);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (TurnScreenApp) getApplicationContext();

        osmAndAidlHelper = app.getOsmAndAidlHelper();
        settings = app.getSettings();
        sensorHelper = app.getSensorHelper();
        lockHelper = app.getLockHelper();
        unlockMessageListener = new UnlockMessageListener(app);

        inflater = getLayoutInflater();

        availableOsmandVersions = PluginSettings.OsmandVersion.getOnlyInstalledVersions();

        if (availableOsmandVersions == null || availableOsmandVersions.size() == 0) {
//            pluginState = NO_INSTALLED_OSMAND_STATE;
            startActivity(new Intent(this, OsmandInstallActivity.class));
        } else {
            pluginState = PLUGIN_PREFERENCE_STATE;
            //todo refactor
            settings.setOsmandVersion(availableOsmandVersions.get(0).getId());
        }

        if (!settings.programWasOpenedEarlier()) {
            settings.setOpened();
            Intent intent = new Intent(this, PluginDescriptionActivity.class);
            startActivity(intent);
        }

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

        if (pluginState != null) {
            pluginState.createUI();
        }
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

    private void connectOsmand() {
        Log.d("ttpl", "MainActivity: connecting to Osmand...");
        if (osmAndAidlHelper != null) {
            osmAndAidlHelper.reconnectOsmand();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (availableOsmandVersions == null || availableOsmandVersions.size() == 0) {
//            pluginState = NO_INSTALLED_OSMAND_STATE;
            startActivity(new Intent(this, OsmandInstallActivity.class));
        } else {
            pluginState = PLUGIN_PREFERENCE_STATE;
            //todo refactor
            settings.setOsmandVersion(availableOsmandVersions.get(0).getId());
        }

        refreshUI();
    }

    public void refreshUI() {
        if (pluginState != null) {
            pluginState.refreshUI();
        }
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
