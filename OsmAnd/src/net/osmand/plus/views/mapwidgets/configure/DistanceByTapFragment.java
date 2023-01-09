package net.osmand.plus.views.mapwidgets.configure;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		Context context = requireContext();
		themedInflater = UiUtilities.getInflater(context, nightMode);
		View view = themedInflater.inflate(R.layout.distance_by_tap_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		toolbar = view.findViewById(R.id.toolbar);
		navigationIcon = toolbar.findViewById(R.id.close_button);
		toolbarSwitchContainer = toolbar.findViewById(R.id.toolbar_switch_container);
		buttonsCard = view.findViewById(R.id.items_container);

		ImageView imageView = view.findViewById(R.id.descriptionImage);
		imageView.setImageResource(nightMode ? R.drawable.img_distance_by_tap_night : R.drawable.img_distance_by_tap_day);

		setupToolbar();
		setupConfigButtons();

		return view;
	}

	private void setupToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(R.string.map_widget_distance_by_tap);

		updateToolbarNavigationIcon();
		updateToolbarActionButton();
		updateToolbarSwitch();

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
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
		toolbar.findViewById(R.id.action_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				if (activity != null) {
					AndroidUtils.openUrl(activity, R.string.docs_widget_distance_by_tap, nightMode);
				}
			}
		});
	}

	private void updateToolbarSwitch() {
		boolean checked = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);
		int color = checked ? selectedAppMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(color));

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settings.SHOW_DISTANCE_RULER.setModeValue(selectedAppMode, !checked);

				updateToolbarSwitch();
				setupConfigButtons();
			}
		});
	}

	protected void showTextSizeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(requireContext(), nightMode));
		builder.setTitle(R.string.text_size);

		String[] items = new String[DistanceByTapTextSize.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = DistanceByTapTextSize.values()[i].toHumanString(app);
		}
		int selected = settings.DISTANCE_BY_TAP_TEXT_SIZE.get().ordinal();
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, selected, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					settings.DISTANCE_BY_TAP_TEXT_SIZE.set(DistanceByTapTextSize.values()[which]);
					setupConfigButtons();
				}
		);
		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}

	private View createButtonWithState(int iconId,
	                                   @NonNull String title,
	                                   boolean enabled,
	                                   boolean showShortDivider,
	                                   OnClickListener listener) {
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
		stateContainer.setText(settings.DISTANCE_BY_TAP_TEXT_SIZE.get().toHumanString(app));
		stateContainer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(stateContainer, true);

		View button = view.findViewById(R.id.button_container);
		button.setOnClickListener(listener);
		button.setEnabled(enabled);

		setupListItemBackground(view);
		return view;
	}

	private void setupListItemBackground(@NonNull View view) {
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.button_container), background);
	}

	private void setupConfigButtons() {
		buttonsCard.removeAllViews();

		boolean enabled = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);
		buttonsCard.addView(createButtonWithState(
				R.drawable.ic_action_map_text_size,
				getString(R.string.text_size),
				enabled,
				false,
				v -> {
					if (AndroidUtils.isActivityNotDestroyed(getActivity())) {
						showTextSizeDialog();
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
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DistanceByTapFragment fragment = new DistanceByTapFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
