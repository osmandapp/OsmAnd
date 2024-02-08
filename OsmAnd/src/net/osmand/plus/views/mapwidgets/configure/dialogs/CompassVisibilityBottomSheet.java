package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;

public class CompassVisibilityBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = CompassVisibilityBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private ApplicationMode appMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		settings = app.getSettings();

		Bundle args = getArguments();
		if (savedInstanceState != null) {
			restoreAppMode(savedInstanceState);
		} else if (args != null) {
			restoreAppMode(args);
		}
	}

	private void restoreAppMode(@NonNull Bundle bundle) {
		appMode = ApplicationMode.valueOfStringKey(bundle.getString(APP_MODE_KEY), settings.getApplicationMode());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(createView())
				.create());
	}

	@NonNull
	private View createView() {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		View view = inflater.inflate(R.layout.fragment_compass_visibility_bottom_sheet_dialog, null);

		setupVisibilityItem(CompassVisibility.ALWAYS_VISIBLE, view);
		setupVisibilityItem(CompassVisibility.ALWAYS_HIDDEN, view);
		setupVisibilityItem(CompassVisibility.VISIBLE_IF_MAP_ROTATED, view);

		return view;
	}

	private void setupVisibilityItem(@NonNull CompassVisibility itemVisibility, @NonNull View view) {
		boolean selected = itemVisibility == settings.COMPASS_VISIBILITY.getModeValue(appMode);

		View container = view.findViewById(itemVisibility.containerId);
		ImageView ivIcon = container.findViewById(R.id.icon);
		TextView tvTitle = container.findViewById(R.id.title);
		TextView tvDesc = container.findViewById(R.id.desc);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		ivIcon.setImageDrawable(getIconForVisibility(itemVisibility));
		tvTitle.setText(itemVisibility.titleId);
		if (itemVisibility.descId != 0) {
			tvDesc.setText(itemVisibility.descId);
			AndroidUiHelper.updateVisibility(tvDesc, true);
		}

		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
		compoundButton.setChecked(selected);

		container.setOnClickListener(v -> {
			settings.COMPASS_VISIBILITY.setModeValue(appMode, itemVisibility);
			Fragment target = getTargetFragment();
			if (target instanceof CompassVisibilityUpdateListener) {
				((CompassVisibilityUpdateListener) target).onCompassVisibilityUpdated(itemVisibility);
			}
			dismiss();
		});
		container.setBackground(UiUtilities.getColoredSelectableDrawable(app, ColorUtilities.getActiveColor(app, nightMode)));
	}

	@Nullable
	private Drawable getIconForVisibility(@NonNull CompassVisibility visibility) {
		boolean selected = visibility == settings.COMPASS_VISIBILITY.getModeValue(appMode);
		int iconColorId = selected
				? ColorUtilities.getActiveIconColorId(nightMode)
				: ColorUtilities.getDefaultIconColorId(nightMode);
		return getIcon(visibility.iconId, iconColorId);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @NonNull ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(APP_MODE_KEY, appMode.getStringKey());

			CompassVisibilityBottomSheet fragment = new CompassVisibilityBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface CompassVisibilityUpdateListener {

		void onCompassVisibilityUpdated(@NonNull CompassVisibility visibility);
	}
}