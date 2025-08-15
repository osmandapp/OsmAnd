package net.osmand.plus.firstusage;

import static net.osmand.plus.firstusage.FirstUsageAction.DETERMINE_LOCATION;
import static net.osmand.plus.firstusage.FirstUsageAction.SELECT_COUNTRY;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class FirstUsageLocationBottomSheet extends BaseFirstUsageBottomSheet {

	private static final String TAG = FirstUsageLocationBottomSheet.class.getSimpleName();

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_location);
	}

	@Override
	protected void setupItems(@NonNull ViewGroup container) {
		container.addView(createItemView(getString(R.string.search_another_country), R.drawable.ic_show_on_map, view -> {
			processActionClick(SELECT_COUNTRY);
			dismiss();
		}));

		container.addView(createItemView(getString(R.string.determine_location), R.drawable.ic_action_marker_dark, view -> {
			processActionClick(DETERMINE_LOCATION);
			dismiss();
		}));
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull Fragment target) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FirstUsageLocationBottomSheet fragment = new FirstUsageLocationBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}