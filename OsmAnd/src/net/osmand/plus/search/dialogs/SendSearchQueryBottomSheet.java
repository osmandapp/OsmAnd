package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class SendSearchQueryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SendSearchQueryBottomSheet.class.getSimpleName();
	public static final String MISSING_SEARCH_QUERY_KEY = "missing_search_query_key";
	public static final String MISSING_SEARCH_LOCATION_KEY = "missing_search_location_key";

	private final Map<String, String> params = new HashMap<>();

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
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		TextView textView = (TextView) View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.send_missing_search_query_tv, null);
		textView.setText(getString(R.string.send_search_query_description, searchQuery));
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
		if (!settings.isInternetConnectionAvailable()) {
			app.showToastMessage(R.string.internet_not_available);
			dismiss();
		} else {
			AndroidNetworkUtils.sendRequestAsync(app, "https://osmand.net/api/missing_search", params,
					null, true, true, (result, error, resultCode) -> {
						if (result != null && isAdded()) {
							try {
								JSONObject obj = new JSONObject(result);
								if (!obj.has("error")) {
									app.showShortToastMessage(R.string.thank_you_for_feedback);
								} else {
									app.showShortToastMessage(R.string.error_message_pattern, obj.getString("error"));
								}
							} catch (JSONException e) {

							}
						}
						dismiss();
					});
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String searchLocation, @NonNull String searchQuery) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(MISSING_SEARCH_LOCATION_KEY, searchLocation);
			args.putString(MISSING_SEARCH_QUERY_KEY, searchQuery);

			SendSearchQueryBottomSheet fragment = new SendSearchQueryBottomSheet();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}
