package com.anvisics.battery;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.format.DateFormat;

public class BatteryLogService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	

	private BatteryLogBinder binder = new BatteryLogBinder();
	private BatteryStatusReceiver batteryStatusReceiver;
	
	
	public static class BatteryLogEntry {
		private int batteryLevel;
		private int batteryVoltage;
		private long time;
		private int plugged;
		private int status;
		
		
		public BatteryLogEntry(){
			time = System.currentTimeMillis();
		}
		
		public int getPlugged() {
			return plugged;
		}
		public void setPlugged(int plugged) {
			this.plugged = plugged;
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public int getBatteryLevel() {
			return batteryLevel;
		}
		public void setBatteryLevel(int batteryLevel) {
			this.batteryLevel = batteryLevel;
		}
		public int getBatteryVoltage() {
			return batteryVoltage;
		}
		public void setBatteryVoltage(int batteryVoltage) {
			this.batteryVoltage = batteryVoltage;
		}
		public long getTime() {
			return time;
		}
		
		public boolean sameMeasurements(BatteryLogEntry e ){
			return batteryLevel == e.batteryLevel && 
				batteryVoltage == e.batteryVoltage &&
				status == e.status &&
				plugged == e.plugged;
		}
		
		public String getMessage(){
			String statusS = "";
			switch(status){
			case BatteryManager.BATTERY_STATUS_CHARGING : statusS = "CHARGING";
			case BatteryManager.BATTERY_STATUS_DISCHARGING: statusS = "DISCHARGING";
			case BatteryManager.BATTERY_STATUS_FULL: statusS = "FULL";
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING: statusS = "NOT_CHARGING";
			}
			String pluggedS = "";
			switch(plugged){
			case BatteryManager.BATTERY_PLUGGED_AC: pluggedS = "PLUGGED_AC";
			case BatteryManager.BATTERY_PLUGGED_USB: pluggedS = "PLUGGED_USB";
			}
			CharSequence timeS = DateFormat.format("MM/dd/yy k:mm",this.time);
			return MessageFormat.format("{0} : battery ({1}), voltage ({2}), plugged ({3}), status ({4})", 
					timeS, batteryLevel, batteryVoltage, pluggedS, statusS);
		}
		
		@Override
		public String toString() {
			return getMessage();
		}
		
	}
	
	public static class BatteryLogBinder extends Binder {
		
		private List<BatteryLogEntry> entries = new ArrayList<BatteryLogEntry>();
		
		
		public boolean addEntry(BatteryLogEntry e){
			if(entries.isEmpty()){
				entries.add(e);
				return true;
			}
			BatteryLogEntry last = entries.get(entries.size() - 1);
			if(!last.sameMeasurements(e) ||  e.getTime() - last.getTime() > 60000){
				entries.add(e);
				return true;
			}
			return false;
		}
		
		
		public void clearEntries(){
			entries.clear();
		}
		public List<BatteryLogEntry> getEntries() {
			return entries;
		}
	}

	
	@Override
	public void onCreate() {
		super.onCreate();
        
        batteryStatusReceiver = new BatteryStatusReceiver(this);
        registerReceiver(batteryStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        receiveMessage(100, 0, 0, 1);
	}
	
	public void receiveMessage(int voltage, int level, int plugged, int status){
		BatteryLogEntry entry = new BatteryLogEntry();
		entry.setBatteryVoltage(voltage);
		entry.setBatteryLevel(level);
		entry.setPlugged(plugged);
		entry.setStatus(status);
		binder.addEntry(entry);
	}
	
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(batteryStatusReceiver);
	}

}
