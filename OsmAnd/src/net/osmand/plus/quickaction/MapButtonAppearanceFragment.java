package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.AddQuickActionFragment.QUICK_ACTION_BUTTON_KEY;

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
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MapButtonAppearanceFragment extends BaseOsmAndFragment implements CardListener {

	public static final String TAG = MapButtonAppearanceFragment.class.getSimpleName();

	private List<BaseCard> cards;
	private DialogButton applyButton;
	private QuickActionButton actionButton;
	private QuickActionButtonState buttonState;

	private ButtonAppearanceParams appearanceParams;
	private ButtonAppearanceParams originalAppearanceParams;

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
		String key = args.getString(QUICK_ACTION_BUTTON_KEY);
		if (key != null) {
			buttonState = app.getMapButtonsHelper().getButtonStateById(key);
		}
		if (buttonState != null) {
			appearanceParams = buttonState.createAppearanceParams();
			originalAppearanceParams = buttonState.createAppearanceParams();

			if (savedInstanceState != null) {
				appearanceParams.readBundle(savedInstanceState);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.map_button_appearance_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		setupActionButton(view);
		setupCards(view);
		setupApplyButton(view);

		updateContent();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarSecondaryColor(view.getContext(), nightMode));

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_string_appearance);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(view.getContext(), nightMode));

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		ImageView resetButton = toolbar.findViewById(R.id.action_button);
		resetButton.setOnClickListener(v -> resetAppearance());
		resetButton.setImageDrawable(getContentIcon(R.drawable.ic_action_reset));
		resetButton.setContentDescription(getString(R.string.shared_string_reset));
		AndroidUiHelper.updateVisibility(resetButton, true);
	}

	private void setupActionButton(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.button_container);
		actionButton = container.findViewById(R.id.map_quick_actions_button);
		actionButton.setButtonState(buttonState);
		actionButton.setCustomAppearanceParams(appearanceParams);
		setupButtonBackground(container);
	}

	private void setupButtonBackground(@NonNull View view) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null) {
			MapRenderRepositories maps = app.getResourceManager().getRenderer();
			RenderingRuleSearchRequest request = maps.getSearchRequestWithAppliedCustomRules(renderer, nightMode);
			if (request.searchRenderingAttribute("waterColor")) {
				int color = request.getIntPropertyValue(renderer.PROPS.R_ATTR_COLOR_VALUE);
				if (color != -1) {
					view.setBackgroundColor(color);
				}
			}
		}
	}

	private void setupCards(@NonNull View view) {
		cards = new ArrayList<>();
		MapActivity activity = requireMapActivity();
		ViewGroup container = view.findViewById(R.id.cards_container);
		container.removeAllViews();

		addCard(container, new ButtonIconsCard(activity, buttonState, appearanceParams), false);
		addCard(container, new CornerRadiusCard(activity, appearanceParams), false);
		addCard(container, new ButtonSizeCard(activity, appearanceParams), false);
		addCard(container, new OpacitySliderCard(activity, appearanceParams), true);
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card, boolean lastItem) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build());

		if (!lastItem) {
			container.addView(themedInflater.inflate(R.layout.simple_divider_item, container, false));
		}
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			saveChanges();

			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void saveChanges() {
		buttonState.getSizePref().set(appearanceParams.getSize());
		buttonState.getOpacityPref().set(appearanceParams.getOpacity());
		buttonState.getCornerRadiusPref().set(appearanceParams.getCornerRadius());
		buttonState.getIconPref().set(appearanceParams.getIconName());
	}

	private void updateContent() {
		updateCards();
		updateButtons();
	}

	private void updateCards() {
		for (BaseCard card : cards) {
			card.update();
		}
	}

	private void updateButtons() {
		actionButton.update(nightMode, true);
		applyButton.setEnabled(!Algorithms.objectEquals(originalAppearanceParams, appearanceParams));
	}

	private void resetAppearance() {
		appearanceParams.setIconName(buttonState.getIconPref().getDefaultValue());
		appearanceParams.setSize(buttonState.getSizePref().getDefaultValue());
		appearanceParams.setOpacity(buttonState.getOpacityPref().getDefaultValue());
		appearanceParams.setCornerRadius(buttonState.getCornerRadiusPref().getDefaultValue());
		updateContent();
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		updateButtons();
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

	@Override
	public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
		super.onSaveInstanceState(outState);
		appearanceParams.saveToBundle(outState);
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull QuickActionButtonState buttonState) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(QUICK_ACTION_BUTTON_KEY, buttonState.getId());

			MapButtonAppearanceFragment fragment = new MapButtonAppearanceFragment();
			fragment.setArguments(args);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}