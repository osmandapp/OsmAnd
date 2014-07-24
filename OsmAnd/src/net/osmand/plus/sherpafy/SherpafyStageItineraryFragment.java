package net.osmand.plus.sherpafy;

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
		description.loadData("<html><body>" + stage.getItinerary() 
				+ "</body></html", "text/html; charset=utf-8", "utf-8");
	}
}