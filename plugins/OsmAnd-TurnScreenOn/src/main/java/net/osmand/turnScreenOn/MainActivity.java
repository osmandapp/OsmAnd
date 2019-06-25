package net.osmand.turnScreenOn;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
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
import android.widget.TextView;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.dialog.PluginStandardDialog;
import net.osmand.turnScreenOn.helpers.AndroidUtils;
import net.osmand.turnScreenOn.helpers.LockHelper;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;
import net.osmand.turnScreenOn.helpers.RadioGroupWrapper;
import net.osmand.turnScreenOn.helpers.SensorHelper;
import net.osmand.turnScreenOn.listener.UnlockMessageListener;
import net.osmand.turnScreenOn.listener.OnMessageListener;

import java.util.List;

import static android.view.View.LAYER_TYPE_HARDWARE;

public class MainActivity extends AppCompatActivity {
    private TurnScreenApp app;
    private OsmAndAidlHelper osmAndAidlHelper;
    private PluginSettings settings;
    private SensorHelper sensorHelper;
    private LockHelper lockHelper;
    private OnMessageListener unlockMessageListener;

    private List<PluginSettings.OsmandVersion> availableOsmandVersions;

    private Toolbar tbPluginToolbar;
    private LinearLayout llElementsScreen;
    private FrameLayout flPanelPluginEnable;
    private SwitchCompat swPluginEnableSwitcher;
    private FrameLayout flPanelSensor;
    private SwitchCompat swSensorEnableSwitcher;
    private TextView tvPluginStateDescription;
    private FrameLayout flPanelTime;
    private TextView tvTime;
    private LinearLayout llPluginPreferencesLayout;
    private RadioGroupWrapper osmandVersionRadioGroup;
    private FrameLayout btnOpenOsmand;
    private ImageView ivTimeSetUpIcon;
    private ImageView ivSensorIcon;
    private View bottomSensorCardShadow;
    private View osmandVersionsPanel;

