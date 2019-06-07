package net.osmand.turn_screen_on;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.turn_screen_on.helpers.LockHelper;
import net.osmand.turn_screen_on.helpers.OsmAndAidlHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Toolbar tbPluginToolbar;
    private FrameLayout btnOpenOsmand;
    private SwitchCompat swFunctionSwitcher;
    private TextView tvPluginStateDescription;
    private Spinner spTime;

    //todo delete
    private Button btnRegister;

    private static int[] timeBySeconds = {5, 10, 15};

    private OsmAndAidlHelper osmAndAidlHelper;
    private PluginSettings settings;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        osmAndAidlHelper = OsmAndAidlHelper.getInstance();
        settings = PluginSettings.getInstance();

        tbPluginToolbar = findViewById(R.id.tbPluginToolbar);
        btnOpenOsmand = findViewById(R.id.btnOpenOsmand);
        swFunctionSwitcher = findViewById(R.id.swFunctionSwitcher);
        tvPluginStateDescription = findViewById(R.id.tvPluginStateDescription);
        spTime = findViewById(R.id.spTime);

        btnOpenOsmand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                osmAndAidlHelper.show();
            }
        });

        swFunctionSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent intent = new Intent(MainActivity.this, AdminDevicePermissionSetUpActivity.class);
                    startActivity(intent);
                } else {
                    settings.disablePlugin();
                }
                refreshView();
            }
        });

        List<String> spinnerArray = new ArrayList<>();
        for (int t : timeBySeconds) {
            spinnerArray.add(new StringBuilder().append(t).append(" ")
                    .append(getString(R.string.secondsShort)).toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_spinner_item,
                spinnerArray
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spTime.setAdapter(adapter);
        spTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                                       View itemSelected, int selectedItemPosition, long selectedId) {

                int time = timeBySeconds[selectedItemPosition];
                settings.setTimeLikeSeconds(time);
                settings.setTimePosition(selectedItemPosition);
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //todo delete
        btnRegister = findViewById(R.id.btnRegister);
        /*btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (settings.isPluginEnabled()) {
                    Handler handler = new Handler();
                    final LockHelper lockHelper = new LockHelper(MainActivity.this);
                    lockHelper.lock();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            lockHelper.timedUnlock(5000);
                        }
                    }, 5000);
                }
            }
        });*/
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                osmAndAidlHelper.register();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshView();
    }

    public void refreshView() {
        //set up status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (settings.isPluginEnabled()) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.orange));
            } else {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.darkGrey));
            }
        }

        boolean isPluginEnabled = settings.isPluginEnabled();

        if (isPluginEnabled) {
            tvPluginStateDescription.setText(getString(R.string.enabled));
            tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.orange));
        } else {
            tvPluginStateDescription.setText(getString(R.string.disabled));
            tbPluginToolbar.setBackgroundColor(getResources().getColor(R.color.darkGrey));
        }
        swFunctionSwitcher.setChecked(isPluginEnabled);

        int timePosition = settings.getTimePosition();
        spTime.setSelection(timePosition);
    }
}
