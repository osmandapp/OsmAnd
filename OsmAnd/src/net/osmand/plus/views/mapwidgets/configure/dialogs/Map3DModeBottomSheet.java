package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class Map3DModeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = Map3DModeBottomSheet.class.getSimpleName();

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
		View view = inflater.inflate(R.layout.fragment_map_3d_mode_bottom_sheet, null);
		LinearLayout itemsContainer = view.findViewById(R.id.items_container);

		for (Map3DModeVisibility visibility : Map3DModeVisibility.values()) {
			itemsContainer.addView(createVisibilityItem(visibility, inflater));
		}

		return view;
	}

	private View createVisibilityItem(@NonNull Map3DModeVisibility itemVisibility, LayoutInflater inflater) {
		View item = inflater.inflate(R.layout.compass_visibility_item, null);
		ImageView ivIcon = item.findViewById(R.id.icon);
		TextView tvTitle = item.findViewById(R.id.title);
		CompoundButton compoundButton = item.findViewById(R.id.compound_button);

		boolean selected = itemVisibility == settings.MAP_3D_MODE_VISIBILITY.getModeValue(appMode);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
		compoundButton.setChecked(selected);
		ivIcon.setImageDrawable(getIconForVisibility(itemVisibility));
		tvTitle.setText(itemVisibility.titleId);

		item.setOnClickListener(v -> {
			settings.MAP_3D_MODE_VISIBILITY.setModeValue(appMode, itemVisibility);
			Fragment targetFragment = getTargetFragment();
			if (targetFragment instanceof Map3DModeUpdateListener) {
				((Map3DModeUpdateListener) targetFragment).onMap3DModeUpdated(itemVisibility);
			}
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				mapActivity.getMapLayers().updateLayers(mapActivity);
			}
			dismiss();
		});
		item.setBackground(UiUtilities.getColoredSelectableDrawable(app, ColorUtilities.getActiveColor(app, nightMode)));

		return item;
	}

	@Nullable
	private Drawable getIconForVisibility(@NonNull Map3DModeVisibility visibility) {
		boolean selected = visibility == settings.MAP_3D_MODE_VISIBILITY.getModeValue(appMode);
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
			Map3DModeBottomSheet fragment = new Map3DModeBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.setUsedOnMap(false);
			Bundle args = new Bundle();
			args.putString(APP_MODE_KEY, appMode.getStringKey());
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}


	public interface Map3DModeUpdateListener {

		void onMap3DModeUpdated(@NonNull Map3DModeVisibility visibility);
	}
}