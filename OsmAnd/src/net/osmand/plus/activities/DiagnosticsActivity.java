package net.osmand.plus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import net.osmand.access.AccessibleActivity;
import net.osmand.plus.R;

public class DiagnosticsActivity extends AccessibleActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.diagnostics_fragment);
        
        View backToMainMenuButton = findViewById(R.id.diagnosticsToMenuButton);
        backToMainMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newIntent = new Intent(DiagnosticsActivity.this, MainMenuActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
            }
        });
        
        // Hack up data for now to make diagnostics screen look right
        // Overall Driving Score
        TextView drivingScoreText = (TextView) findViewById(R.id.drivingScoreText);
        drivingScoreText.setText("86 %");
        RatingBar drivingScoreRating = (RatingBar) findViewById(R.id.drivingScoreRating);
        drivingScoreRating.setRating(3);
        // Current driving stats
        ((TextView) findViewById(R.id.speedText)).setText("28 km/hr");
        ((ProgressBar) findViewById(R.id.tirePressureProgressBar)).setProgress(95);
        ((ProgressBar) findViewById(R.id.fuelLevelProgressBar)).setProgress(50);
        ((ProgressBar) findViewById(R.id.coolantLevelProgressBar)).setProgress(84);
        // Driving quality stats
        ((ProgressBar) findViewById(R.id.brakingScoreProgressBar)).setProgress(90);
        ((ProgressBar) findViewById(R.id.accelerationScoreProgressBar)).setProgress(82);
        ((ProgressBar) findViewById(R.id.speedScoreProgressBar)).setProgress(75);
        // Fuel economy
        ((TextView) findViewById(R.id.currMileageText)).setText("8 km/litre");
        ((TextView) findViewById(R.id.avgMileageText)).setText("11.5 km/litre");
    }

}
