package net.osmand.plus.nearbyplaces;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import org.apache.commons.logging.Log;

public class NearbyPlacesFragment extends BaseOsmAndFragment {

	public static final String TAG = NearbyPlacesFragment.class.getSimpleName();
	private final Log log = PlatformUtil.getLog(NearbyPlacesFragment.class);

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_nearby_places, container, false);
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view);
		setupToolBar(view);
		return view;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
	}

	private void setupToolBar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitleTextColor(app.getColor(ColorUtilities.getPrimaryTextColorId(nightMode)));
		toolbar.setNavigationIcon(getIcon(R.drawable.ic_arrow_back, ColorUtilities.getPrimaryIconColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			requireActivity().onBackPressed();
			if (mapActivity != null) {
				QuickSearchDialogFragment searchDialog = mapActivity.getFragmentsHelper().getQuickSearchDialogFragment();
				if (searchDialog != null) {
					searchDialog.show();
				}
			}
		});
		toolbar.setBackgroundColor(app.getColor(nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new NearbyPlacesFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}
}