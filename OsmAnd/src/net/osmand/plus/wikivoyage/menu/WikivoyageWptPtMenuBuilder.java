package net.osmand.plus.wikivoyage.menu;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.data.Amenity;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BuildRowAttrs;
import net.osmand.plus.mapcontextmenu.builders.WptPtMenuBuilder;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.views.layers.PlaceDetailsObject;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.HashMap;

public class WikivoyageWptPtMenuBuilder extends WptPtMenuBuilder {

	private static final String KEY_PHONE = "Phone: ";
	private static final String KEY_EMAIL = "Email: ";
	private static final String KEY_WORKING_HOURS = "Working hours: ";
	private static final String KEY_PRICE = "Price: ";
	private static final String KEY_DIRECTIONS = "Directions: ";
	private static final String KEY_DESCRIPTION = "Description";

	private String mainImageUrl;
	private HashMap<String, String> descTokens;

	public WikivoyageWptPtMenuBuilder(@NonNull MapActivity activity, @NonNull WptPt wpt,
			@Nullable PlaceDetailsObject detailsObject) {
		super(activity, wpt, detailsObject);
		updateImageLinkAndDescriptionTokens(wpt);
	}

	public void updateImageLinkAndDescriptionTokens(@NonNull WptPt wpt) {
		if (wpt.getLink() != null && PicassoUtils.isImageUrl(wpt.getLink().getHref())) {
			mainImageUrl = wpt.getLink().getHref();
		}
		descTokens = getDescriptionTokens(wpt.getDesc(), KEY_PHONE, KEY_EMAIL, KEY_WORKING_HOURS, KEY_PRICE, KEY_DIRECTIONS);
	}

	@Override
	protected void buildMainImage(View view) {
		if (mainImageUrl != null) {
			AppCompatImageView imageView = inflateAndGetMainImageView(view);
			PicassoUtils.setupImageViewByUrl(app, imageView, mainImageUrl, true);
		}
	}

	@Override
	protected void buildDescription(View view) {
		String desc = descTokens.get(KEY_DESCRIPTION);
		if (!Algorithms.isEmpty(desc)) {
			buildDescriptionRow(view, desc);
		}
	}

	@Override
	protected void prepareDescription(WptPt wpt, View view) {
		String phones = descTokens.get(KEY_PHONE);
		String emails = descTokens.get(KEY_EMAIL);
		String workingHours = descTokens.get(KEY_WORKING_HOURS);
		String price = descTokens.get(KEY_PRICE);
		String direction = descTokens.get(KEY_DIRECTIONS);

		if (!Algorithms.isEmpty(phones)) {
			buildRow(view, new BuildRowAttrs.Builder().setText(phones).setIconId(R.drawable.ic_action_call_dark)
					.setNumber(true).setTextPrefix(app.getString(R.string.phone)).build());
		}
		if (wpt.getLink() != null && !Algorithms.isEmpty(wpt.getLink().getHref())) {
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_world_globe_dark)
					.setText(wpt.getLink().getHref()).setTextPrefix(app.getString(R.string.shared_string_link))
					.setUrl(true).build());
		}
		if (!Algorithms.isEmpty(emails)) {
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_action_message)
					.setText(emails).setTextPrefix(app.getString(R.string.poi_email)).setEmail(true).build());
		}
		if (!Algorithms.isEmpty(workingHours)) {
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_action_time)
					.setText(workingHours).setTextPrefix(app.getString(R.string.opening_hours)).build());
		}
		if (!Algorithms.isEmpty(direction)) {
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_action_gdirections_dark)
					.setText(direction).setTextPrefix(app.getString(R.string.poi_direction)).build());
		}
		if (!Algorithms.isEmpty(price)) {
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_action_price_tag)
					.setText(price).setTextPrefix(app.getString(R.string.shared_string_price)).build());
		}
	}

	private HashMap<String, String> getDescriptionTokens(String desc, String... allowedKeys) {
		HashMap<String, String> mTokens = new HashMap<>();
		if (!Algorithms.isEmpty(desc)) {
			String[] tokens = desc.split("\n");
			for (String token : tokens) {
				boolean matched = false;
				for (String key : allowedKeys) {
					if (token.startsWith(key)) {
						matched = true;
						String value = token.substring(key.length()).trim();
						mTokens.put(key, value);
					}
				}
				if (!matched) {
					String s = mTokens.get(KEY_DESCRIPTION);
					mTokens.put(KEY_DESCRIPTION, s != null ? s + "\n" + token : token);
				}
			}
		}
		return mTokens;
	}
}