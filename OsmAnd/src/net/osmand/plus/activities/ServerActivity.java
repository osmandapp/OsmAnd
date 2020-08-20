package net.osmand.plus.activities;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.R;
import net.osmand.plus.server.OsmAndHttpServer;

import java.io.IOException;

public class ServerActivity extends AppCompatActivity {
	private boolean initialized = false;
	private OsmAndHttpServer server;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		enableStrictMode();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_activity);
		findViewById(R.id.Button01).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!initialized) {
					updateTextView("Click second button to deactivate server");
					initServer();
				}
			}
		});
		findViewById(R.id.Button03).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (initialized) {
					updateTextView("Click first button to activate server");
					deInitServer();
				}
			}
		});
	}

	public static void enableStrictMode() {
		StrictMode.setThreadPolicy(
				new StrictMode.ThreadPolicy.Builder()
						.detectDiskReads()
						.detectDiskWrites()
						.detectNetwork()
						.penaltyLog()
						.build());
		StrictMode.setVmPolicy(
				new StrictMode.VmPolicy.Builder()
						.detectLeakedSqlLiteObjects()
						.penaltyLog()
						.build());
	}


	private void updateTextView(String text) {
		((TextView) findViewById(R.id.TextView02)).setText(text);
	}

	private void initServer() {
		final int THREAD_ID = 10000;
		TrafficStats.setThreadStatsTag(THREAD_ID);
		try {
			server = new OsmAndHttpServer();
			initialized = true;
			updateTextView("Server started at: http://" + OsmAndHttpServer.HOSTNAME
                    + ":" + OsmAndHttpServer.PORT);
		} catch (IOException e) {
			Toast.makeText(this,
					e.getLocalizedMessage(),
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	private void deInitServer() {
		server.closeAllConnections();
		server.stop();
		initialized = false;
	}

	@Override
	protected void onDestroy() {
		deInitServer();
		super.onDestroy();
	}
}
