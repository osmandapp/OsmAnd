package net.osmand.plus.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.dashboard.tools.DashFragmentData;

public class DashSearchFragment extends DashBaseFragment {
	public static final String TAG = "DASH_SEARCH_FRAGMENT";
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return R.string.search_for;
				}
			};

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_search_fragment, container, false);

		TextView searchFor = view.findViewById(R.id.search_for);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(searchFor,
				getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_search_dark), null, null, null);
		searchFor.setCompoundDrawablePadding(AndroidUtils.dpToPx(getActivity(), 16f));

		view.findViewById(R.id.search_card).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((MapActivity) getActivity()).getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW, false);
				closeDashboard();
			}
		});

		return view;
	}

	@Override
	public void onOpenDash() {
	}
}