    private Paint pGreyScale;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (TurnScreenApp) getApplicationContext();
        osmAndAidlHelper = app.getOsmAndAidlHelper();
        settings = app.getSettings();
        sensorHelper = app.getSensorHelper();
        lockHelper = app.getLockHelper();
        unlockMessageListener = new UnlockMessageListener(app);
    }

    public void startProcess() {
        if (settings.isOpenedFirstTime()) {
            startPluginDescriptionActivity(PluginDescriptionActivity.MODE_FIRST_OPEN);
        } else if (!settings.hasAvailableOsmandVersions()) {
            startInstallOsmandInstructionActivity();
        }
        createUI();
    }

    public void createUI() {
        final LayoutInflater inflater = getLayoutInflater();

        pGreyScale = AndroidUtils.createPaintWithGreyScale();
        llElementsScreen = findViewById(R.id.llElementsScreen);

        tbPluginToolbar = findViewById(R.id.tbPluginToolbar);
        setSupportActionBar(tbPluginToolbar);

        llPluginPreferencesLayout = findViewById(R.id.llPluginPreferencesLayout);
        swPluginEnableSwitcher = findViewById(R.id.swPluginEnableSwitcher);
        flPanelPluginEnable = findViewById(R.id.flPluginEnabled);
        tvPluginStateDescription = findViewById(R.id.tvPluginStateDescription);
        btnOpenOsmand = findViewById(R.id.btnOpenOsmand);
        flPanelTime = findViewById(R.id.flPanelTime);
        tvTime = findViewById(R.id.tvTime);
        flPanelSensor = findViewById(R.id.flPanelSensor);
        swSensorEnableSwitcher = findViewById(R.id.swSensorEnableSwitcher);
        ivTimeSetUpIcon = findViewById(R.id.ivTimeSetUpIcon);
        ivSensorIcon = findViewById(R.id.ivSensorIcon);

        btnOpenOsmand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchIntent = getPackageManager()
                        .getLaunchIntentForPackage(settings.getOsmandVersion().getPath());
                if (launchIntent != null) {
                    startActivity(launchIntent);
                }
            }
        });

        swPluginEnableSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!settings.isAdminDevicePermissionAvailable()) {
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

        flPanelPluginEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swPluginEnableSwitcher.setChecked(!swPluginEnableSwitcher.isChecked());
            }
        });

        flPanelTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final RadioGroupWrapper unlockTimeRadioButtonsGroup = new RadioGroupWrapper();

                final PluginStandardDialog dialog = new PluginStandardDialog(MainActivity.this) {

                    @Override
                    public ViewGroup prepareElements() {
                        LinearLayout llTimeList = new LinearLayout(MainActivity.this);
                        llTimeList.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        llTimeList.setOrientation(LinearLayout.VERTICAL);

                        PluginSettings.UnlockTime chosenTime = settings.getTime();

                        PluginSettings.UnlockTime[] unlockTimes = PluginSettings.UnlockTime.values();

                        for (final PluginSettings.UnlockTime unlockTime : unlockTimes) {
                            final int currentTimeItemId = unlockTime.getId();
                            final View timeItemView = inflater.inflate(R.layout.item_time, null, false);
                            timeItemView.setId(currentTimeItemId);

                            TextView tvTime = timeItemView.findViewById(R.id.tvTime);
                            tvTime.setText(String.valueOf(unlockTime.getSeconds()));

                            RadioButton rb = timeItemView.findViewById(R.id.rbTime);
                            unlockTimeRadioButtonsGroup.addRadioButton(currentTimeItemId, rb);

                            View.OnClickListener listener = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    settings.setTime(currentTimeItemId);
                                    unlockTimeRadioButtonsGroup.setChecked(currentTimeItemId);

                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            closeDialog();
                                            refreshUI();
                                        }
                                    }, 5);
                                }
                            };

                            timeItemView.setOnClickListener(listener);
                            rb.setOnClickListener(listener);

                            if (chosenTime != null && currentTimeItemId == chosenTime.getId()) {
                                unlockTimeRadioButtonsGroup.setChecked(currentTimeItemId);
                            }

                            llTimeList.addView(timeItemView);
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
            startPluginDescriptionActivity(PluginDescriptionActivity.MODE_HELP);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void connectOsmand() {
        if (osmAndAidlHelper != null) {
            osmAndAidlHelper.reconnectOsmand();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startProcess();
        refreshUI();
    }

    public void refreshUI() {
        refreshOsmandVersionsPanel();

        boolean isPluginEnabled = settings.isPluginEnabled();

        if (isPluginEnabled) {
            setStatusBarColor(R.color.orange);

            lockHelper.enableFunction();

            osmAndAidlHelper.registerForVoiceRouterMessages();
            osmAndAidlHelper.addListener(unlockMessageListener);
            sensorHelper.addListener(unlockMessageListener);

            tvPluginStateDescription.setText(getString(R.string.enabled));
            tvPluginStateDescription.setTextColor(getResources().getColor(R.color.black));
            tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.orange));
            ivTimeSetUpIcon.setColorFilter(ContextCompat.getColor(this, R.color.blue),
                    android.graphics.PorterDuff.Mode.MULTIPLY);

            llPluginPreferencesLayout.setLayerType(LAYER_TYPE_HARDWARE, null);

            setEnableForElements(true);

        } else {
            setStatusBarColor(R.color.darkGrey);

            lockHelper.disableFunction();

            osmAndAidlHelper.unregisterFromVoiceRouterMessages();
            osmAndAidlHelper.removeListener(unlockMessageListener);
            sensorHelper.removeListener(unlockMessageListener);
            lockHelper.disableFunction();

            tvPluginStateDescription.setText(getString(R.string.disabled));
            tvPluginStateDescription.setTextColor(getResources().getColor(R.color.darkGrey));
            tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.darkGrey));
            ivTimeSetUpIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkGrey),
                    android.graphics.PorterDuff.Mode.MULTIPLY);

            llPluginPreferencesLayout.setLayerType(LAYER_TYPE_HARDWARE, pGreyScale);

            setEnableForElements(false);
        }

        swPluginEnableSwitcher.setChecked(isPluginEnabled);

        boolean isSensorEnabled = settings.isSensorEnabled();
        swSensorEnableSwitcher.setChecked(isSensorEnabled);

        if (isSensorEnabled) {
            sensorHelper.switchOnSensor();
            ivSensorIcon.setColorFilter(ContextCompat.getColor(this, R.color.blue),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            sensorHelper.switchOffSensor();
            ivSensorIcon.setColorFilter(ContextCompat.getColor(this, R.color.darkGrey),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        }

        PluginSettings.UnlockTime currentTime = settings.getTime();
        String time = String.valueOf(currentTime.getSeconds()) + " "
                + getString(R.string.secondsShort);
        tvTime.setText(time);
    }

    public void refreshOsmandVersionsPanel() {
        availableOsmandVersions = PluginSettings.OsmandVersion.getOnlyInstalledVersions();
        llElementsScreen.removeView(osmandVersionsPanel);

        if (bottomSensorCardShadow==null) {
            bottomSensorCardShadow = getLayoutInflater().inflate(R.layout.card_top_divider, null, false);
            llElementsScreen.addView(bottomSensorCardShadow);
        }

        if (availableOsmandVersions.size() > 1) {
            llElementsScreen.removeView(bottomSensorCardShadow);
            bottomSensorCardShadow = null;
            osmandVersionsPanel = createOsmandVersionsPanel();
            llElementsScreen.addView(osmandVersionsPanel);

            PluginSettings.OsmandVersion version = settings.getOsmandVersion();
            int checkedId = version.getId();
            FrameLayout checkedVersion = findViewById(checkedId);
            if (checkedVersion != null) {
                RadioButton rbVersion = checkedVersion.findViewById(R.id.rbVersion);
                if (rbVersion != null) {
                    rbVersion.setChecked(true);
                }
            }
        }
    }

    private View createOsmandVersionsPanel() {
        LayoutInflater inflater = getLayoutInflater();

        LinearLayout llWrapper = new LinearLayout(MainActivity.this);
        llWrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        llWrapper.setOrientation(LinearLayout.VERTICAL);

        View panelOsmandVersions = inflater.inflate(R.layout.main_el_osmand_versions, null, false);
        LinearLayout llOsmandVersions = panelOsmandVersions.findViewById(R.id.llOsmandVersions);

        osmandVersionRadioGroup = new RadioGroupWrapper();

        for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
            final int currentVersionId = version.getId();

            final ViewGroup osmandVersionItemView = (ViewGroup) inflater
                    .inflate(R.layout.item_osmand_version, null, false);

            osmandVersionItemView.setId(currentVersionId);

            ImageView ivVersionImg = osmandVersionItemView.findViewById(R.id.ivVersionImg);
            ivVersionImg.setImageResource(version.getImgResId());

            TextView tvVersionName = osmandVersionItemView.findViewById(R.id.tvVersionName);
            tvVersionName.setText(getString(version.getNameId()));

            RadioButton rb = osmandVersionItemView.findViewById(R.id.rbVersion);
            osmandVersionRadioGroup.addRadioButton(currentVersionId, rb);

            View.OnClickListener onRadioCheckedListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settings.setOsmandVersion(currentVersionId);
                    osmandVersionRadioGroup.setChecked(currentVersionId);
                    connectOsmand();
                }
            };

            osmandVersionItemView.setOnClickListener(onRadioCheckedListener);
            rb.setOnClickListener(onRadioCheckedListener);

            llOsmandVersions.addView(osmandVersionItemView);
        }

        View cardDevider = inflater.inflate(R.layout.card_devider, null, false);
        View cardTop = inflater.inflate(R.layout.card_top_divider, null, false);

        llWrapper.addView(cardDevider);
        llWrapper.addView(panelOsmandVersions);
        llWrapper.addView(cardTop);

        return llWrapper;
    }

    private void setEnableForElements(boolean enable) {
        swSensorEnableSwitcher.setEnabled(enable);
        flPanelSensor.setEnabled(enable);
        for (PluginSettings.OsmandVersion version : availableOsmandVersions) {
            int versionId = version.getId();
            View vOsmandVersionItem = findViewById(versionId);
            if (vOsmandVersionItem != null) {
                vOsmandVersionItem.setEnabled(enable);
                RadioButton rb = vOsmandVersionItem.findViewById(R.id.rbVersion);
                if (rb != null) {
                    rb.setEnabled(enable);
                }
            }
        }
        flPanelTime.setEnabled(enable);
        tvTime.setEnabled(enable);
        btnOpenOsmand.setEnabled(enable);
    }

    private void setStatusBarColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, colorResId));
        }
    }

    private void startInstallOsmandInstructionActivity() {
        startSimpleActivity(OsmandInstallActivity.class);
    }

    private void startPluginDescriptionActivity(int mode) {
        Intent intent = new Intent(this, PluginDescriptionActivity.class);
        intent.putExtra(PluginDescriptionActivity.MODE_KEY, mode);
        startActivity(intent);
    }

    private void startSimpleActivity(Class destinationActivityClass) {
        Intent intent = new Intent(this, destinationActivityClass);
        startActivity(intent);
    }
}
