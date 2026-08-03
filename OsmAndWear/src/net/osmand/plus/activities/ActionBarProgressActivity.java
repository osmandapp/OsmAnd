package net.osmand.plus.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import net.osmand.plus.utils.AndroidUtils;
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
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayShowCustomEnabled(true);
			supportActionBar.setCustomView(progressBar);
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.getCustomView().setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public Toolbar getClearToolbar(boolean visible) {
		Toolbar tb = findViewById(R.id.bottomControls);
		if (tb != null) {
			tb.setTitle(null);
			tb.getMenu().clear();
			tb.setVisibility(visible ? View.VISIBLE : View.GONE);
			return tb;
		} else {
			return null;
		}
	}

	public void setToolbarVisibility(boolean visible) {
		View toolbar = findViewById(R.id.bottomControls);
		if (toolbar != null) {
			toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public boolean isToolbarVisible() {
		View toolbar = findViewById(R.id.bottomControls);
		return toolbar != null && toolbar.getVisibility() == View.VISIBLE;
	}

	public void updateListViewFooter(View footerView) {
		if (footerView != null) {
			View bottomMarginView = footerView.findViewById(R.id.bottomMarginView);
			if (bottomMarginView != null) {
				if (isToolbarVisible()) {
					bottomMarginView.setLayoutParams(new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(this, 72f)));
				} else {
					bottomMarginView.setLayoutParams(new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(this, 16f)));
				}
			}
		}
	}
}
