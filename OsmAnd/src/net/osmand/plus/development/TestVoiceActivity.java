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

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.JSCommandBuilder;
import net.osmand.plus.voice.JSMediaCommandPlayerImpl;
import net.osmand.plus.voice.JSTTSCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;

import static net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu.getVoiceFiles;


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
		Set<String> voiceFiles = getVoiceFiles(this);
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
		v += " \u25CF App profile:   " + ((OsmandApplication) getApplication()).getSettings().APPLICATION_MODE.get().getStringKey();

		if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 3) {
			v += "\n \u25CF Voice guidance output:   Media/music audio";
		} else if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 5) {
			v += "\n \u25CF Voice guidance output:   Notification audio";
		} else if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 0) {
			v += "\n \u25CF Voice guidance output:   Phone call audio";
		} else {
			v += "\n \u25CF Voice guidance output:   " + ((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get();
		}

		v += "\n \u25CF OsmAnd voice:   " + osmandVoice;
		v += "\n \u25CF OsmAnd voice language:   " + osmandVoiceLang;

		if (AbstractPrologCommandPlayer.getCurrentVersion() > 99) {
			v += "\n \u25CF Voice language availability:   " + TTSCommandPlayerImpl.getTtsVoiceStatus();
			v += "\n \u25CF Voice actually used:   " + TTSCommandPlayerImpl.getTtsVoiceUsed();
		} else {
			v += "\n \u25CF Voice language availability:   Recorded voice";
			v += "\n \u25CF Voice actually used:   Recorded voice";
		}

		if (((OsmandApplication) getApplication()).getSettings().AUDIO_STREAM_GUIDANCE.get() == 0) {
			v += "\n \u25CF BT SCO:   " + AbstractPrologCommandPlayer.btScoInit;
		} else {
			v += "\n \u25CF BT SCO:   The current app profile is not set to use 'Phone call audio'.";
		}

		v += "\n \u25CF Phone call audio delay:   " + ((OsmandApplication) getApplication()).getSettings().BT_SCO_DELAY.get() + "\u00A0ms";
		return v;
	}

	private Term street(CommandPlayer p, String name) {
		return street(p, name, "", "", "");
	}
	
	private Term street(CommandPlayer p, String name, String ref) {
		return street(p, name, ref, "", "");
	}
	
	private Term street(CommandPlayer p, String name, String ref, String dest) {
		return street(p, name, ref, dest, "");
	}
	
	private Term getTermString(String s) {
		if(!Algorithms.isEmpty(s)) {
			return new Struct(s);
		}
		return new Struct("");
	}

	private Term street(CommandPlayer p, String name, String ref, String destName, String currentName) {
		// Pass all test strings through our character replacement method
			ref = VoiceRouter.getSpeakablePointName(ref);
			name = VoiceRouter.getSpeakablePointName(name);
			destName = VoiceRouter.getSpeakablePointName(destName);
			currentName = VoiceRouter.getSpeakablePointName(currentName);

		if(p.supportsStructuredStreetNames()) {
			Struct next = new Struct(new Term[] { getTermString(ref),
					getTermString(name),
					getTermString(destName) });
			Term current = new Struct("");
			if (currentName.length() > 0) {
				current = new Struct(new Term[] { getTermString(""),
						getTermString(currentName),
						getTermString("") });
			}
			Struct voice = new Struct("voice", next, current );
			return voice;
		}
		return new Struct(name);
	}

	private void addButtons(final LinearLayout ll, CommandPlayer p) {
		boolean isJS = p instanceof JSTTSCommandPlayerImpl || p instanceof JSMediaCommandPlayerImpl;
		addButton(ll, "Route calculated and number tests:", builder(p));
		addButton(ll, "\u25BA (1.1)  New route calculated, 150m, 230sec (00:03:50)", !isJS ? builder(p).newRouteCalculated(150, 230) : jsBuilder(p).newRouteCalculated(150, 230));
		addButton(ll, "\u25BA (1.2)  New route calculated, 1350m, 3680sec (01:01:20)", !isJS ? builder(p).newRouteCalculated(1350, 3680) : jsBuilder(p).newRouteCalculated(1350, 3680));
		addButton(ll, "\u25BA (1.3)  New route calculated 3700m, 7320sec (02:02)", !isJS ? builder(p).newRouteCalculated(3700, 7320) : jsBuilder(p).newRouteCalculated(3700, 7320));
		addButton(ll, "\u25BA (1.4)  New route calculated 9100m, 10980sec (03:03)", !isJS ? builder(p).newRouteCalculated(9100, 10980) : jsBuilder(p).newRouteCalculated(9100, 10980));
		addButton(ll, "\u25BA (2.1)  Route recalculated 11500m, 18600sec (05:10)", !isJS ? builder(p).routeRecalculated(11500, 18600) : jsBuilder(p).routeRecalculated(11500, 18600));
		addButton(ll, "\u25BA (2.2)  Route recalculated 19633m, 26700sec (07:25)", !isJS ? builder(p).routeRecalculated(19633, 26700) : jsBuilder(p).routeRecalculated(19633, 26700));
		addButton(ll, "\u25BA (2.3)  Route recalculated 89750m, 55800sec (15:30)", !isJS ? builder(p).routeRecalculated(89750, 55800) : jsBuilder(p).routeRecalculated(89750, 55800));
		addButton(ll, "\u25BA (2.4)  Route recalculated 125900m, 92700sec (25:45)", !isJS ? builder(p).routeRecalculated(125900, 92700) : jsBuilder(p).routeRecalculated(125900, 92700));

		addButton(ll, "All turn types: prepareTurn, makeTurnIn, turn:", builder(p));
		addButton(ll, "\u25BA (3.1)  After 1520m turn slightly left", !isJS ? builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_SL, 1520, street(p, "")) :
				jsBuilder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_SL, 1520, jsStreet(p, "")));
		addButton(ll, "\u25BA (3.2)  In 450m turn sharply left onto 'Hauptstra"+"\u00df"+"e', then bear right", !isJS ? builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_SH, 450, street(p, "Hauptstraße")).then().bearRight(street(p, "")) :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_LEFT_SH, 450, jsStreet(p, "Hauptstraße")).then().bearRight(jsStreet(p, "")));
		addButton(ll, "\u25BA (3.3)  Turn left, then in 100m turn slightly right", !isJS ? builder(p).turn(AbstractPrologCommandPlayer.A_LEFT, street(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 100, street(p, "")) :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_LEFT, jsStreet(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 100, jsStreet(p, "")));
		addButton(ll, "\u25BA (3.4)  After 3100m turn right onto 'SR 80' toward 'Rome'", !isJS ? builder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT, 3100, street(p, "", "SR 80", "Rome")) :
				jsBuilder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT, 3100, jsStreet(p, "", "SR 80", "Rome")));
		addButton(ll, "\u25BA (3.5)  In 370m turn slightly right onto 'Route 23' 'Main Street', then bear left", !isJS ? builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 370, street(p, "Main Street", "Route 23")).then().bearLeft(street(p, "")) :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 370, jsStreet(p, "Main jsStreet", "Route 23")).then().bearLeft(jsStreet(p, "")));
		addButton(ll, "\u25BA (3.6)  Turn sharply right onto 'Dr.-Quinn-Stra"+"\u00df"+"e'", !isJS ? builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SH, street(p, "Dr.-Quinn-Straße")) :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SH, jsStreet(p, "Dr.-Quinn-Straße")));

		addButton(ll, "Keep left/right: prepareTurn, makeTurnIn, turn:", builder(p));
		addButton(ll, "\u25BA (4.1)  After 1810m keep left ' '", !isJS ? builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 1810, street(p, "")) :
				jsBuilder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 1810, jsStreet(p, "")));
		addButton(ll, "\u25BA (4.2)  In 400m keep left ' ' then in 80m keep right onto 'A1'", !isJS ? builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 400, street(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 80, street(p,"", "A1")) :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 400, jsStreet(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 80, jsStreet(p,"", "A1")));
		addButton(ll, "\u25BA (4.3)  Keep right on 'Highway 60'", !isJS ? builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, street(p, "Highway 60", "", "", "Highway 60")) :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, jsStreet(p, "Highway 60", "", "", "Highway 60")));
		addButton(ll, "\u25BA (4.4)  Turn left onto 'Broadway', then in 100m keep right and arrive at your destination 'Town Hall'", !isJS ?
				builder(p).turn(AbstractPrologCommandPlayer.A_LEFT, street(p, "Broadway"))
				.then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 100, street(p, "")).andArriveAtDestination("Town Hall") :
				jsBuilder(p).turn(AbstractPrologCommandPlayer.A_LEFT, jsStreet(p, "Broadway"))
						.then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 100, jsStreet(p, "")).andArriveAtDestination("Town Hall"));

		addButton(ll, "Roundabouts: prepareTurn, makeTurnIn, turn:", builder(p));
		addButton(ll, "\u25BA (5.1)  After 1250m enter a roundabout", !isJS ? builder(p).prepareRoundAbout(1250, 3, street(p,"", "I 15", "Los Angeles")) :
				jsBuilder(p).prepareRoundAbout(1250, 3, jsStreet(p,"", "I 15", "Los Angeles")));
		addButton(ll, "\u25BA (5.2)  In 450m enter the roundabout and take the 1st exit onto 'I 15' toward 'Los Angeles'", !isJS ? builder(p).roundAbout(450, 0, 1, street(p,"", "I 15", "Los Angeles")) :
				jsBuilder(p).roundAbout(450, 0, 1, jsStreet(p,"", "I 15", "Los Angeles")));
		addButton(ll, "\u25BA (5.3)  Roundabout: Take the 2nd exit onto 'Highway 60'", !isJS ? builder(p).roundAbout(0, 2, street(p, "Highway 60")) :
				jsBuilder(p).roundAbout(0, 2, jsStreet(p, "Highway 60")));

		addButton(ll, "U-turns: prepareTurn, makeTurnIn, turn, when possible:", builder(p));
		addButton(ll, "\u25BA (6.1)  After 640m make a U-turn", !isJS ? builder(p).prepareMakeUT(640, street(p, "")) :
				jsBuilder(p).prepareMakeUT(640, jsStreet(p, "")));
		addButton(ll, "\u25BA (6.2)  In 400m make a U-turn", !isJS ? builder(p).makeUT(400, street(p, "")) :
				jsBuilder(p).makeUT(400, jsStreet(p, "")));
		addButton(ll, "\u25BA (6.3)  Make a U-turn on 'Riviera'", !isJS ? builder(p).makeUT(street(p, "Riviera", "", "", "Riviera")) :
				jsBuilder(p).makeUT(jsStreet(p, "Riviera", "", "", "Riviera")));
		addButton(ll, "\u25BA (6.4)  When possible, make a U-turn", builder(p).makeUTwp());

		addButton(ll, "Go straight, follow the road, approaching:", builder(p));
		addButton(ll, "\u25BA (7.1)  Straight ahead", builder(p).goAhead());
		addButton(ll, "\u25BA (7.2)  Continue for 2350m to ' '", !isJS ? builder(p).goAhead(2350, street(p, "")) :
				jsBuilder(p).goAhead(2350, jsStreet(p, "")));
		addButton(ll, "\u25BA (7.3)  Continue for 360m to 'Broadway' and arrive at your intermediate destination ' '", !isJS ? builder(p).goAhead(360, street(p,"Broadway")).andArriveAtIntermediatePoint("") :
				jsBuilder(p).goAhead(360, jsStreet(p,"Broadway")).andArriveAtIntermediatePoint(""));
		addButton(ll, "\u25BA (7.4)  Continue for 800m to 'Dr Martin Luther King Jr Boulevard' and arrive at your destination ' '", !isJS ? builder(p).goAhead(800, street(p,"", "Dr Martin Luther King Jr Boulevard")).andArriveAtDestination("") :
				jsBuilder(p).goAhead(800, jsStreet(p,"", "Dr Martin Luther King Jr Boulevard")).andArriveAtDestination(""));
		addButton(ll, "\u25BA (7.5)  Continue for 200m and pass GPX waypoint 'Trailhead'", !isJS ? builder(p).goAhead(200, null).andArriveAtWayPoint("Trailhead") : jsBuilder(p).goAhead(200, new HashMap<String, String>()).andArriveAtWayPoint("Trailhead"));
		addButton(ll, "\u25BA (7.6)  Continue for 400m and pass favorite 'Brewery'", !isJS ? builder(p).goAhead(400, null).andArriveAtFavorite("Brewery") : jsBuilder(p).goAhead(400, new HashMap<String, String>()).andArriveAtFavorite("Brewery"));
		addButton(ll, "\u25BA (7.7)  Continue for 600m and pass POI 'Museum'", !isJS ? builder(p).goAhead(600, null).andArriveAtPoi("Museum") : jsBuilder(p).goAhead(600, new HashMap<String, String>()).andArriveAtPoi("Museum"));

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

	private Map<String, String> jsStreet(CommandPlayer p, String... args) {
		Map<String, String> res = new HashMap<>();
		if (!p.supportsStructuredStreetNames()) {
			return res;
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
		return res;
	}

	private CommandBuilder builder(CommandPlayer p){
		return p.newCommandBuilder();
	}

	private JSCommandBuilder jsBuilder(CommandPlayer p) {
		return (JSCommandBuilder) p.newCommandBuilder();
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
