package com.anvisics.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BatteryStatusReceiver extends BroadcastReceiver {

	private final BatteryLogService service;

	public BatteryStatusReceiver(BatteryLogService service){
		this.service = service;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		service.receiveMessage(intent.getIntExtra("voltage", -1), intent.getIntExtra("level", -1), 
				intent.getIntExtra("plugged", -1), intent.getIntExtra("status", -1));
		
	}

}
