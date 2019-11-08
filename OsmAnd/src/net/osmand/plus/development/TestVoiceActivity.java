package net.osmand.plus.development;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.routing.data.StreetName;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Test Voice activity
 */
public class TestVoiceActivity extends OsmandActionBarActivity {

	private String osmandVoice ="";
	private String osmandVoiceLang ="";
	private Button infoButton;

	@Override
	public void onCreate(Bundle icicle) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		}
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		final OsmandApplication app = ((OsmandApplication) getApplication());
		
		
		LinearLayout gl = new LinearLayout(this);
		gl.setOrientation(LinearLayout.VERTICAL);
		gl.setPadding(3, 3, 3, 3);
		
		TextView tv = new TextView(this);
		tv.setText(R.string.test_voice_desrc);
		tv.setPadding(0, 5, 0, 7);
		
		ScrollView sv = new ScrollView(this);
		gl.addView(sv, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, 
				android.view.ViewGroup.LayoutParams.FILL_PARENT));
		final LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		sv.addView(ll, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, 
				android.view.ViewGroup.LayoutParams.FILL_PARENT));
		
		// add buttons
		setContentView(gl);
		getSupportActionBar().setTitle(R.string.test_voice_prompts);
		
		selectVoice(ll);
	}

	private void selectVoice(final LinearLayout ll) {
		String[] entries;
		final String[] entrieValues;
		Set<String> voiceFiles = getMyApplication().getRoutingOptionsHelper().getVoiceFiles(this);
		entries = new String[voiceFiles.size() ];
		entrieValues = new String[voiceFiles.size() ];
		int k = 0;
		int selected = 0;
		for (String s : voiceFiles) {
			entries[k] = s;
			entrieValues[k] = s;
			if(s.equals(((OsmandApplication) getApplication()).getSettings().VOICE_PROVIDER.get())) {
				selected = k;
			}
			k++;
		}
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setSingleChoiceItems(entrieValues, selected, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, final int which) {
				final OsmandApplication app = (OsmandApplication) getApplication();
				getSupportActionBar().setTitle(app.getString(R.string.test_voice_prompts) + " (" + entrieValues[which] + ")");
				app.getSettings().VOICE_PROVIDER.set(entrieValues[which]);
				app.initVoiceCommandPlayer(TestVoiceActivity.this,
						app.getSettings().APPLICATION_MODE.get(), true, new Runnable() {
					
					@Override
					public void run() {
						CommandPlayer p = app.getRoutingHelper().getVoiceRouter().getPlayer();
						if (p == null) {
							Toast.makeText(TestVoiceActivity.this, "Voice player not initialized", Toast.LENGTH_SHORT).show();
						} else {
							osmandVoice = entrieValues[which];
							osmandVoiceLang = p.getLanguage();
							addButtons(ll, p);
						}
					}
				}, true, true);
				dialog.dismiss();
			}
		});
		bld.show();
	}

	private String getVoiceSystemInfo() {
		String v ="";
		v += " \u25CF App profile: " + ((OsmandApplication) getApplication()).getSettings().APPLICATION_MODE.get().getStringKey();

		if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 3) {
			v += "\n \u25CF Voice guidance output: Media/music audio";
		} else if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 5) {
			v += "\n \u25CF Voice guidance output: Notification audio";
		} else if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 0) {
			v += "\n \u25CF Voice guidance output: Phone call audio";
		} else {
			v += "\n \u25CF Voice guidance output: " + ((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get();
		}

		v += "\n \u25CF OsmAnd voice: " + osmandVoice;
		v += "\n \u25CF OsmAnd voice language: " + osmandVoiceLang;

		v += "\n \u25CF TTS voice language availability: " + TTSCommandPlayerImpl.getTtsVoiceStatus();
		v += "\n \u25CF TTS voice actually used: " + TTSCommandPlayerImpl.getTtsVoiceUsed();

		if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 0) {
			v += "\n \u25CF BT SCO: " + AbstractPrologCommandPlayer.btScoInit;
		} else {
			v += "\n \u25CF BT SCO: The current app profile is not set to use 'Phone call audio'.";
		}

		v += "\n \u25CF Phone call audio delay: " + ((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() + "\u00A0ms";
		return v;
	}

	private void addButtons(final LinearLayout ll, CommandPlayer p) {
		addButton(ll, "Route calculated and number tests:", builder(p));
		addButton(ll, "\u25BA (1.1)  New route calculated, 150m, 230sec (00:03:50)", builder(p).newRouteCalculated(150, 230));
		addButton(ll, "\u25BA (1.2)  New route calculated, 1350m, 3680sec (01:01:20)", builder(p).newRouteCalculated(1350, 3680));
		addButton(ll, "\u25BA (1.3)  New route calculated 3700m, 7320sec (02:02)", builder(p).newRouteCalculated(3700, 7320));
		addButton(ll, "\u25BA (1.4)  New route calculated 9100m, 10980sec (03:03)", builder(p).newRouteCalculated(9100, 10980));
		addButton(ll, "\u25BA (1.5)  New route calculated, 1500m, 4280sec (01:20:20)", builder(p).newRouteCalculated(1500, 4280));
		addButton(ll, "\u25BA (2.1)  Route recalculated 11500m, 18600sec (05:10)", builder(p).routeRecalculated(11500, 18600));
		addButton(ll, "\u25BA (2.2)  Route recalculated 19633m, 26700sec (07:25)", builder(p).routeRecalculated(19633, 26700) );
		addButton(ll, "\u25BA (2.3)  Route recalculated 89750m, 55800sec (15:30)", builder(p).routeRecalculated(89750, 55800) );
		addButton(ll, "\u25BA (2.4)  Route recalculated 125900m, 92700sec (25:45)", builder(p).routeRecalculated(125900, 92700) );

		addButton(ll, "All turn types: prepareTurn, makeTurnIn, turn:", builder(p));
		addButton(ll, "\u25BA (3.1)  After 1520m turn slightly left", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_SL, 1520, street(p, "")));
		addButton(ll, "\u25BA (3.2)  In 450m turn sharply left onto 'Hauptstra"+"\u00df"+"e', then bear right", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_SH, 450, street(p, "Hauptstraße")).then().bearRight(street(p, "")));
		addButton(ll, "\u25BA (3.3)  Turn left, then in 100m turn slightly right", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT, street(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 100, street(p, "")));
		addButton(ll, "\u25BA (3.4)  After 3100m turn right onto 'SR 80' toward 'Rome'", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT, 3100, street(p,  "SR 80", "", "Rome")));
		addButton(ll, "\u25BA (3.5)  In 370m turn slightly right onto 'Route 23' 'Main Street', then bear left", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 370, street(p, "Route 23", "Main Street", "")).then().bearLeft(street(p, "")));
		addButton(ll, "\u25BA (3.6)  Turn sharply right onto 'Dr.-Quinn-Stra"+"\u00df"+"e'", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SH, street(p, "", "Dr.-Quinn-Straße", "")));

		addButton(ll, "Keep left/right: prepareTurn, makeTurnIn, turn:", builder(p));
		addButton(ll, "\u25BA (4.1)  After 1810m keep left ' '", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 1810, street(p, "")));
		addButton(ll, "\u25BA (4.2)  In 400m keep left ' ' then in 80m keep right onto 'A1'", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 400, street(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 80, street(p,"", "A1")));
		addButton(ll, "\u25BA (4.3)  Keep right on 'Highway 60'", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, street(p, "Highway 60", "", "", "Highway 60")));
		addButton(ll, "\u25BA (4.4)  Turn left onto 'Broadway', then in 100m keep right and arrive at your destination 'Town Hall'",  
				builder(p).turn(AbstractPrologCommandPlayer.A_LEFT, street(p, "Broadway"))
				.then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 100, street(p, "")).andArriveAtDestination("Town Hall"));
		addButton(ll, "Roundabouts: prepareTurn, makeTurnIn, turn:", builder(p));
		addButton(ll, "\u25BA (5.1)  After 1250m enter a roundabout", builder(p).prepareRoundAbout(1250, 3, street(p,"", "I 15", "Los Angeles")));
		addButton(ll, "\u25BA (5.2)  In 450m enter the roundabout and take the 1st exit onto 'I 15' toward 'Los Angeles'", builder(p).roundAbout(450, 0, 1, street(p,"", "I 15", "Los Angeles")));
		addButton(ll, "\u25BA (5.3)  Roundabout: Take the 2nd exit onto 'Highway 60'", builder(p).roundAbout(0, 2, street(p, "Highway 60")));

		addButton(ll, "U-turns: prepareTurn, makeTurnIn, turn, when possible:", builder(p));
		addButton(ll, "\u25BA (6.1)  After 640m make a U-turn", builder(p).prepareMakeUT(640, street(p, "")));
		addButton(ll, "\u25BA (6.2)  In 400m make a U-turn", builder(p).makeUT(400, street(p, "")));
		addButton(ll, "\u25BA (6.3)  Make a U-turn on 'Riviera'", builder(p).makeUT(street(p, "Riviera", "", "", "Riviera")));
		addButton(ll, "\u25BA (6.4)  When possible, make a U-turn", builder(p).makeUTwp());

		addButton(ll, "Go straight, follow the road, approaching:", builder(p));
		addButton(ll, "\u25BA (7.1)  Straight ahead", builder(p).goAhead());
		addButton(ll, "\u25BA (7.2)  Continue for 2350m to ' '", builder(p).goAhead(2350, street(p)));
		addButton(ll, "\u25BA (7.3)  Continue for 360m to 'Broadway' and arrive at your intermediate destination ' '", builder(p).goAhead(360, street(p,"Broadway")).andArriveAtIntermediatePoint(""));
		addButton(ll, "\u25BA (7.4)  Continue for 800m to 'Dr Martin Luther King Jr Boulevard' and arrive at your destination ' '", builder(p).goAhead(800, street(p,"", "Dr Martin Luther King Jr Boulevard")).andArriveAtDestination(""));
		addButton(ll, "\u25BA (7.5)  Continue for 200m and pass GPX waypoint 'Trailhead'", builder(p).goAhead(200, new StreetName()).andArriveAtWayPoint("Trailhead") );
		addButton(ll, "\u25BA (7.6)  Continue for 400m and pass favorite 'Brewery'", builder(p).goAhead(400, new StreetName()).andArriveAtFavorite("Brewery") );
		addButton(ll, "\u25BA (7.7)  Continue for 600m and pass POI 'Museum'", builder(p).goAhead(600, new StreetName()).andArriveAtPoi("Museum") );

		addButton(ll, "Arriving and passing points:", builder(p));
		addButton(ll, "\u25BA (8.1)  Arrive at your destination 'Home'", builder(p).arrivedAtDestination("Home"));
		addButton(ll, "\u25BA (8.2)  Arrive at your intermediate destination 'Friend'", builder(p).arrivedAtIntermediatePoint("Friend"));
		addButton(ll, "\u25BA (8.3)  Passing GPX waypoint 'Trailhead'", builder(p).arrivedAtWayPoint("Trailhead"));
		addButton(ll, "\u25BA (8.4)  Passing favorite 'Brewery'", builder(p).arrivedAtFavorite("Brewery"));
		addButton(ll, "\u25BA (8.5)  Passing POI 'Museum'", builder(p).arrivedAtPoi("Museum"));

		addButton(ll, "Attention prompts:", builder(p));
		addButton(ll, "\u25BA (9.1)  You are exceeding the speed limit '50' (18 m/s)", builder(p).speedAlarm(50, 18f));
		addButton(ll, "\u25BA (9.2)  Attention, speed camera", builder(p).attention("SPEED_CAMERA"));
		addButton(ll, "\u25BA (9.3)  Attention, border control", builder(p).attention("BORDER_CONTROL"));
		addButton(ll, "\u25BA (9.4)  Attention, railroad crossing", builder(p).attention("RAILWAY"));
		addButton(ll, "\u25BA (9.5)  Attention, traffic calming", builder(p).attention("TRAFFIC_CALMING"));
		addButton(ll, "\u25BA (9.6)  Attention, toll booth", builder(p).attention("TOLL_BOOTH"));
		addButton(ll, "\u25BA (9.7)  Attention, stop sign", builder(p).attention("STOP"));
		addButton(ll, "\u25BA (9.8)  Attention, pedestrian crosswalk", builder(p).attention("PEDESTRIAN"));
		addButton(ll, "\u25BA (9.9)  Attention, tunnel", builder(p).attention("TUNNEL"));

		addButton(ll, "Other prompts:", builder(p));
		addButton(ll, "\u25BA (10.1) GPS signal lost", builder(p).gpsLocationLost());
		addButton(ll, "\u25BA (10.2) GPS signal recovered", builder(p).gpsLocationRecover());
		addButton(ll, "\u25BA (10.3) You have been off the route for 1050m", builder(p).offRoute(1050));
		addButton(ll, "\u25BA (10.4) You are back on the route", builder(p).backOnRoute());

		addButton(ll, "Voice system info:", builder(p));
		addButton(ll, "\u25BA (11.1) (Tap to refresh)\n" + getVoiceSystemInfo(), builder(p).attention(""));
		addButton(ll, "\u25BA (11.2) Tap to change Phone call audio delay (if car stereo cuts off prompts). Default is 1500\u00A0ms.", builder(p).attention(""));
		ll.forceLayout();
	}

	private StreetName street(CommandPlayer p, String... args) {
		Map<String, String> res = new HashMap<>();
		if (!p.supportsStructuredStreetNames()) {
			return new StreetName();
		}
		String[] streetNames = new String[]{"toRef", "toStreetName", "toDest", "fromRef", "fromStreetName", "fromDest"};
		for (int i = 0; i < args.length; i++) {
			res.put(streetNames[i], args[i]);
		}
		for (String streetName : streetNames) {
			if (res.get(streetName) == null) {
				res.put(streetName, "");
			}
		}
		return new StreetName(res);
	}

	private CommandBuilder builder(CommandPlayer p){
		return p.newCommandBuilder();
	}

	public void addButton(ViewGroup layout, final String description, final CommandBuilder builder){
		final Button button = new Button(this);
		button.setGravity(Gravity.LEFT);
		button.setTransformationMethod(null); //or else button text is all upper case
		button.setText(description);
		button.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		if (!description.startsWith("\u25BA (")) {
			// Section headline buttons
			button.setPadding(10, 20, 10, 5);
		} else {
			button.setPadding(40, 5, 10, 5);
		}
		if (description.startsWith("\u25BA (11.1)")) {
			infoButton = button;
		}
		
		layout.addView(button);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				builder.play();
				if (description.startsWith("\u25BA (11.1)")) {
					infoButton.setText("\u25BA (11.1) (Tap to refresh)\n" + getVoiceSystemInfo());
					Toast.makeText(TestVoiceActivity.this, "Info refreshed.", Toast.LENGTH_LONG).show();
				}
				if (description.startsWith("\u25BA (11.2)")) {
					if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 0) {
						if (((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() == 1000) {
							((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.set(1500);
						} else if (((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() == 1500) {
							((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.set(2000);
						} else if (((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() == 2000) {
							((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.set(2500);
						} else if (((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() == 2500) {
							((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.set(3000);
						} else {
							((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.set(1000);
						}
						infoButton.setText("\u25BA (11.1) (Tap to refresh)\n" + getVoiceSystemInfo());
						Toast.makeText(TestVoiceActivity.this, "BT SCO init delay changed to " + ((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() + "\u00A0ms.", Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(TestVoiceActivity.this, "Setting only available when using 'Phone call audio'.", Toast.LENGTH_LONG).show();
					}
				}
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;
		}
		return false;
	}
}
