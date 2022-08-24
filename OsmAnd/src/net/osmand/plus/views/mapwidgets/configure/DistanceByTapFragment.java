package net.osmand.plus.views.mapwidgets.configure;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

import java.util.ArrayList;

public class DistanceByTapFragment extends BaseOsmAndFragment {
	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private static ArrayList<DistanceByTapUpdateListener> listeners = new ArrayList<>();

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

		ApplicationMode appMode = requireMyApplication().getSettings().getApplicationMode();

		updateToolbarNavigationIcon();
		updateToolbarActionButton();
		updateToolbarSwitch(settings.SHOW_DISTANCE_RULER.getModeValue(appMode));
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
					AndroidUtils.openUrl(activity, Uri.parse(docsUrl), nightMode);
				}
			}
		});
	}

	private void updateToolbarSwitch(boolean isChecked) {
		OsmandApplication app = requireMyApplication();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		int color = isChecked ? appMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(color));

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(isChecked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity mapActivity = (MapActivity) requireMyActivity();
				settings.SHOW_DISTANCE_RULER.setModeValue(appMode, !isChecked);
				mapActivity.updateApplicationModeSettings();

				boolean visible = !isChecked;
				updateToolbarSwitch(visible);
				setupConfigButtons();
				updateListeners();
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
		buttonsCard = view.findViewById(R.id.linear_layout);

		AppCompatImageView imageView = view.findViewById(R.id.descriptionImage);
		imageView.setImageResource(nightMode ? R.drawable.img_distance_by_tap_night : R.drawable.img_distance_by_tap_day);

		TextView hintTitle = view.findViewById(R.id.titleHint);
		hintTitle.setText(R.string.how_to_use);
		int textColor = ColorUtilities.getPrimaryTextColorId(nightMode);
		hintTitle.setTextColor(getResources().getColor(textColor, app.getTheme()));

		TextView hintDescription = view.findViewById(R.id.descriptionHint);
		hintDescription.setText(R.string.distance_by_tap_use_description);

		setUpToolbar();
		setupConfigButtons();

		return view;
	}

	protected void showTextSizeDialog(MapActivity activity, int themeRes, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		OsmandMapTileView view = activity.getMapView();
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		builder.setTitle(R.string.text_size);

		String[] items = new String[DistanceByTapTextSize.values().length];
		for (int index = 0; index < items.length; index++) {
			items[index] = DistanceByTapTextSize.values()[index].toHumanString(app);
		}
		int selected = view.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get().ordinal();

		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, selected, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					view.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.set(DistanceByTapTextSize.values()[which]);
					setupConfigButtons();
					if (view.getMapRenderer() == null) {
						activity.refreshMapComplete();
					}
					updateListeners();
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

		int iconColor = enabled
				? ColorUtilities.getDefaultIconColorId(nightMode)
				: ColorUtilities.getSecondaryIconColorId(nightMode);
		Drawable icon = getPaintedContentIcon(iconId, getResources().getColor(iconColor, app.getTheme()));
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);

		int textColor = enabled
				? ColorUtilities.getPrimaryTextColorId(nightMode)
				: ColorUtilities.getSecondaryTextColorId(nightMode);
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);
		tvTitle.setTextColor(getResources().getColor(textColor, app.getTheme()));

		if (showShortDivider) {
			view.findViewById(R.id.short_divider).setVisibility(View.VISIBLE);
		}

		TextView stateContainer = view.findViewById(R.id.items_count_descr);

		int selected = mapView.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get().ordinal();
		String textSize = DistanceByTapTextSize.values()[selected].toHumanString(app);

		stateContainer.setText(textSize);
		stateContainer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(stateContainer, true);

		setupClickListener(view, listener);
		setupListItemBackground(view);
		return view;
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	private void setupClickListener(@NonNull View view, @Nullable OnClickListener listener) {
		View button = view.findViewById(R.id.button_container);
		button.setOnClickListener(listener);
	}

	private void setupConfigButtons() {
		buttonsCard.removeAllViews();

		buttonsCard.addView(createButtonWithState(
				R.drawable.ic_action_map_text_size,
				getString(R.string.text_size),
				settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode),
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
			int slideInAnim = 0;
			int slideOutAnim = 0;
			OsmandApplication app = ((OsmandApplication) activity.getApplication());
			if (animate && !app.getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				slideInAnim = R.anim.slide_in_bottom;
				slideOutAnim = R.anim.slide_out_bottom;
			}

			DistanceByTapFragment fragment = new DistanceByTapFragment();
			fm.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	private void updateListeners() {
		for (DistanceByTapUpdateListener listener : listeners) {
			listener.onDistanceByTapUpdated();
		}
	}

	public static void registerListener(DistanceByTapUpdateListener listener) {
		listeners.add(listener);
	}

	public static void unRegisterListener(DistanceByTapUpdateListener listener) {
		listeners.remove(listener);
	}

	public interface DistanceByTapUpdateListener {
		void onDistanceByTapUpdated();
	}
}
