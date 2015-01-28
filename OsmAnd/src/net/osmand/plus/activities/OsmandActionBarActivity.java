package net.osmand.plus.activities;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import net.osmand.plus.R;

/**
 * Created by Denis
 * on 28.01.15.
 */
public class OsmandActionBarActivity extends ActionBarActivity {

    //should be called after set content view
    protected void setupHomeButton(){
        Drawable back = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        back.setColorFilter(0xffffffff, PorterDuff.Mode.MULTIPLY);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(back);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupHomeButton();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setupHomeButton();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setupHomeButton();
    }
}
