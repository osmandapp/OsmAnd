package net.osmand.plus.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Created by Denis
 * on 23.01.15.
 */
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
}
