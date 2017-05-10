package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		if (!Algorithms.isEmpty(getUrl())) {
			OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(getUrl()));
					v.getContext().startActivity(intent);
				}
			};
			if (!Algorithms.isEmpty(buttonText)) {
				this.onButtonClickListener = onClickListener;
			} else {
				this.onClickListener = onClickListener;
			}
		}
	}
}
