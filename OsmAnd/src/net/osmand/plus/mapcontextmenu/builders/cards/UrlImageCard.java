package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
		this.icon = getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_osmand_logo, R.color.osmand_orange);
		if (!Algorithms.isEmpty(getImageUrl())) {
			this.onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(getUrl()));
					v.getContext().startActivity(intent);
				}
			};
		}
	}
}
