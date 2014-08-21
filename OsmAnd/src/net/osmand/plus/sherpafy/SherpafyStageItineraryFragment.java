package net.osmand.plus.sherpafy;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class SherpafyStageItineraryFragment extends SherpafyStageInfoFragment {
	
	protected void updateView(WebView description, ImageView icon, TextView additional, TextView text, TextView header) {
		if (stage.getItineraryBitmap() != null) {
			icon.setImageBitmap(stage.getItineraryBitmap());
		} else {
			icon.setVisibility(View.GONE);
		}
		additional.setVisibility(View.GONE);
		header.setVisibility(View.GONE);
		String ins = "";
		if(stage.distance > 0) {
			ins += "<h4>" + app.getString(R.string.distance) + ": "+ OsmAndFormatter.getFormattedDistance((float) stage.distance, app) + "<h4/>";
		}
		if(stage.duration > 0) {
			int min = stage.duration % 60;
			int h = stage.duration / 60;
			ins += "<h4>" + app.getString(R.string.duration) + ": "+ 
					( h == 0 ? "" : h + " " + app.getString(R.string.int_hour) + " ") + 
					( min == 0 ? "" : min + " " + app.getString(R.string.int_min))+ "<h4/>";
		}
		description.loadData("<html><body>" + ins + stage.getItinerary() + "</body></html", "text/html; charset=utf-8",
				"utf-8");
	}
}