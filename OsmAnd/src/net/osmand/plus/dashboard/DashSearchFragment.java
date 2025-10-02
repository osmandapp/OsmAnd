package net.osmand.plus.dashboard;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.dashboard.tools.DashFragmentData;

public class DashSearchFragment extends DashBaseFragment {
	public static final String TAG = "DASH_SEARCH_FRAGMENT";
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return R.string.search_for;
				}
			};

	@Override
	public View initView(@Nullable ViewGroup container, @Nullable Bundle savedState) {
		View view = inflate(R.layout.dash_search_fragment, container, false);

		TextView tvSearchFor = view.findViewById(R.id.search_for);
		Drawable iconSearch = uiUtilities.getThemedIcon(R.drawable.ic_action_search_dark);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
				tvSearchFor, iconSearch, null, null, null);
		tvSearchFor.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 16f));

		view.findViewById(R.id.search_card).setOnClickListener(v -> {
			requireMapActivity().getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW, false);
			closeDashboard();
		});
		return view;
	}
}
