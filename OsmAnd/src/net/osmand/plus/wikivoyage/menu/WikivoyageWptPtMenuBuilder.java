package net.osmand.plus.wikivoyage.menu;

import android.support.annotation.NonNull;
import android.view.View;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.WptPtMenuBuilder;
import net.osmand.util.Algorithms;

import java.util.HashMap;

public class WikivoyageWptPtMenuBuilder extends WptPtMenuBuilder {

	private final static String KEY_PHONE = "Phone: ";
	private final static String KEY_EMAIL = "Email: ";
	private final static String KEY_WORKING_HOURS = "Working hours: ";
	private final static String KEY_PRICE = "Price: ";
	private final static String KEY_DIRECTIONS = "Directions: ";
	private final static String KEY_DESCRIPTION = "Description";

	private HashMap<String, String> descTokens;

	public WikivoyageWptPtMenuBuilder(@NonNull MapActivity mapActivity, @NonNull WptPt wpt) {
		super(mapActivity, wpt);
		descTokens = getDescriptionTokens(wpt.desc, KEY_PHONE, KEY_EMAIL, KEY_WORKING_HOURS, KEY_PRICE, KEY_DIRECTIONS);
	}

	@Override
	protected void buildDescription(View view) {
		final String desc = descTokens.get(KEY_DESCRIPTION);
		if (!Algorithms.isEmpty(desc)) {
			buildDescriptionRow(view, app.getString(R.string.shared_string_description), desc, 0, 10, true);
		}
	}

	@Override
	protected void prepareDescription(final WptPt wpt, View view) {
		String phones = descTokens.get(KEY_PHONE);
		String emails = descTokens.get(KEY_EMAIL);
		String workingHours = descTokens.get(KEY_WORKING_HOURS);
		String price = descTokens.get(KEY_PRICE);
		String direction = descTokens.get(KEY_DIRECTIONS);

		if (!Algorithms.isEmpty(phones)) {
			buildRow(view, R.drawable.ic_action_call_dark,
					null, phones, 0,
					false, null, false, 0, false, true, false, null, false);
		}
		if (!Algorithms.isEmpty(wpt.link)) {
			buildRow(view, R.drawable.ic_world_globe_dark,
					null, wpt.link, 0,
					false, null, false, 0, true, null, false);
		}
		if (!Algorithms.isEmpty(emails)) {
			buildRow(view, R.drawable.ic_action_message,
					null, emails, 0,
					false, null, false, 0, false, false, true, null, false);
		}
		if (!Algorithms.isEmpty(workingHours)) {
			buildRow(view, R.drawable.ic_action_time,
					null, workingHours, 0,
					false, null, false, 0, false, null, false);
		}
		if (!Algorithms.isEmpty(direction)) {
			buildRow(view, R.drawable.ic_action_gdirections_dark,
					null, direction, 0,
					false, null, false, 0, false, null, false);
		}
		if (!Algorithms.isEmpty(price)) {
			buildRow(view, R.drawable.ic_action_price_tag,
					null, price, 0,
					false, null, false, 0, false, null, false);
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