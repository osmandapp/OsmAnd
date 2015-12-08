package net.osmand.plus.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import net.osmand.plus.R;

public class ActionBarProgressActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupIntermediateProgressBar();
	}

	protected void setupIntermediateProgressBar() {
		ProgressBar progressBar = new ProgressBar(this);
		progressBar.setVisibility(View.GONE);
		progressBar.setIndeterminate(true);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		getSupportActionBar().setCustomView(progressBar);
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
		getSupportActionBar().getCustomView().setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		if (tb == null) {
			return null;
		}
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible ? View.VISIBLE : View.GONE);
		return tb;
	}

	public void setToolbarVisibility(boolean visible) {
		View toolbar = findViewById(R.id.bottomControls);
		if (toolbar != null) {
			toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}
}
