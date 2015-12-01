package btools.routingapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BRouterServiceConnection implements ServiceConnection {
		IBRouterService brouterService;

		public void onServiceConnected(ComponentName className, IBinder boundService) {
			brouterService = IBRouterService.Stub.asInterface(boundService);
		}

		public void onServiceDisconnected(ComponentName className) {
			brouterService = null;
		}
		
		public void disconnect(Context ctx){
			ctx.unbindService(this);
		}
		
		public IBRouterService getBrouterService() {
			return brouterService;
		}
		
		public static BRouterServiceConnection connect(Context ctx){
			BRouterServiceConnection conn = new BRouterServiceConnection();
			Intent intent = new Intent();
			intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService");
			boolean hasBRouter = ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE);
			if(!hasBRouter){
				conn = null;
			}
			return conn;
		}
	}