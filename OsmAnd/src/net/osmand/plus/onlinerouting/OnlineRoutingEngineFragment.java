package net.osmand.plus.onlinerouting;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.onlinerouting.OnlineRoutingCard.OnTextChangedListener;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine.EngineParameterType;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OnlineRoutingEngineFragment extends BaseOsmAndFragment {

	public static final String TAG = OnlineRoutingEngineFragment.class.getSimpleName();

	private static final String ENGINE_NAME_KEY = "engine_name";
	private static final String ENGINE_SERVER_KEY = "engine_server";
	private static final String ENGINE_SERVER_URL_KEY = "engine_server_url";
	private static final String ENGINE_VEHICLE_TYPE_KEY = "engine_vehicle_type";
	private static final String ENGINE_CUSTOM_VEHICLE_KEY = "engine_custom_vehicle";
	private static final String ENGINE_API_KEY_KEY = "engine_api_key";
	private static final String EXAMPLE_LOCATION_KEY = "example_location";
	private static final String APP_MODE_KEY = "app_mode";
	private static final String EDITED_ENGINE_KEY = "edited_engine_key";

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OnlineRoutingHelper helper;

	private View view;
	private ViewGroup segmentsContainer;
	private OnlineRoutingCard nameCard;
	private OnlineRoutingCard serverCard;
	private OnlineRoutingCard vehicleCard;
	private OnlineRoutingCard apiKeyCard;
	private OnlineRoutingCard exampleCard;
	private View testResultsContainer;

	private ApplicationMode appMode;

	private OnlineRoutingEngineObject engine;
	private ExampleLocation selectedLocation;
	private String editedEngineKey;

	private enum ExampleLocation {

		AMSTERDAM("Amsterdam",
				new LatLon(52.379189, 4.899431),
				new LatLon(52.308056, 4.764167)),

		BERLIN("Berlin",
				new LatLon(52.520008, 13.404954),
				new LatLon(52.3666652, 13.501997992)),

		NEW_YORK("New York",
				new LatLon(43.000000, -75.000000),
				new LatLon(40.641766, -73.780968)),

		PARIS("Paris",
				new LatLon(48.864716, 2.349014),
				new LatLon(48.948437, 2.434931));

		ExampleLocation(String name, LatLon cityCenterLatLon, LatLon cityAirportLatLon) {
			this.name = name;
			this.cityCenterLatLon = cityCenterLatLon;
			this.cityAirportLatLon = cityAirportLatLon;
		}

		private String name;
		private LatLon cityCenterLatLon;
		private LatLon cityAirportLatLon;

		public String getName() {
			return name;
		}

		public LatLon getCityCenterLatLon() {
			return cityCenterLatLon;
		}

		public LatLon getCityAirportLatLon() {
			return cityAirportLatLon;
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		mapActivity = getMapActivity();
		helper = app.getOnlineRoutingHelper();
		engine = new OnlineRoutingEngineObject();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {
			initState();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
							 @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		view = getInflater().inflate(
				R.layout.online_routing_engine_fragment, container, false);
		segmentsContainer = (ViewGroup) view.findViewById(R.id.segments_container);
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(getContext(), view);
		}
		setupToolbar((Toolbar) view.findViewById(R.id.toolbar));

		setupNameCard();
		setupServerCard();
		setupVehicleCard();
		setupApiKeyCard();
		setupExampleCard();
		setupResultsContainer();
		addSpaceSegment();

		setupButtons();

		updateCardViews(nameCard, serverCard, vehicleCard, exampleCard);
		return view;
	}

	private void setupNameCard() {
		nameCard = new OnlineRoutingCard(mapActivity, isNightMode());
		nameCard.build(mapActivity);
		nameCard.setDescription(getString(R.string.select_nav_profile_dialog_message));
		nameCard.setEditedText(engine.getName(app));
		nameCard.setFieldBoxLabelText(getString(R.string.shared_string_name));
		nameCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean changedByUser, String text) {
				if (changedByUser) {
					engine.customName = text;
				}
			}
		});
		nameCard.showDivider();
		segmentsContainer.addView(nameCard.getView());
	}

	private void setupServerCard() {
		serverCard = new OnlineRoutingCard(mapActivity, isNightMode());
		serverCard.build(mapActivity);
		serverCard.setHeaderTitle(getString(R.string.shared_string_type));
		List<HorizontalSelectionItem> serverItems = new ArrayList<>();
		for (ServerType server : ServerType.values()) {
			serverItems.add(new HorizontalSelectionItem(server.getTitle(), server));
		}
		serverCard.setSelectionMenu(serverItems, engine.serverType.getTitle(),
				new CallbackWithObject<HorizontalSelectionItem>() {
					@Override
					public boolean processResult(HorizontalSelectionItem result) {
						ServerType server = (ServerType) result.getObject();
						if (engine.serverType != server) {
							engine.serverType = server;
							updateCardViews(nameCard, serverCard, exampleCard);
							return true;
						}
						return false;
					}
				});
		serverCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean editedByUser, String text) {
				if (editedByUser) {
					engine.customServerUrl = text;
				}
			}
		});
		serverCard.setFieldBoxLabelText(getString(R.string.shared_string_server_url));
		serverCard.showDivider();
		segmentsContainer.addView(serverCard.getView());
	}

	private void setupVehicleCard() {
		vehicleCard = new OnlineRoutingCard(mapActivity, isNightMode());
		vehicleCard.build(mapActivity);
		vehicleCard.setHeaderTitle(getString(R.string.shared_string_vehicle));
		List<HorizontalSelectionItem> vehicleItems = new ArrayList<>();
		for (VehicleType vehicle : VehicleType.values()) {
			vehicleItems.add(new HorizontalSelectionItem(vehicle.getTitle(app), vehicle));
		}
		vehicleCard.setSelectionMenu(vehicleItems, engine.vehicleType.getTitle(app),
				new CallbackWithObject<HorizontalSelectionItem>() {
					@Override
					public boolean processResult(HorizontalSelectionItem result) {
						VehicleType vehicle = (VehicleType) result.getObject();
						if (engine.vehicleType != vehicle) {
							engine.vehicleType = vehicle;
							updateCardViews(nameCard, vehicleCard, exampleCard);
							return true;
						}
						return false;
					}
				});
		vehicleCard.setFieldBoxLabelText(getString(R.string.shared_string_custom));
		vehicleCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean editedByUser, String text) {
				if (editedByUser) {
					engine.customVehicleKey = text;
					updateCardViews(nameCard, exampleCard);
				}
			}
		});
		vehicleCard.setEditedText(engine.customVehicleKey);
		vehicleCard.setFieldBoxHelperText(getString(R.string.shared_string_enter_param));
		vehicleCard.showDivider();
		segmentsContainer.addView(vehicleCard.getView());
	}

	private void setupApiKeyCard() {
		apiKeyCard = new OnlineRoutingCard(mapActivity, isNightMode());
		apiKeyCard.build(mapActivity);
		apiKeyCard.setHeaderTitle(getString(R.string.shared_string_api_key));
		apiKeyCard.setFieldBoxLabelText(getString(R.string.keep_it_empty_if_not));
		apiKeyCard.setEditedText(engine.apiKey);
		apiKeyCard.showDivider();
		apiKeyCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean editedByUser, String text) {
				engine.apiKey = text;
				updateCardViews(exampleCard);
			}
		});
		segmentsContainer.addView(apiKeyCard.getView());
	}

	private void setupExampleCard() {
		exampleCard = new OnlineRoutingCard(mapActivity, isNightMode());
		exampleCard.build(mapActivity);
		exampleCard.setHeaderTitle(getString(R.string.shared_string_example));
		List<HorizontalSelectionItem> locationItems = new ArrayList<>();
		for (ExampleLocation location : ExampleLocation.values()) {
			locationItems.add(new HorizontalSelectionItem(location.getName(), location));
		}
		exampleCard.setSelectionMenu(locationItems, selectedLocation.getName(),
				new CallbackWithObject<HorizontalSelectionItem>() {
					@Override
					public boolean processResult(HorizontalSelectionItem result) {
						ExampleLocation location = (ExampleLocation) result.getObject();
						if (selectedLocation != location) {
							selectedLocation = location;
							updateCardViews(exampleCard);
							return true;
						}
						return false;
					}
				});
		exampleCard.setFieldBoxHelperText(getString(R.string.online_routing_example_hint));
		exampleCard.setButton(getString(R.string.test_route_calculation), new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				testEngineWork();
			}
		});
		segmentsContainer.addView(exampleCard.getView());
	}

	private void setupResultsContainer() {
		testResultsContainer = getInflater().inflate(
				R.layout.bottom_sheet_item_with_descr_64dp, segmentsContainer, false);
		testResultsContainer.setVisibility(View.GONE);
		segmentsContainer.addView(testResultsContainer);
	}

	private void addSpaceSegment() {
		int space = (int) getResources().getDimension(R.dimen.empty_state_text_button_padding_top);
		View bottomSpaceView = new View(app);
		bottomSpaceView.setLayoutParams(
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, space));
		segmentsContainer.addView(bottomSpaceView);
	}

	private void setupToolbar(Toolbar toolbar) {
		ImageView navigationIcon = toolbar.findViewById(R.id.close_button);
		navigationIcon.setImageResource(R.drawable.ic_action_close);
		navigationIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);
		View actionBtn = toolbar.findViewById(R.id.action_button);
		if (isEditingMode()) {
			title.setText(getString(R.string.edit_online_routing_engine));
			ImageView ivBtn = toolbar.findViewById(R.id.action_button_icon);
			ivBtn.setImageDrawable(
					getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete));
			actionBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteEngine();
					dismiss();
				}
			});
		} else {
			title.setText(getString(R.string.add_online_routing_engine));
			actionBtn.setVisibility(View.GONE);
		}
	}

	private void updateCardViews(BaseCard... cardsToUpdate) {
		for (BaseCard card : cardsToUpdate) {
			if (nameCard.equals(card)) {
				if (Algorithms.isEmpty(engine.customName)) {
					String name = OnlineRoutingEngine.getStandardName(app, engine.serverType, engine.getVehicleKey());
					nameCard.setEditedText(name);
				}

			} else if (serverCard.equals(card)) {
				serverCard.setHeaderSubtitle(engine.serverType.getTitle());
				serverCard.setEditedText(engine.getBaseUrl());
				if (engine.serverType == ServerType.GRAPHHOPER) {
					apiKeyCard.show();
				} else {
					apiKeyCard.hide();
				}

			} else if (vehicleCard.equals(card)) {
				vehicleCard.setHeaderSubtitle(engine.vehicleType.getTitle(app));
				if (engine.vehicleType == VehicleType.CUSTOM) {
					vehicleCard.showFieldBox();
					vehicleCard.setEditedText(engine.getVehicleKey());
				} else {
					vehicleCard.hideFieldBox();
				}

			} else if (exampleCard.equals(card)) {
				exampleCard.setEditedText(getTestUrl());
			}
		}
	}

	private void setupButtons() {
		boolean nightMode = isNightMode();
		View cancelButton = view.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, cancelButton,
				DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		view.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

		View saveButton = view.findViewById(R.id.right_bottom_button);
		UiUtilities.setupDialogButton(nightMode, saveButton,
				UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_save);
		saveButton.setVisibility(View.VISIBLE);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveChanges();
				dismiss();
			}
		});
	}

	private void saveChanges() {
		OnlineRoutingEngine engineToSave;
		if (isEditingMode()) {
			engineToSave = new OnlineRoutingEngine(editedEngineKey, engine.serverType, engine.getVehicleKey());
		} else {
			engineToSave = OnlineRoutingEngine.createNewEngine(engine.serverType, engine.getVehicleKey(), null);
		}

		engineToSave.putParameter(EngineParameterType.CUSTOM_SERVER_URL, engine.customServerUrl);
		engineToSave.putParameter(EngineParameterType.CUSTOM_NAME, engine.customName);
		if (engine.serverType == ServerType.GRAPHHOPER) {
			engineToSave.putParameter(EngineParameterType.API_KEY, engine.apiKey);
		}

		helper.saveEngine(engineToSave);
	}

	private void deleteEngine() {
		helper.deleteEngine(editedEngineKey);
	}

	private String getTestUrl() {
		String baseUrl = engine.serverType.getBaseUrl();
		String vehicle = engine.getVehicleKey();

		LatLon startPoint = selectedLocation.getCityCenterLatLon();
		LatLon endPoint = selectedLocation.getCityAirportLatLon();

		if (engine.serverType == ServerType.GRAPHHOPER) {
			return baseUrl + "?" + "point=" + startPoint.getLatitude()
					+ "," + startPoint.getLongitude()
					+ "&" + "point=" + endPoint.getLatitude()
					+ "," + endPoint.getLongitude()
					+ "&" + "vehicle=" + vehicle
					+ (!Algorithms.isEmpty(engine.apiKey) ? ("&" + "key=" + engine.apiKey) : "");
		} else {
			return baseUrl + vehicle + "/" + startPoint.getLatitude()
					+ "," + startPoint.getLongitude()
					+ ";" + endPoint.getLatitude()
					+ "," + endPoint.getLongitude()
					+ "?" + "geometries=geojson";
		}
	}

	private void testEngineWork() {
		final ServerType server = engine.serverType;
		final ExampleLocation location = selectedLocation;
		AndroidNetworkUtils.sendRequestAsync(app, exampleCard.getEditedText(), null,
				null, false, false, new OnRequestResultListener() {
					@Override
					public void onResult(String response) {
						boolean resultOk = false;
						if (response != null) {
							try {
								JSONObject obj = new JSONObject(response);

								if (server == ServerType.GRAPHHOPER) {
									resultOk = obj.has("paths");
								} else if (server == ServerType.OSRM) {
									resultOk = obj.has("routes");
								}
							} catch (JSONException e) {

							}
						}
						showTestResults(resultOk, location);
					}
				});
	}

	private void showTestResults(boolean resultOk, ExampleLocation location) {
		testResultsContainer.setVisibility(View.VISIBLE);
		ImageView ivImage = testResultsContainer.findViewById(R.id.icon);
		TextView tvTitle = testResultsContainer.findViewById(R.id.title);
		TextView tvDescription = testResultsContainer.findViewById(R.id.description);
		if (resultOk) {
			ivImage.setImageDrawable(getContentIcon(R.drawable.ic_action_gdirections_dark));
			tvTitle.setText(getString(R.string.shared_string_ok));
		} else {
			ivImage.setImageDrawable(getContentIcon(R.drawable.ic_action_alert));
			tvTitle.setText(getString(R.string.message_error_recheck_parameters));
		}
		tvDescription.setText(location.getName());
	}

	private boolean isEditingMode() {
		return editedEngineKey != null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState(outState);
	}

	private void saveState(Bundle outState) {
		outState.putString(ENGINE_NAME_KEY, engine.customName);
		outState.putString(ENGINE_SERVER_KEY, engine.serverType.name());
		outState.putString(ENGINE_SERVER_URL_KEY, engine.customServerUrl);
		outState.putString(ENGINE_VEHICLE_TYPE_KEY, engine.vehicleType.name());
		outState.putString(ENGINE_CUSTOM_VEHICLE_KEY, engine.customVehicleKey);
		outState.putString(ENGINE_API_KEY_KEY, engine.apiKey);
		outState.putString(EXAMPLE_LOCATION_KEY, selectedLocation.name());
		if (appMode != null) {
			outState.putString(APP_MODE_KEY, appMode.getStringKey());
		}
		outState.putString(EDITED_ENGINE_KEY, editedEngineKey);
	}

	private void restoreState(Bundle savedState) {
		engine.customName = savedState.getString(ENGINE_NAME_KEY);
		engine.serverType = ServerType.valueOf(savedState.getString(ENGINE_SERVER_KEY));
		engine.customServerUrl = savedState.getString(ENGINE_SERVER_URL_KEY);
		engine.vehicleType = VehicleType.valueOf(savedState.getString(ENGINE_VEHICLE_TYPE_KEY));
		engine.customVehicleKey = savedState.getString(ENGINE_CUSTOM_VEHICLE_KEY);
		engine.apiKey = savedState.getString(ENGINE_API_KEY_KEY);
		selectedLocation = ExampleLocation.valueOf(savedState.getString(EXAMPLE_LOCATION_KEY));
		appMode = ApplicationMode.valueOfStringKey(savedState.getString(APP_MODE_KEY), null);
		editedEngineKey = savedState.getString(EDITED_ENGINE_KEY);
	}

	private void initState() {
		engine.serverType = ServerType.values()[0];
		engine.vehicleType = VehicleType.values()[0];
		selectedLocation = ExampleLocation.values()[0];

		if (isEditingMode()) {
			OnlineRoutingEngine editedEngine = helper.getEngineByKey(editedEngineKey);
			if (editedEngine != null) {
				engine.customName = editedEngine.getParameter(EngineParameterType.CUSTOM_NAME);
				engine.serverType = editedEngine.getServerType();
				engine.customServerUrl = editedEngine.getParameter(EngineParameterType.CUSTOM_SERVER_URL);
				String vehicleKey = editedEngine.getVehicleKey();
				if (vehicleKey != null) {
					VehicleType vehicleType = VehicleType.getVehicleByKey(vehicleKey);
					if (vehicleType == VehicleType.CUSTOM) {
						engine.customVehicleKey = vehicleKey;
					}
					engine.vehicleType = vehicleType;
				}
				engine.apiKey = editedEngine.getParameter(EngineParameterType.API_KEY);
			}
		}
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private boolean isNightMode() {
		return !app.getSettings().isLightContentForMode(appMode);
	}

	@Nullable
	private MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	private LayoutInflater getInflater() {
		return UiUtilities.getInflater(mapActivity, isNightMode());
	}

	public static void showInstance(@NonNull FragmentActivity activity,
									@NonNull ApplicationMode appMode,
									String editedEngineKey) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (!fm.isStateSaved() && fm.findFragmentByTag(OnlineRoutingEngineFragment.TAG) == null) {
			OnlineRoutingEngineFragment fragment = new OnlineRoutingEngineFragment();
			fragment.appMode = appMode;
			fragment.editedEngineKey = editedEngineKey;
			fm.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();
		}
	}

	private static class OnlineRoutingEngineObject {
		private String customName;
		private ServerType serverType;
		private String customServerUrl;
		private VehicleType vehicleType;
		private String customVehicleKey;
		private String apiKey;

		public String getVehicleKey() {
			if (vehicleType == VehicleType.CUSTOM) {
				return customVehicleKey;
			}
			return vehicleType.getKey();
		}

		public String getBaseUrl() {
			return customServerUrl != null ? customServerUrl : serverType.getBaseUrl();
		}

		public String getName(Context ctx) {
			if (customName != null) {
				return customName;
			}
			return OnlineRoutingEngine.getStandardName(ctx, serverType, getVehicleKey());
		}
	}
}
