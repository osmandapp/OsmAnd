/**
 * 
 */
package net.osmand.plus.activities;

import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.voice.CommandPlayerFactory;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

/**
 * Test Voice activity
 */
public class TestVoiceActivity extends Activity {



	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		
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
		Runnable r = new Runnable(){

			@Override
			public void run() {
				CommandPlayer p = app.getRoutingHelper().getVoiceRouter().getPlayer();
				if (p == null) {
					Toast.makeText(TestVoiceActivity.this, "Voice player not initialized", Toast.LENGTH_SHORT).show();
				} else {
					addButtons(ll, p);
				}
			}
			
		};
		if (app.getRoutingHelper().getVoiceRouter().getPlayer() != null) {
			r.run();
		} else {
			app.showDialogInitializingCommandPlayer(this, true, r);
		}
	}
	
	private void addButtons(final LinearLayout ll, CommandPlayer p) {
		addButton(ll, "New route is calculated (15350 m)", builder(p).newRouteCalculated(15350));
		addButton(ll, "Prepare 400 m make UT", builder(p).prepareMakeUT(400));
		addButton(ll, "Prepare 320 m make right turn", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT, 320));
		addButton(ll, "In 370 m make right sharp turn", builder(p).turn(AbstractPrologCommandPlayer.A_RIGHT_SH, 370));
		addButton(ll, "In 1050 m make left slight turn", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT_SL, 1050));
		addButton(ll, "Make left turn", builder(p).turn(AbstractPrologCommandPlayer.A_LEFT));
		addButton(ll, "Prepare right SL turn in 850 and then bear right", builder(p).prepareTurn(AbstractPrologCommandPlayer.A_RIGHT_SL, 850).then().bearRight());
		addButton(ll, "Go ahead 800 and arrive at destination", builder(p).goAhead(800).andArriveAtDestination());
		addButton(ll, "Arrive at destination", builder(p).arrivedAtDestination());
		addButton(ll, "Gps location lost", builder(p).gpsLocationLost());
		addButton(ll, "Make UT in 640", builder(p).makeUT(640));
		addButton(ll, "Route recalculated 23150", builder(p).routeRecalculated(23150));
		addButton(ll, "Prepare roundabout 750", builder(p).prepareRoundAbout(750));
		addButton(ll, "Roundabout 3 exit", builder(p).roundAbout(0, 3));
		addButton(ll, "In 450 roundabout 1 exit", builder(p).roundAbout(450, 0, 1));
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

}
