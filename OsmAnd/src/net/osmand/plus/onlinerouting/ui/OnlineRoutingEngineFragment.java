package net.osmand.plus.onlinerouting.ui;

import static net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.CUSTOM_VEHICLE;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.DERIVED_PROFILE_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingUtils;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.profiles.SelectOnlineApproxProfileBottomSheet;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineRoutingEngineFragment extends BaseFullScreenFragment implements OnSelectProfileCallback {


	public static final String TAG = OnlineRoutingEngineFragment.class.getSimpleName();

	private static final String ENGINE_TYPE_KEY = "engine_type";
	private static final String ENGINE_CUSTOM_VEHICLE_KEY = "engine_custom_vehicle";
	private static final String EXAMPLE_LOCATION_KEY = "example_location";
	private static final String EDITED_ENGINE_KEY = "edited_engine_key";

	private String approxRouteProfile;
	private String approxDerivedProfile;
	private MapActivity mapActivity;
	private OnlineRoutingHelper helper;

	private View view;
	private ViewGroup segmentsContainer;
	private OnlineRoutingCard nameCard;
	private OnlineRoutingCard typeCard;
	private OnlineRoutingCard vehicleCard;
	private OnlineRoutingCard apiKeyCard;
	private OnlineRoutingCard approximateCard;
	private OnlineRoutingCard useExternalTimestampsCard;
	private OnlineRoutingCard routingFallbackCard;
	private OnlineRoutingCard exampleCard;
	private View testResultsContainer;
	private DialogButton saveButton;
	private ScrollView scrollView;
	private AppCompatImageView buttonsShadow;
	private OnGlobalLayoutListener onGlobalLayout;
	private OnScrollChangedListener onScroll;
	private boolean isKeyboardShown;

	private OnlineRoutingEngine engine;
	private OnlineRoutingEngine initEngine;
	private String customVehicleKey;
	private ExampleLocation selectedLocation;
	private String editedEngineKey;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapActivity = getMapActivity();
		helper = app.getOnlineRoutingHelper();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {
			initState();
		}
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					showExitDialog();
				}
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.online_routing_engine_fragment, container, false);
		segmentsContainer = view.findViewById(R.id.segments_container);
		scrollView = view.findViewById(R.id.segments_scroll);
		buttonsShadow = view.findViewById(R.id.buttons_shadow);

		AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		setupToolbar(view.findViewById(R.id.toolbar));

		setupNameCard();
		setupTypeCard();
		setupVehicleCard();
		setupApproximateCard();
		setupExternalTimestampsCard();
		setupRoutingFallbackCard();
		setupApiKeyCard();
		setupExampleCard();
		setupResultsContainer();
		setupButtons();

		generateUniqueNameIfNeeded();
		updateCardViews(nameCard, typeCard, vehicleCard, exampleCard);

		showShadowBelowButtons();
		if (!InsetsUtils.isEdgeToEdgeSupported()) {
			showButtonsAboveKeyboard();
		}
		hideKeyboardOnScroll();

		return view;
	}

	private void setupToolbar(Toolbar toolbar) {
		ImageView navigationIcon = toolbar.findViewById(R.id.close_button);
		navigationIcon.setImageResource(R.drawable.ic_action_close);
		navigationIcon.setOnClickListener(v -> showExitDialog());
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);
		View actionBtn = toolbar.findViewById(R.id.action_button);
		if (isEditingMode()) {
			title.setText(getString(R.string.edit_online_routing_engine));
			ImageView ivBtn = toolbar.findViewById(R.id.action_button_icon);
			ivBtn.setImageDrawable(
					getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete));
			actionBtn.setOnClickListener(v -> delete(mapActivity));
		} else {
			title.setText(getString(R.string.add_online_routing_engine));
			actionBtn.setVisibility(View.GONE);
		}
	}

	private void setupNameCard() {
		nameCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		nameCard.build(mapActivity);
		nameCard.setDescription(getString(R.string.select_nav_profile_dialog_message));
		nameCard.setEditedText(engine.getName(app));
		nameCard.setFieldBoxLabelText(getString(R.string.shared_string_name));
		nameCard.setOnTextChangedListener((changedByUser, text) -> {
			if (changedByUser) {
				engine.put(EngineParameter.CUSTOM_NAME, text);
				engine.remove(EngineParameter.NAME_INDEX);
				checkCustomNameUnique(engine);
			}
		});
		nameCard.showDivider();
		segmentsContainer.addView(nameCard.getView());
	}

	private void setupTypeCard() {
		typeCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		typeCard.build(mapActivity);
		typeCard.setHeaderTitle(getString(R.string.shared_string_type));
		List<net.osmand.plus.widgets.chips.ChipItem> typeItems = new ArrayList<>();
		for (OnlineRoutingEngine type : EngineType.values()) {
			String title = type.getTitle();
			ChipItem item = new ChipItem(title);
			item.title = title;
			item.contentDescription = title;
			item.tag = type;
			typeItems.add(item);
		}
		typeCard.setSelectionMenu(typeItems, engine.getType().getTitle(),
				result -> {
					OnlineRoutingEngine type = (OnlineRoutingEngine) result.tag;
					if (engine.getType() != type) {
						changeEngineType(type);
						return true;
					}
					return false;
				});
		typeCard.setOnTextChangedListener((editedByUser, text) -> {
			if (editedByUser) {
				engine.put(EngineParameter.CUSTOM_URL, text);
				updateCardViews(exampleCard);
			}
		});
		typeCard.setFieldBoxLabelText(getString(R.string.shared_string_server_url));
		typeCard.showDivider();
		segmentsContainer.addView(typeCard.getView());
	}

	private void setupVehicleCard() {
		vehicleCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		vehicleCard.build(mapActivity);
		vehicleCard.setHeaderTitle(getString(R.string.shared_string_vehicle));
		vehicleCard.setFieldBoxLabelText(getString(R.string.shared_string_custom));
		vehicleCard.setOnTextChangedListener((editedByUser, text) -> {
			if (editedByUser) {
				customVehicleKey = text;
				engine.put(EngineParameter.VEHICLE_KEY, customVehicleKey);
				updateCardViews(nameCard, exampleCard);
			}
		});
		vehicleCard.setEditedText(customVehicleKey);
		vehicleCard.setFieldBoxHelperText(getString(R.string.shared_string_enter_param));
		vehicleCard.showDivider();
		segmentsContainer.addView(vehicleCard.getView());
		setupVehicleTypes();
	}

	private void setupVehicleTypes() {
		List<ChipItem> vehicleItems = new ArrayList<>();
		for (VehicleType vehicle : engine.getAllowedVehicles()) {
			String title = vehicle.getTitle(app);
			ChipItem item = new ChipItem(title);
			item.title = title;
			item.contentDescription = title;
			item.tag = vehicle;
			vehicleItems.add(item);
		}
		vehicleCard.setSelectionMenu(vehicleItems, engine.getSelectedVehicleType().getTitle(app),
				result -> {
					VehicleType vehicle = (VehicleType) result.tag;
					if (!Algorithms.objectEquals(engine.getSelectedVehicleType(), vehicle)) {
						String vehicleKey = vehicle.equals(CUSTOM_VEHICLE) ? customVehicleKey : vehicle.getKey();
						engine.put(EngineParameter.VEHICLE_KEY, vehicleKey);
						generateUniqueNameIfNeeded();
						updateCardViews(nameCard, vehicleCard, exampleCard);
						return true;
					}
					return false;
				});
	}

	private void setupApproximateCard() {
		approximateCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		approximateCard.build(mapActivity);
		setApproximateCardTitle();
		approximateCard.onClickCheckBox(getString(R.string.attach_roads_descr), result -> {
			if (getActivity() != null) {
				SelectOnlineApproxProfileBottomSheet.showInstance(getActivity(), this,
						appMode, approxRouteProfile, approxDerivedProfile, false);
			}
			return false;
		});
		approximateCard.showDivider();
		segmentsContainer.addView(approximateCard.getView());
	}

	private void setApproximateCardTitle() {
		approxRouteProfile = engine.getApproximationRoutingProfile();
		approxDerivedProfile = engine.get(EngineParameter.APPROXIMATION_DERIVED_PROFILE);
		String appModeName = "";
		if (approxRouteProfile != null) {
			appModeName = approxDerivedProfile != null ? approxDerivedProfile : approxRouteProfile;
			appModeName = " (" + appModeName + ")";
		}
		String title = getString(R.string.attach_to_the_roads) + appModeName;
		approximateCard.setHeaderTitle(title);
		approximateCard.setCheckBox(approxRouteProfile != null);
	}

	private void setupExternalTimestampsCard() {
		useExternalTimestampsCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		useExternalTimestampsCard.build(mapActivity);
		useExternalTimestampsCard.setHeaderTitle(getString(R.string.use_external_timestamps));
		useExternalTimestampsCard.setCheckBox(getString(R.string.use_external_timestamps_description), engine.useExternalTimestamps(), result -> {
			engine.put(EngineParameter.USE_EXTERNAL_TIMESTAMPS, String.valueOf(result));
			return false;
		});
		useExternalTimestampsCard.showDivider();
		segmentsContainer.addView(useExternalTimestampsCard.getView());
	}

	private void setupRoutingFallbackCard() {
		routingFallbackCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		routingFallbackCard.build(mapActivity);
		routingFallbackCard.setHeaderTitle(getString(R.string.use_routing_fallback));
		routingFallbackCard.setCheckBox(getString(R.string.use_routing_fallback_description), engine.useRoutingFallback(), result -> {
			engine.put(EngineParameter.USE_ROUTING_FALLBACK, String.valueOf(result));
			return false;
		});
		routingFallbackCard.showDivider();
		segmentsContainer.addView(routingFallbackCard.getView());
	}

	private void setupApiKeyCard() {
		apiKeyCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		apiKeyCard.build(mapActivity);
		apiKeyCard.setHeaderTitle(getString(R.string.shared_string_api_key));
		apiKeyCard.setFieldBoxLabelText(getString(R.string.keep_it_empty_if_not));
		String apiKey = engine.get(EngineParameter.API_KEY);
		if (apiKey != null) {
			apiKeyCard.setEditedText(apiKey);
		}
		apiKeyCard.showDivider();
		apiKeyCard.setOnTextChangedListener((editedByUser, text) -> {
			if (Algorithms.isBlank(text)) {
				engine.remove(EngineParameter.API_KEY);
			} else {
				engine.put(EngineParameter.API_KEY, text);
			}
			updateCardViews(exampleCard);
		});
		segmentsContainer.addView(apiKeyCard.getView());
	}

	private void setupExampleCard() {
		exampleCard = new OnlineRoutingCard(mapActivity, isNightMode(), appMode);
		exampleCard.build(mapActivity);
		exampleCard.setHeaderTitle(getString(R.string.shared_string_example));
		exampleCard.hideFieldBoxLabel();
		List<ChipItem> locationItems = new ArrayList<>();
		for (ExampleLocation location : ExampleLocation.values()) {
			String title = location.getName();
			ChipItem item = new ChipItem(title);
			item.title = title;
			item.contentDescription = title;
			item.tag = location;
			locationItems.add(item);
		}
		exampleCard.setSelectionMenu(locationItems, selectedLocation.getName(),
				result -> {
					ExampleLocation location = (ExampleLocation) result.tag;
					if (selectedLocation != location) {
						selectedLocation = location;
						updateCardViews(exampleCard);
						return true;
					}
					return false;
				});
		exampleCard.setDescription(getString(R.string.online_routing_example_hint));
		exampleCard.showFieldBox();
		exampleCard.setButton(getString(R.string.test_route_calculation), v -> testEngineWork());
		segmentsContainer.addView(exampleCard.getView());
	}

	private void setupResultsContainer() {
		testResultsContainer = inflate(R.layout.bottom_sheet_item_with_descr_64dp, segmentsContainer, false);
		testResultsContainer.setVisibility(View.GONE);
		segmentsContainer.addView(testResultsContainer);
	}

	private void setupButtons() {
		boolean nightMode = isNightMode();
		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> showExitDialog());

		view.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

		saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setButtonType(DialogButtonType.PRIMARY);
		saveButton.setTitleId(R.string.shared_string_save);
		saveButton.setVisibility(View.VISIBLE);
		saveButton.setOnClickListener(v -> {
			onSaveEngine();
			dismiss();
		});
	}

	private void changeEngineType(OnlineRoutingEngine type) {
		OnlineRoutingEngine tmp = (OnlineRoutingEngine) engine.clone();
		engine = type.newInstance(tmp.getParams());

		// after changing the type, select the vehicle
		// with the same name that was selected before
		VehicleType previous = tmp.getSelectedVehicleType();
		VehicleType next = null;
		for (VehicleType vt : engine.getAllowedVehicles()) {
			if (Algorithms.objectEquals(previous.getTitle(app), vt.getTitle(app))) {
				next = vt;
				break;
			}
		}
		String vehicleKey;
		if (next != null) {
			vehicleKey = next.equals(CUSTOM_VEHICLE) ? customVehicleKey : next.getKey();
		} else {
			vehicleKey = engine.getAllowedVehicles().get(0).getKey();
		}
		engine.put(EngineParameter.VEHICLE_KEY, vehicleKey);

		setupVehicleTypes();
		generateUniqueNameIfNeeded();
		updateCardViews(nameCard, typeCard, vehicleCard, exampleCard);
	}

	private void generateUniqueNameIfNeeded() {
		if (engine.get(EngineParameter.CUSTOM_NAME) == null) {
			List<OnlineRoutingEngine> cachedEngines = helper.getEnginesExceptMentionedKeys(editedEngineKey);
			OnlineRoutingUtils.generateUniqueName(app, engine, cachedEngines);
		}
	}

	private void checkCustomNameUnique(@NonNull OnlineRoutingEngine engine) {
		if (hasNameDuplicate(engine)) {
			nameCard.showFieldBoxError(getString(R.string.message_name_is_already_exists));
			saveButton.setEnabled(false);
		} else {
			nameCard.hideFieldBoxError();
			saveButton.setEnabled(true);
		}
	}

	private boolean hasNameDuplicate(@NonNull OnlineRoutingEngine engine) {
		List<OnlineRoutingEngine> cachedEngines = helper.getEnginesExceptMentionedKeys(editedEngineKey);
		return OnlineRoutingUtils.hasNameDuplicate(app, engine.getName(app), cachedEngines);
	}

	private void onSaveEngine() {
		if (engine != null) {
			helper.saveEngine(engine);
		}
	}

	private void onDeleteEngine() {
		helper.deleteEngine(engine);
	}

	private void delete(Activity activity) {
		if (engine != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, isNightMode()));
			builder.setMessage(getString(R.string.delete_online_routing_engine));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
				onDeleteEngine();
				dismiss();
			});
			builder.create().show();
		}
	}

	private boolean isEditingMode() {
		return editedEngineKey != null;
	}

	private String getTestUrl() {
		List<LatLon> path = new ArrayList<>();
		path.add(selectedLocation.getCityCenterLatLon());
		path.add(selectedLocation.getCityAirportLatLon());
		return engine.getFullUrl(path, 0f);
	}

	private void testEngineWork() {
		OnlineRoutingEngine requestedEngine = (OnlineRoutingEngine) engine.clone();
		ExampleLocation location = selectedLocation;
		new Thread(() -> {
			StringBuilder errorMessage = new StringBuilder();
			boolean resultOk = false;
			try {
				String method = engine.getHTTPMethod();
				List<LatLon> path = Arrays.asList(location.getCityAirportLatLon(),
						location.getCityCenterLatLon());
				String body = engine.getRequestBody(path, null);
				Map<String, String> headers = engine.getRequestHeaders();
				String response = helper.makeRequest(exampleCard.getEditedText(), method, body, headers);
				resultOk = requestedEngine.isResultOk(errorMessage, response);
			} catch (IOException | JSONException e) {
				errorMessage.append(e);
			}
			showTestResults(resultOk, errorMessage.toString(), location);
		}).start();
	}

	private void showTestResults(boolean resultOk,
	                             @NonNull String message,
	                             @NonNull ExampleLocation location) {
		app.runInUIThread(() -> {
			if (isAdded()) {
				testResultsContainer.setVisibility(View.VISIBLE);
				ImageView ivImage = testResultsContainer.findViewById(R.id.icon);
				TextView tvTitle = testResultsContainer.findViewById(R.id.title);
				TextView tvDescription = testResultsContainer.findViewById(R.id.description);
				if (resultOk) {
					ivImage.setImageDrawable(getContentIcon(R.drawable.ic_action_gdirections_dark));
					tvTitle.setText(getString(R.string.shared_string_ok));
				} else {
					ivImage.setImageDrawable(getContentIcon(R.drawable.ic_action_alert));
					tvTitle.setText(String.format(getString(R.string.message_server_error), message));
				}
				tvDescription.setText(location.getName());
				scrollView.post(() -> scrollView.scrollTo(0, scrollView.getChildAt(0).getBottom()));
			}
		});
	}

	private void updateCardViews(@NonNull BaseCard... cardsToUpdate) {
		for (BaseCard card : cardsToUpdate) {
			if (nameCard.equals(card)) {
				if (Algorithms.isEmpty(engine.get(EngineParameter.CUSTOM_NAME))) {
					nameCard.setEditedText(engine.getName(app));
				}

			} else if (typeCard.equals(card)) {
				typeCard.setHeaderSubtitle(engine.getType().getTitle());
				typeCard.setEditedText(engine.getBaseUrl());
				updateCardVisibility(apiKeyCard, EngineParameter.API_KEY);
				updateCardVisibility(vehicleCard, EngineParameter.VEHICLE_KEY);
				updateCardVisibility(approximateCard, EngineParameter.APPROXIMATION_ROUTING_PROFILE);
				updateCardVisibility(useExternalTimestampsCard, EngineParameter.USE_EXTERNAL_TIMESTAMPS);
				updateCardVisibility(routingFallbackCard, EngineParameter.USE_ROUTING_FALLBACK);

			} else if (vehicleCard.equals(card)) {
				VehicleType vt = engine.getSelectedVehicleType();
				vehicleCard.setHeaderSubtitle(vt.getTitle(app));
				if (vt.equals(CUSTOM_VEHICLE)) {
					vehicleCard.showFieldBox();
					vehicleCard.setEditedText(customVehicleKey);
				} else {
					vehicleCard.hideFieldBox();
				}

			} else if (exampleCard.equals(card)) {
				exampleCard.setEditedText(getTestUrl(), false);
			}
		}
	}

	private void updateCardVisibility(OnlineRoutingCard card, EngineParameter parameter) {
		if (engine.isParameterAllowed(parameter)) {
			card.show();
		} else {
			card.hide();
		}
	}

	public void showExitDialog() {
		View focus = view.findFocus();
		AndroidUtils.hideSoftKeyboard(mapActivity, focus);
		if (hasNameDuplicate(initEngine)) {
			List<OnlineRoutingEngine> cachedEngines = helper.getEnginesExceptMentionedKeys(editedEngineKey);
			OnlineRoutingUtils.generateUniqueName(app, initEngine, cachedEngines);
		}
		if (!engine.equals(initEngine)) {
			AlertDialog.Builder dismissDialog = createWarningDialog(mapActivity,
					R.string.shared_string_dismiss, R.string.exit_without_saving, R.string.shared_string_cancel);
			dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
			dismissDialog.show();
		} else {
			dismiss();
		}
	}

	private AlertDialog.Builder createWarningDialog(Activity activity, int title, int message, int negButton) {
		Context themedContext = UiUtilities.getThemedContext(activity, isNightMode());
		AlertDialog.Builder warningDialog = new AlertDialog.Builder(themedContext);
		warningDialog.setTitle(getString(title));
		warningDialog.setMessage(getString(message));
		warningDialog.setNegativeButton(negButton, null);
		return warningDialog;
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		removeOnGlobalLayoutListener();
		removeOnScrollListener();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState(outState);
	}

	private void saveState(@NonNull Bundle outState) {
		outState.putString(ENGINE_TYPE_KEY, engine.getTypeName());
		for (EngineParameter key : EngineParameter.values()) {
			String value = engine.get(key);
			if (value != null) {
				outState.putString(key.name(), value);
			}
		}
		outState.putString(ENGINE_CUSTOM_VEHICLE_KEY, customVehicleKey);
		outState.putString(EXAMPLE_LOCATION_KEY, selectedLocation.name());
		outState.putString(EDITED_ENGINE_KEY, editedEngineKey);
	}

	private void restoreState(@NonNull Bundle savedState) {
		editedEngineKey = savedState.getString(EngineParameter.KEY.name());
		initEngine = createInitStateEngine();
		String typeKey = savedState.getString(ENGINE_TYPE_KEY);
		OnlineRoutingEngine type = EngineType.getTypeByName(typeKey);
		Map<String, String> params = new HashMap<>();
		for (EngineParameter key : EngineParameter.values()) {
			String value = savedState.getString(key.name());
			if (value != null) {
				params.put(key.name(), value);
			}
		}
		engine = type.newInstance(params);
		customVehicleKey = savedState.getString(ENGINE_CUSTOM_VEHICLE_KEY);
		selectedLocation = ExampleLocation.valueOf(savedState.getString(EXAMPLE_LOCATION_KEY));
	}

	private void initState() {
		initEngine = createInitStateEngine();
		selectedLocation = ExampleLocation.values()[0];
		engine = (OnlineRoutingEngine) initEngine.clone();
		if (Algorithms.objectEquals(engine.getSelectedVehicleType(), CUSTOM_VEHICLE)) {
			customVehicleKey = engine.get(EngineParameter.VEHICLE_KEY);
		} else {
			customVehicleKey = "";
		}
	}

	private OnlineRoutingEngine createInitStateEngine() {
		OnlineRoutingEngine engine;
		OnlineRoutingEngine editedEngine = helper.getEngineByKey(editedEngineKey);
		if (editedEngine != null) {
			engine = (OnlineRoutingEngine) editedEngine.clone();
		} else {
			engine = EngineType.values()[0].newInstance(null);
			String vehicle = engine.getAllowedVehicles().get(0).getKey();
			engine.put(EngineParameter.VEHICLE_KEY, vehicle);
			if (editedEngineKey != null) {
				engine.put(EngineParameter.KEY, editedEngineKey);
			}
		}
		return engine;
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull ApplicationMode appMode,
	                                @Nullable String editedEngineKey) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OnlineRoutingEngineFragment fragment = new OnlineRoutingEngineFragment();
			fragment.setAppMode(appMode);
			fragment.editedEngineKey = editedEngineKey;
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void hideKeyboardOnScroll() {
		scrollView.setOnTouchListener(new View.OnTouchListener() {
			int scrollViewY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int y = scrollView.getScrollY();
				if (isKeyboardShown && scrollViewY != y) {
					scrollViewY = y;
					View focus = mapActivity.getCurrentFocus();
					if (focus != null) {
						AndroidUtils.hideSoftKeyboard(mapActivity, focus);
						focus.clearFocus();
					}
				}
				return false;
			}
		});
	}

	private void showShadowBelowButtons() {
		scrollView.getViewTreeObserver().addOnScrollChangedListener(getShowShadowOnScrollListener());
	}

	private void showButtonsAboveKeyboard() {
		view.getViewTreeObserver().addOnGlobalLayoutListener(getShowButtonsOnGlobalListener());
	}

	private OnScrollChangedListener getShowShadowOnScrollListener() {
		if (onScroll == null) {
			onScroll = () -> {
				boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
				if (scrollToBottomAvailable) {
					showShadowButton();
				} else {
					hideShadowButton();
				}
			};
		}
		return onScroll;
	}

	private OnGlobalLayoutListener getShowButtonsOnGlobalListener() {
		if (onGlobalLayout == null) {
			onGlobalLayout = new OnGlobalLayoutListener() {
				private int layoutHeightPrevious;
				private int layoutHeightMin;

				@Override
				public void onGlobalLayout() {
					view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

					int layoutHeight = AndroidUtils.resizeViewForKeyboard(mapActivity, view, layoutHeightPrevious);
					if (layoutHeight < layoutHeightPrevious) {
						isKeyboardShown = true;
						layoutHeightMin = layoutHeight;
					} else {
						isKeyboardShown = layoutHeight == layoutHeightMin;
					}
					if (layoutHeight != layoutHeightPrevious) {
						layoutHeightPrevious = layoutHeight;
					}

					view.post(() -> {
						view.getViewTreeObserver().addOnGlobalLayoutListener(getShowButtonsOnGlobalListener());
					});
				}
			};
		}
		return onGlobalLayout;
	}

	private void removeOnScrollListener() {
		scrollView.getViewTreeObserver().removeOnScrollChangedListener(getShowShadowOnScrollListener());
	}

	private void removeOnGlobalLayoutListener() {
		view.getViewTreeObserver().removeOnGlobalLayoutListener(getShowButtonsOnGlobalListener());
	}

	private void showShadowButton() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200)
				.setListener(null);
	}

	private void hideShadowButton() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);
	}

	@Override
	public void onProfileSelected(Bundle args) {
		engine.put(EngineParameter.APPROXIMATION_ROUTING_PROFILE, args.getString(PROFILE_KEY_ARG));
		engine.put(EngineParameter.APPROXIMATION_DERIVED_PROFILE, args.getString(DERIVED_PROFILE_ARG));
		setApproximateCardTitle();
	}
}
