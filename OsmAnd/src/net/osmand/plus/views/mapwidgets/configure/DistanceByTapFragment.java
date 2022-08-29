package net.osmand.plus.views.mapwidgets.configure;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.DistanceByTapTextSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;

public class DistanceByTapFragment extends BaseOsmAndFragment {
	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private Toolbar toolbar;
	private View toolbarSwitchContainer;
	private ImageView navigationIcon;

	private LayoutInflater themedInflater;
	private ApplicationMode selectedAppMode;
	private LinearLayout buttonsCard;

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = getMyApplication();
		settings = getSettings();
		nightMode = isNightMode(false);
		selectedAppMode = settings.getApplicationMode();
	}

	private void setUpToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		TextView tvSubTitle = toolbar.findViewById(R.id.toolbar_subtitle);
		tvSubTitle.setVisibility(View.GONE);
		tvTitle.setText(getString(R.string.map_widget_distance_by_tap));

		updateToolbarNavigationIcon();
		updateToolbarActionButton();
		updateToolbarSwitch(settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode));
	}

	private void updateToolbarNavigationIcon() {
		navigationIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
	}

	private void updateToolbarActionButton() {
		ImageButton iconHelpContainer = toolbar.findViewById(R.id.action_button);
		iconHelpContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				if (activity != null) {
					String docsUrl = getString(R.string.docs_widget_distance_by_tap);
					AndroidUtils.openUrl(activity, docsUrl, nightMode);
				}
			}
		});
	}

	private void updateToolbarSwitch(boolean isChecked) {
		int color = isChecked ? selectedAppMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(color));

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(isChecked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity mapActivity = (MapActivity) requireMyActivity();
				settings.SHOW_DISTANCE_RULER.setModeValue(selectedAppMode, !isChecked);
				mapActivity.updateApplicationModeSettings();

				boolean visible = !isChecked;
				updateToolbarSwitch(visible);
				setupConfigButtons();
			}
		});

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(isChecked ? R.string.shared_string_on : R.string.shared_string_off);
	}


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		View view = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.distance_by_tap_fragment, container, false);

		AndroidUtils.addStatusBarPadding21v(requireContext(), view);
		toolbar = view.findViewById(R.id.toolbar);
		navigationIcon = toolbar.findViewById(R.id.close_button);
		toolbarSwitchContainer = toolbar.findViewById(R.id.toolbar_switch_container);
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		buttonsCard = view.findViewById(R.id.items_container);

		AppCompatImageView imageView = view.findViewById(R.id.descriptionImage);
		imageView.setImageResource(nightMode ? R.drawable.img_distance_by_tap_night : R.drawable.img_distance_by_tap_day);

		setUpToolbar();
		setupConfigButtons();

		return view;
	}

	protected void showTextSizeDialog(MapActivity activity, int themeRes, boolean nightMode) {
		int selectedProfileColor = app.getSettings().APPLICATION_MODE.get().getProfileColor(nightMode);

		OsmandMapTileView mapView = activity.getMapView();
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		builder.setTitle(R.string.text_size);

		String[] items = new String[DistanceByTapTextSize.values().length];
		if (app != null) {
			for (int index = 0; index < items.length; index++) {
				items[index] = DistanceByTapTextSize.values()[index].toHumanString(app);
			}
		}
		int selected = mapView.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get().ordinal();

		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, selected, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					mapView.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.set(DistanceByTapTextSize.values()[which]);
					setupConfigButtons();
					if (mapView.getMapRenderer() == null) {
						activity.refreshMapComplete();
					}
				}
		);

		builder.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(builder.show());
	}

	@StyleRes
	protected static int getThemeRes(boolean nightMode) {
		return nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	private View createButtonWithState(int iconId,
	                                   @NonNull String title,
	                                   boolean enabled,
	                                   boolean showShortDivider,
	                                   MapActivity activity,
	                                   OnClickListener listener) {
		OsmandMapTileView mapView = activity.getMapView();
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);


		Drawable icon = getPaintedContentIcon(iconId, enabled
				? ColorUtilities.getDefaultIconColor(app, nightMode)
				: ColorUtilities.getSecondaryIconColor(app, nightMode));
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);
		tvTitle.setTextColor(enabled
				? ColorUtilities.getPrimaryTextColor(app, nightMode)
				: ColorUtilities.getSecondaryTextColor(app, nightMode));

		if (showShortDivider) {
			view.findViewById(R.id.short_divider).setVisibility(View.VISIBLE);
		}

		TextView stateContainer = view.findViewById(R.id.items_count_descr);

		int selected = mapView.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get().ordinal();
		String textSize = "";
		if (app != null) {
			textSize = DistanceByTapTextSize.values()[selected].toHumanString(app);
		}

		stateContainer.setText(textSize);
		stateContainer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(stateContainer, true);


		View button = view.findViewById(R.id.button_container);
		enableDisableView(button, enabled, listener);

		setupListItemBackground(view);
		return view;
	}

	private void enableDisableView(View view, boolean enabled, @Nullable OnClickListener listener) {
		if (enabled) {
			view.setOnClickListener(listener);
		} else {
			view.setEnabled(enabled);
		}
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(button, background);
	}


	private void setupConfigButtons() {
		buttonsCard.removeAllViews();
		boolean distanceByTapEnabled = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);

		buttonsCard.addView(createButtonWithState(
				R.drawable.ic_action_map_text_size,
				getString(R.string.text_size),
				distanceByTapEnabled,
				false,
				(MapActivity) requireActivity(),
				v -> {
					if (AndroidUtils.isActivityNotDestroyed(requireActivity())) {
						int themeRes = getThemeRes(nightMode);
						showTextSizeDialog((MapActivity) requireActivity(), themeRes, nightMode);
					}
				}
		));
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMapActivity().enableDrawer();
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		showInstance(activity, false);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                boolean animate) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			DistanceByTapFragment fragment = new DistanceByTapFragment();
			fm.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
