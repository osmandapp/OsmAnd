package net.osmand.plus.search;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidNetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;


public class SendSearchQueryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SendSearchQueryBottomSheet";
	public static final String MISSING_SEARCH_QUERY_KEY = "missing_search_query_key";
	public static final String MISSING_SEARCH_LOCATION_KEY = "missing_search_location_key";

	private Map<String, String> params = new HashMap<>();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args == null) {
			return;
		}
		String searchQuery = args.getString(MISSING_SEARCH_QUERY_KEY);
		String searchLocation = args.getString(MISSING_SEARCH_LOCATION_KEY);
		if (Algorithms.isEmpty(searchQuery)) {
			return;
		}
		params.put("query", searchQuery);
		params.put("location", searchLocation);
		items.add(new TitleItem(getString(R.string.send_search_query)));
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final TextView textView = (TextView) View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.send_missing_search_query_tv, null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			textView.setText(getString(R.string.send_search_query_description, searchQuery));
		} else {
			textView.setText(getString(R.string.send_search_query_description, searchQuery));
		}
		BaseBottomSheetItem sendSearchQueryDescription = new SimpleBottomSheetItem.Builder().setCustomView(textView)
				.create();
		items.add(sendSearchQueryDescription);

	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_send;
	}

	@Override
	protected void onRightBottomButtonClick() {
		final OsmandApplication app = getMyApplication();
		if (app != null) {
			if (!app.getSettings().isInternetConnectionAvailable()) {
				Toast.makeText(app, R.string.internet_not_available, Toast.LENGTH_LONG).show();
				dismiss();
			} else {
				AndroidNetworkUtils.sendRequestAsync(app, "https://osmand.net/api/missing_search", params,
						null, true, true, new AndroidNetworkUtils.OnRequestResultListener() {
							@Override
							public void onResult(String result) {
								if (result != null && isAdded()) {
									try {
										JSONObject obj = new JSONObject(result);
										if (!obj.has("error")) {
											Toast.makeText(app, getString(R.string.thank_you_for_feedback), Toast.LENGTH_SHORT).show();
										} else {
											Toast.makeText(app, MessageFormat.format(getString(R.string.error_message_pattern), obj.getString("error")), Toast.LENGTH_SHORT).show();
										}
									} catch (JSONException e) {

									}
								}
								dismiss();
							}
						});
			}
		}
	}
}
