package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(OsmandApplication app, JSONObject imageObject) {
		super(app, imageObject);
		this.icon = app.getIconsCache().getIcon(R.drawable.ic_action_osmand_logo, R.color.osmand_orange);
		if (!Algorithms.isEmpty(getImageUrl())) {
			this.onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(getImageUrl()));
					v.getContext().startActivity(intent);
				}
			};
		}
	}

	@Override
	public void update() {
		super.update();
		if (view != null) {
			ImageView image = (ImageView) view.findViewById(R.id.image);
			image.setVisibility(View.GONE);
			TextView urlText = (TextView) view.findViewById(R.id.url);
			urlText.setText(getImageUrl());
			urlText.setVisibility(View.VISIBLE);
		}
	}
}
