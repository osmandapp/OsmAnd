package net.osmand.plus.dashboard;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MainMenuActivity;

/**
 * Created by Denis on 23.12.2014.
 */
public class DashAudioVideoNotesActivity extends SherlockFragmentActivity {

	@Override
	protected void onResume() {
		super.onResume();
		setContentView(R.layout.audio_video_notes_all);

		ColorDrawable color = new ColorDrawable(getResources().getColor(R.color.actionbar_color));
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.audionotes_plugin_name);
		actionBar.setBackgroundDrawable(color);
		actionBar.setIcon(android.R.color.transparent);
		actionBar.setHomeButtonEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
