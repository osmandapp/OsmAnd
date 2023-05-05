package net.osmand.plus.views.mapwidgets.configure;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class Map3DModeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = Map3DModeBottomSheet.class.getSimpleName();

	private static final String APP_MODE_KEY = "app_mode";

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
		View view = inflater.inflate(R.layout.fragment_map_3d_mode_bottom_sheet_dialog, null);

		setupVisibilityItem(Map3DModeVisibility.HIDDEN, view);
		setupVisibilityItem(Map3DModeVisibility.VISIBLE, view);
		setupVisibilityItem(Map3DModeVisibility.VISIBLE_IN_3D_MODE, view);

		return view;
	}

	private void setupVisibilityItem(@NonNull Map3DModeVisibility itemVisibility, @NonNull View view) {
		boolean selected = itemVisibility == settings.MAP_3D_MODE_VISIBILITY.getModeValue(appMode);

		View container = view.findViewById(itemVisibility.containerId);
		ImageView ivIcon = container.findViewById(R.id.icon);
		TextView tvTitle = container.findViewById(R.id.title);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		ivIcon.setImageDrawable(getIconForVisibility(itemVisibility));
		tvTitle.setText(itemVisibility.titleId);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
		compoundButton.setChecked(selected);

		container.setOnClickListener(v -> {
			settings.MAP_3D_MODE_VISIBILITY.setModeValue(appMode, itemVisibility);
			Fragment targetFragment = getTargetFragment();
			if (targetFragment instanceof ConfigureScreenFragment) {
				((ConfigureScreenFragment) targetFragment).setupButtonsCard();
			}
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				MapActivity mapActivity = (MapActivity) activity;
				mapActivity.getMapLayers().updateLayers(mapActivity);
			}
			dismiss();
		});
		container.setBackground(UiUtilities.getColoredSelectableDrawable(app, ColorUtilities.getActiveColor(app, nightMode)));
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

	public enum Map3DModeVisibility {

		HIDDEN(R.string.shared_string_hidden, R.drawable.ic_action_button_3d_off, R.id.always_hidden_mode),
		VISIBLE(R.string.shared_string_visible, R.drawable.ic_action_button_3d, R.id.always_visible_mode),
		VISIBLE_IN_3D_MODE(R.string.visible_in_3d_mode, R.drawable.ic_action_button_3d, R.id.visible_in_3d_mode);

		@StringRes
		public final int titleId;
		@DrawableRes
		public final int iconId;
		@IdRes
		public final int containerId;

		Map3DModeVisibility(@StringRes int titleId, @DrawableRes int iconId, @IdRes int containerId) {
			this.titleId = titleId;
			this.iconId = iconId;
			this.containerId = containerId;
		}

		@NonNull
		public String getTitle(@NonNull Context context) {
			return context.getString(titleId);
		}
	}
}