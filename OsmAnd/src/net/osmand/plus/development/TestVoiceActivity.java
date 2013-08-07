/**
 * 
 */
package net.osmand.plus.development;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * Test Voice activity
 */
public class TestVoiceActivity extends SherlockActivity {



	@Override
	public void onCreate(Bundle icicle) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
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
		Builder bld = new AlertDialog.Builder(this);
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
				});
				dialog.dismiss();
			}
		});
		bld.show();
	}
	
	private void addButtons(final LinearLayout ll, CommandPlayer p) {
		addButton(ll, "New route has been calculated (11350m & 2h30m5sec)", builder(p).newRouteCalculated(11350, 9005));
		addButton(ll, "Route recalculated (23150m & 350sec)", builder(p).routeRecalculated(23150, 350));

		addButton(ll, "Prepare to turn slighlty left after 850m then bear right", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_SL, 850, "").then().bearRight(""));
		addButton(ll, "After 1050m turn sharply left onto 'Hauptstrasse'", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_SH, 1050, "Hauptstrasse"));
		addButton(ll, "Turn left onto 'Main Street'", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT, "Main Street"));
		addButton(ll, "Prepare to turn right after 320m onto 'Mini'", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT, 320, "Mini"));
		addButton(ll, "After 370m turn slightly right onto 'F23'", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SL, 370, "F23"));
		addButton(ll, "Turn sharply right onto 'Main Street' then bear left", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SH, "Main Street").then().bearLeft(""));

		addButton(ll, "Prepare to keep left ' ' after 370m", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_LEFT_KEEP, 370, ""));
		addButton(ll, "Keep left ' ' then after 400m keep right 'A1'", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_KEEP, "").then().turn(AbstractPrologCommandPlayer.A_RIGHT_KEEP, 400, "A1"));

		addButton(ll, "Prepare to make a U-turn after 400m", builder(p).prepareMakeUT(400, ""));
		addButton(ll, "After 640m make a U-turn", builder(p).makeUT(640, ""));
		addButton(ll, "Make a U-turn on 'Riviera'", builder(p).makeUT("Riviera"));
		addButton(ll, "When possible, make a U-turn", builder(p).makeUTwp());

		addButton(ll, "Prepare to enter a roundabout after 750m (and take the 3rd exit onto 'Liberty')", builder(p).prepareRoundAbout(750, 3, "Liberty"));
		addButton(ll, "After 450m enter the roundabout and take the 1st exit onto 'Market Square'", builder(p).roundAbout(450, 0, 1, "Market Square"));
		addButton(ll, "Roundabout: Take the 2nd exit onto 'Bridge Avenue'", builder(p).roundAbout(0, 2, "Bridge Avenue"));

		addButton(ll, "Follow the road for 2350m to ' '", builder(p).goAhead(2350, ""));
		addButton(ll, "Follow the road for 360m to 'Broadway' and arrive at your waypoint ' '", builder(p).goAhead(360, "Broadway").andArriveAtIntermediatePoint(""));
		addButton(ll, "Follow the road for 800m to 'A33' and arrive at your destination", builder(p).goAhead(800, "A33").andArriveAtDestination(""));

		addButton(ll, "Arrive at your destination 'Home'", builder(p).arrivedAtDestination("Home"));
		addButton(ll, "Arrive at your intermediate point 'Friend'", builder(p).arrivedAtIntermediatePoint("Friend"));
		addButton(ll, "Arrive at your GPX waypoint 'Trailhead'", builder(p).arrivedAtWayPoint("Trailhead"));

		addButton(ll, "Attention, 'bump'", builder(p).attention("bump"));
		addButton(ll, "GPS signal lost", builder(p).gpsLocationLost());
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
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

}
