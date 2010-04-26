package com.anvisics.battery;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;

import com.anvisics.battery.BatteryLogService.BatteryLogBinder;
import com.anvisics.battery.BatteryLogService.BatteryLogEntry;

public class BatteryViewActivity extends Activity {
    private Button startServiceButton;
	private Button stopServiceButton;
	private GridView gridView;
	
	
	private final Intent serviceIntent = new Intent("com.anvisics.BatteryLogService");

	ServiceConnection serviceConnection = null;
	private ArrayAdapter<BatteryLogEntry> gridViewAdapter;
	private BatteryLogBinder binder = null;
	
	private class MyServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(service instanceof BatteryLogBinder){
				gridViewAdapter.clear();
				binder = (BatteryLogBinder) service;
				List<BatteryLogEntry> entries = ((BatteryLogBinder) service).getEntries();
				for(int i = entries.size() - 1; i>=0; i--){
					gridViewAdapter.add(entries.get(i));
				}
			}
		}
		

		@Override
		public void onServiceDisconnected(ComponentName name) {
			gridViewAdapter.clear();
			binder = null;
		}
    	
    };

	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        startServiceButton = (Button)findViewById(R.id.Button01);
        stopServiceButton = (Button)findViewById(R.id.Button02);
        
        
        
        gridView = (GridView) findViewById(R.id.GridView01);
        
        gridView.setNumColumns(1);
        gridView.setVerticalSpacing(3);
        gridViewAdapter = new ArrayAdapter<BatteryLogEntry>(getWindow().getContext(), R.layout.mytext);
        gridView.setAdapter(gridViewAdapter);
        
        
        startServiceButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ComponentName name = getWindow().getContext().startService(serviceIntent);
				if(name != null){
					stopServiceButton.setEnabled(true);
					startServiceButton.setEnabled(false);
				}
			}
        });
        
        stopServiceButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean stopService = getWindow().getContext().stopService(serviceIntent);
				if(stopService){
					stopServiceButton.setEnabled(false);
					startServiceButton.setEnabled(true);
				}
			}
        });
        
        ComponentName componentName = getWindow().getContext().startService(serviceIntent);
        startServiceButton.setEnabled(componentName == null);
        stopServiceButton.setEnabled(componentName != null);
        serviceConnection = new MyServiceConnection();
        
        
        getWindow().getContext().bindService(serviceIntent, serviceConnection, 0);
        
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId() == R.id.Clear){
    		if(binder != null){
    			binder.clearEntries();
        		gridViewAdapter.clear();
    		}
    		return true;
    	} else if (item.getItemId() == R.id.Export) {
			if (binder == null) {
				return true;
			}
			File directory = Environment.getExternalStorageDirectory();
			if (directory.canWrite()) {
				File f = new File(directory, "BatteryLog.txt");
				int i = 1;
				while (f.exists()) {
					f = new File(directory, "BatteryLog" + (++i) + ".txt");
				}

				try {
					BufferedWriter writer = new BufferedWriter(
							new FileWriter(f));
					List<BatteryLogEntry> entries = binder.getEntries();
					for (i = entries.size() - 1; i >= 0; i--) {
						writer.write(entries.get(i).toString());
					}
					writer.close();
					binder.clearEntries();
					gridViewAdapter.clear();
				} catch (IOException e) {
					Log.e("batteryLog", "Can't export file", e);
				}
			}
		} else if(item.getItemId() == R.id.Exit){
    		finish();
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }



    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if(serviceConnection != null){
    		getWindow().getContext().unbindService(serviceConnection);
    		serviceConnection = null;
    	}
    }
    
         
}