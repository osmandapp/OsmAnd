package net.osmand.plus.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import net.osmand.access.AccessibleActivity;
import net.osmand.plus.R;

public class PhoneActivity extends AccessibleActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.phone_fragment);
        
        View backToMainMenuButton = findViewById(R.id.phoneToMenuButton);
        backToMainMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newIntent = new Intent(PhoneActivity.this, MainMenuActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
            }
        });
    }
}
