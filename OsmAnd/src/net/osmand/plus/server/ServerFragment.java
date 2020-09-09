package net.osmand.plus.server;

import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import org.apache.commons.logging.Log;

import java.io.IOException;

import static android.content.Context.WIFI_SERVICE;

public class ServerFragment extends BaseOsmAndFragment {
	public static final String TAG = "ServerFrag";
	private final static Log LOG = PlatformUtil.getLog(ServerFragment.class);
	private final int port = 24990;
	final int THREAD_ID = 14231; // random number
	private boolean initialized = false;
	private OsmAndHttpServer server;
	private View view;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		enableStrictMode();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.server_fragment, container, false);
		view.findViewById(R.id.server_start_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!initialized) {
					updateTextView(getString(R.string.stop_web_server));
					initServer();
				}
			}
		});
		view.findViewById(R.id.server_stop_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateTextView(getString(R.string.start_web_server));
				deInitServer();
			}
		});
		return view;
	}

	@Override
	public void onDestroy() {
		deInitServer();
		super.onDestroy();
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
		((TextView) view.findViewById(R.id.server_status_textview)).setText(text);
	}

	private void initServer() {
		TrafficStats.setThreadStatsTag(THREAD_ID);
		String hostname = getDeviceAddress();
		try {
			server = new OsmAndHttpServer(hostname, port);
			server.start((MapActivity) getActivity());
			initialized = true;
			updateTextView("Server started at " + server.getUrl());
		} catch (IOException e) {
			Toast.makeText(requireContext(),
					e.getLocalizedMessage(),
					Toast.LENGTH_SHORT).show();
			LOG.error(e);
		}
	}

	private String getDeviceAddress() {
		WifiManager wm = (WifiManager) requireContext().getSystemService(WIFI_SERVICE);
		String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		return ip != null ? ip : "0.0.0.0";
	}

	private void deInitServer() {
		if (server != null) {
			server.closeAllConnections();
			server.stop();
		}
		initialized = false;
		if (getActivity() != null) {
			try {
				getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
			} catch (RuntimeException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}
}