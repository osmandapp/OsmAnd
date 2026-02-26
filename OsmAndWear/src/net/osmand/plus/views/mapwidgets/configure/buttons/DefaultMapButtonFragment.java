package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.plus.quickaction.MapButtonAppearanceFragment.MAP_BUTTON_KEY;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.CompassVisibilityBottomSheet.CompassVisibilityUpdateListener;
import net.osmand.plus.views.mapwidgets.configure.dialogs.Map3DModeBottomSheet.Map3DModeUpdateListener;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DefaultMapButtonFragment extends BaseOsmAndFragment implements CopyAppModePrefsListener,
		Map3DModeUpdateListener, CompassVisibilityUpdateListener, CardListener {

	public static final String TAG = DefaultMapButtonFragment.class.getSimpleName();

	private List<BaseCard> cards;
	private MapButtonState buttonState;

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = requireArguments();
		String key = args.getString(MAP_BUTTON_KEY);
		if (key != null) {
			buttonState = app.getMapButtonsHelper().getMapButtonStateById(key);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.default_map_button_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		setupCards(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarSecondaryColor(view.getContext(), nightMode));

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(buttonState.getName());
		title.setTextColor(ColorUtilities.getPrimaryTextColor(view.getContext(), nightMode));

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		ImageView resetButton = toolbar.findViewById(R.id.action_button);
		resetButton.setOnClickListener(this::showOptionsMenu);
		resetButton.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));
		resetButton.setContentDescription(getString(R.string.shared_string_more));
		AndroidUiHelper.updateVisibility(resetButton, true);
	}

	private void setupCards(@NonNull View view) {
		cards = new ArrayList<>();
		MapActivity activity = requireMapActivity();
		ViewGroup container = view.findViewById(R.id.cards_container);
		container.removeAllViews();

		addCard(container, new MapButtonCard(activity, buttonState, null));
		addCard(container, new MapButtonVisibilityCard(activity, buttonState, this));
		container.addView(themedInflater.inflate(R.layout.list_item_divider, container, false));
		addCard(container, new MapButtonAppearanceCard(activity, buttonState));
		container.addView(themedInflater.inflate(R.layout.card_bottom_divider, container, false));
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build());
	}

	private void updateCards() {
		for (BaseCard card : cards) {
			card.update();
		}
	}

	private void showOptionsMenu(@NonNull View view) {
		Context context = view.getContext();
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(context)
				.setTitleId(R.string.reset_to_default)
				.setIcon(getContentIcon(R.drawable.ic_action_reset))
				.setOnClickListener(v -> resetToDefault()).create());

		items.add(new PopUpMenuItem.Builder(context)
				.setTitleId(R.string.copy_from_other_profile)
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						ApplicationMode appMode = settings.getApplicationMode();
						FragmentManager manager = activity.getSupportFragmentManager();
						SelectCopyAppModeBottomSheet.showInstance(manager, this, appMode);
					}
				}).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void resetToDefault() {
		buttonState.resetToDefault(settings.getApplicationMode());
		updateCards();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode fromMode) {
		buttonState.copyForMode(fromMode, settings.getApplicationMode());
		updateCards();
	}

	@Override
	public void onMap3DModeUpdated(@NonNull Map3DModeVisibility visibility) {
		updateCards();
	}

	@Override
	public void onCompassVisibilityUpdated(@NonNull CompassVisibility visibility) {
		updateCards();
	}

	@Override
	public void onCardPressed(@NonNull @NotNull BaseCard card) {
		updateCards();
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.enableDrawer();
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull MapButtonState buttonState) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(MAP_BUTTON_KEY, buttonState.getId());

			DefaultMapButtonFragment fragment = new DefaultMapButtonFragment();
			fragment.setArguments(args);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}