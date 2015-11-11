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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;


/**
 * Test Voice activity
 */
public class TestVoiceActivity extends OsmandActionBarActivity {



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
		tv.setText("Press buttons and listen various voice instructions, if you don't hear anything probably they are missed.");
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
		getSupportActionBar(). setTitle(R.string.test_voice_prompts);
		
		selectVoice(ll);
	}
	
	private Set<String> getVoiceFiles() {
		// read available voice data
		File extStorage = ((OsmandApplication) getApplication()).getAppPath(IndexConstants.VOICE_INDEX_DIR);
		Set<String> setFiles = new LinkedHashSet<String>();
		if (extStorage.exists()) {
			for (File f : extStorage.listFiles()) {
				if (f.isDirectory()) {
					setFiles.add(f.getName());
				}
			}
		}
		return setFiles;
	}
	private void selectVoice(final LinearLayout ll) {
		String[] entries;
		final String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() ];
		entrieValues = new String[voiceFiles.size() ];
		int k = 0;
		int selected = 0;
		for (String s : voiceFiles) {
			entries[k] = s;
			entrieValues[k] = s;
			if(s.equals(((OsmandApplication) getApplication()).getSettings().VOICE_PROVIDER)) {
				selected = k;
			}
			k++;
		}
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setSingleChoiceItems(entrieValues, selected, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final OsmandApplication app = (OsmandApplication) getApplication();
				app.getSettings().VOICE_PROVIDER.set(entrieValues[which]);
				app.showDialogInitializingCommandPlayer(TestVoiceActivity.this, true, new Runnable() {
					
					@Override
					public void run() {
						CommandPlayer p = app.getRoutingHelper().getVoiceRouter().getPlayer();
						if (p == null) {
							AccessibleToast.makeText(TestVoiceActivity.this, "Voice player not initialized", Toast.LENGTH_SHORT).show();
						} else {
							addButtons(ll, p);
						}						
					}
				}, true);
				dialog.dismiss();
			}
		});
		bld.show();
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
		addButton(ll, "New route has been calculated (11350m & 2h3m5sec)", builder(p).newRouteCalculated(11350, 7385));
		addButton(ll, "New route has been calculated (150m & 1m5sec)", builder(p).newRouteCalculated(150, 65));
		addButton(ll, "Route recalculated (23150m & 350sec)", builder(p).routeRecalculated(23150, 350));

		addButton(ll, "After 1520m turn slightly left", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_SL, 1520, street(p, "")));
		addButton(ll, "In 450m turn sharply left onto 'Hauptstra"+"\u00df"+"e', then bear right", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_SH, 450, street(p, "Hauptstra�e")).then().bearRight(street(p, "")));
		addButton(ll, "Turn left, then in 100m turn slightly right", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT, street(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 100, street(p, "")));
		addButton(ll, "After 3100m turn right onto 'SR 80'", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT, 3100, street(p, "SR 80")));
		addButton(ll, "In 370m turn slightly right onto 'F23' 'Main Street', then bear left", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 370, street(p, "Main Street", "F23")).then().bearLeft(street(p, "")));
		addButton(ll, "Turn sharply right onto 'Main Street'", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SH, street(p, "Main Street")));

		addButton(ll, "After 1810m keep left ' '", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 1810, street(p, "")));
		addButton(ll, "In 400m keep left ' ' then in 80m keep right 'A1'", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 400, street(p, "")).then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 80, street(p,"", "A1")));

		addButton(ll, "After 640m make a U-turn", builder(p).prepareMakeUT(640, street(p, "")));
		addButton(ll, "In 400m make a U-turn", builder(p).makeUT(400, street(p, "")));
		addButton(ll, "Make a U-turn on 'Riviera'", builder(p).makeUT(street(p, "Riviera")));
		addButton(ll, "When possible, make a U-turn", builder(p).makeUTwp());

		addButton(ll, "After 1250m enter a roundabout [and take the 3rd exit onto 'Liberty']", builder(p).prepareRoundAbout(1250, 3, street(p,"Liberty")));
		addButton(ll, "In 450m enter the roundabout and take the 1st exit onto 'Market Square'", builder(p).roundAbout(450, 0, 1, street(p,"", "", "Market Square")));
		addButton(ll, "Roundabout: Take the 2nd exit onto 'Bridge Avenue'", builder(p).roundAbout(0, 2, street(p, "Bridge Avenue")));

		addButton(ll, "Follow the road for 2350m to ' '", builder(p).goAhead(2350, street(p, "")));
		addButton(ll, "Follow the road for 360m to 'Broadway' and arrive at your waypoint ' '", builder(p).goAhead(360, street(p,"Broadway")).andArriveAtIntermediatePoint(""));
		addButton(ll, "Follow the road for 800m to 'A33' and arrive at your destination", builder(p).goAhead(800, street(p,"", "A33")).andArriveAtDestination(""));

		addButton(ll, "Arrive at your destination 'Home'", builder(p).arrivedAtDestination("Home"));
		addButton(ll, "Arrive at your intermediate point 'Friend'", builder(p).arrivedAtIntermediatePoint("Friend"));
		addButton(ll, "Arrive at your GPX waypoint 'Trailhead'", builder(p).arrivedAtWayPoint("Trailhead"));

		addButton(ll, "Attention, traffic calming", builder(p).attention("TRAFFIC_CALMING"));
		addButton(ll, "GPS signal lost", builder(p).gpsLocationLost());
		addButton(ll, "GPS signal recovered", builder(p).gpsLocationRecover());
		addButton(ll, "You have been off the route for 1050m", builder(p).offRoute(1050));
		addButton(ll, "You are exceeding the speed limit", builder(p).speedAlarm());
		ll.forceLayout();
	}
	
	
	
	private CommandBuilder builder(CommandPlayer p){
		return p.newCommandBuilder();
	}
	
	
	public void addButton(ViewGroup layout, String description, final CommandBuilder builder){
		Button button = new Button(this);
		button.setText(description);
		button.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		button.setPadding(10, 5, 10, 2);
		
		layout.addView(button);
		button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				builder.play();
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
