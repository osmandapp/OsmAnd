package net.osmand.plus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import net.osmand.access.AccessibleActivity;
import net.osmand.plus.R;

public class MusicActivity extends AccessibleActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.music_fragment);
        
        View backToMainMenuButton = findViewById(R.id.musicToMenuButton);
        backToMainMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newIntent = new Intent(MusicActivity.this, MainMenuActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
            }
        });
    }

}
