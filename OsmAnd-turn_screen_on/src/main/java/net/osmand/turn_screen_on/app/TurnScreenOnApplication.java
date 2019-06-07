package net.osmand.turn_screen_on.app;

import android.app.Application;
import android.content.Context;

public class TurnScreenOnApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        this.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
