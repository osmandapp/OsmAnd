package net.osmand.osmandapitestapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

	public static final int REQUEST_OSMAND_API = 101;

	public static final String API_CMD_SHOW_GPX = "show_gpx";
	public static final String API_CMD_NAVIGATE_GPX = "navigate_gpx";

	public static final String API_CMD_NAVIGATE = "navigate";

	public static final String API_CMD_RECORD_AUDIO = "record_audio";
	public static final String API_CMD_RECORD_VIDEO = "record_video";
	public static final String API_CMD_RECORD_PHOTO = "record_photo";
	public static final String API_CMD_STOP_AV_REC = "stop_av_rec";

	public static final String API_CMD_GET_INFO = "get_info";

	public static final String API_CMD_ADD_FAVORITE = "add_favorite";
	public static final String API_CMD_ADD_MAP_MARKER = "add_map_marker";

	public static final String API_CMD_START_GPX_REC = "start_gpx_rec";
	public static final String API_CMD_STOP_GPX_REC = "stop_gpx_rec";

	public static final String API_CMD_SUBSCRIBE_VOICE_NOTIFICATIONS = "subscribe_voice_notifications";

	public static final String PARAM_NAME = "name";
	public static final String PARAM_DESC = "desc";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_LAT = "lat";
	public static final String PARAM_LON = "lon";
	public static final String PARAM_COLOR = "color";
	public static final String PARAM_VISIBLE = "visible";

	public static final String PARAM_PATH = "path";
	public static final String PARAM_DATA = "data";
	public static final String PARAM_FORCE = "force";

	public static final String PARAM_START_NAME = "start_name";
	public static final String PARAM_DEST_NAME = "dest_name";
	public static final String PARAM_START_LAT = "start_lat";
	public static final String PARAM_START_LON = "start_lon";
	public static final String PARAM_DEST_LAT = "dest_lat";
	public static final String PARAM_DEST_LON = "dest_lon";
	public static final String PARAM_PROFILE = "profile";

	public static final String PARAM_ETA = "eta";
	public static final String PARAM_TIME_LEFT = "time_left";
	public static final String PARAM_DISTANCE_LEFT = "time_distance_left";

	public static final int RESULT_CODE_OK = 0;
	public static final int RESULT_CODE_ERROR_UNKNOWN = -1;
	public static final int RESULT_CODE_ERROR_NOT_IMPLEMENTED = -2;
	public static final int RESULT_CODE_ERROR_PLUGIN_INACTIVE = 10;
	public static final int RESULT_CODE_ERROR_GPX_NOT_FOUND = 20;
	public static final int RESULT_CODE_ERROR_INVALID_PROFILE = 30;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Button btn = (Button) findViewById(R.id.btn_add_favorite);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_ADD_FAVORITE);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_add_map_marker);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_ADD_MAP_MARKER);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_start_audio_rec);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_RECORD_AUDIO);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_start_video_rec);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_RECORD_VIDEO);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_stop_rec);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_STOP_AV_REC);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_take_photo);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_RECORD_PHOTO);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_start_gpx_rec);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_START_GPX_REC);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_stop_gpx_rec);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_STOP_GPX_REC);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_show_gpx);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_SHOW_GPX);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_navigate_gpx);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_NAVIGATE_GPX);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_navigate);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_NAVIGATE);
				}
			});
		}

		btn = (Button) findViewById(R.id.btn_get_info);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exec(API_CMD_GET_INFO);
				}
			});
		}
	}

	public void exec(String command) {
		Uri uri = null;
		Intent intent = null;

		String lat = "44.98062";
		String lon = "34.09258";
		String destLat = "44.97799";
		String destLon = "34.10286";
		String gpxName = "xxx.gpx";

		try {

			if (API_CMD_GET_INFO.equals(command)) {
				uri = Uri.parse("osmand.api://get_info");
			}

			if (API_CMD_NAVIGATE.equals(command)) {
				// test navigate
				uri = Uri.parse("osmand.api://navigate" +
						"?start_lat=" + lat + "&start_lon=" + lon + "&start_name=Start" +
						"&dest_lat=" + destLat + "&dest_lon=" + destLon + "&dest_name=Finish" +
						"&profile=bicycle");
			}

			if (API_CMD_RECORD_AUDIO.equals(command)) {
				// test record audio
				uri = Uri.parse("osmand.api://record_audio?lat=" + lat + "&lon=" + lon);
			}
			if (API_CMD_RECORD_VIDEO.equals(command)) {
				// test record video
				uri = Uri.parse("osmand.api://record_video?lat=" + lat + "&lon=" + lon);
			}
			if (API_CMD_RECORD_PHOTO.equals(command)) {
				// test take photo
				uri = Uri.parse("osmand.api://record_photo?lat=" + lat + "&lon=" + lon);
			}
			if (API_CMD_STOP_AV_REC.equals(command)) {
				// test record video
				uri = Uri.parse("osmand.api://stop_av_rec");
			}

			if (API_CMD_ADD_MAP_MARKER.equals(command)) {
				// test marker
				uri = Uri.parse("osmand.api://add_map_marker?lat=" + lat + "&lon=" + lon + "&name=Marker");
			}

			if (API_CMD_ADD_FAVORITE.equals(command)) {
				// test favorite
				uri = Uri.parse("osmand.api://add_favorite?lat=" + lat + "&lon=" + lon + "&name=Favorite&desc=Description&category=test2&color=red&visible=true");
			}

			if (API_CMD_START_GPX_REC.equals(command)) {
				// test start gpx recording
				uri = Uri.parse("osmand.api://start_gpx_rec");
			}

			if (API_CMD_STOP_GPX_REC.equals(command)) {
				// test stop gpx recording
				uri = Uri.parse("osmand.api://stop_gpx_rec");
			}

			if (API_CMD_SHOW_GPX.equals(command)) {
				// test show gpx (path)
				//File gpx = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName);
				//uri = Uri.parse("osmand.api://show_gpx?path=" + URLEncoder.encode(gpx.getAbsolutePath(), "UTF-8"));

				// test show gpx (data)
				uri = Uri.parse("osmand.api://show_gpx");
				intent = new Intent(Intent.ACTION_VIEW, uri);
				//intent.putExtra("data", AndroidUtils.getFileAsString(
				//		new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName)));
			}

			if (API_CMD_NAVIGATE_GPX.equals(command)) {
				// test navigate gpx (path)
				//File gpx = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName);
				//uri = Uri.parse("osmand.api://navigate_gpx?force=true&path=" + URLEncoder.encode(gpx.getAbsolutePath(), "UTF-8"));

				// test navigate gpx (data)
				uri = Uri.parse("osmand.api://navigate_gpx?force=true");
				intent = new Intent(Intent.ACTION_VIEW, uri);
				//intent.putExtra("data", AndroidUtils.getFileAsString(
				//		new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxName)));
			}

			if (intent == null && uri != null) {
				intent = new Intent(Intent.ACTION_VIEW, uri);
			}

			if (intent != null) {
				startActivityForResult(intent, REQUEST_OSMAND_API);
				/*
				// setup the Intent to deep link into Android Market
				Uri marketUri = Uri.parse("market://search?q=pname:net.osmand");
				Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);

				PackageManager pm = getPackageManager();
				startActivity(pm.queryIntentActivities(intent, 0).size() == 0 ?
						intent : marketIntent);
				*/
			}

		} catch (Exception e) {
			Log.e("Osmand API", "Error", e);
		}
	}

	/*
		Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
			.setAction("Action", null).show();
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_OSMAND_API) {
			View view = findViewById(R.id.main_view);
			if (view != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("ResultCode=").append(resultCodeStr(resultCode));
				Bundle extras = data.getExtras();
				if (extras != null && extras.size() > 0) {
					for (String key : data.getExtras().keySet()) {
						Object val = extras.get(key);
						if (sb.length() > 0) {
							sb.append("\n");
						}
						sb.append(key).append("=").append(val);
					}
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(sb.toString());
				builder.setPositiveButton("OK", null);
				builder.create().show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private String resultCodeStr(int resultCode) {
		switch (resultCode) {
			case RESULT_CODE_OK:
				return "OK";
			case RESULT_CODE_ERROR_UNKNOWN:
				return "Unknown error";
			case RESULT_CODE_ERROR_NOT_IMPLEMENTED:
				return "Feature is not implemented";
			case RESULT_CODE_ERROR_GPX_NOT_FOUND:
				return "GPX not found";
			case RESULT_CODE_ERROR_INVALID_PROFILE:
				return "Invalid profile";
			case RESULT_CODE_ERROR_PLUGIN_INACTIVE:
				return "Plugin inactive";
		}
		return "" + resultCode;
	}
}
