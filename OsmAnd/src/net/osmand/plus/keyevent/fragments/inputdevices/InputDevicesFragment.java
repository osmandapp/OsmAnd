package net.osmand.plus.keyevent.fragments.inputdevices;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class InputDevicesFragment extends BaseOsmAndFragment implements InputDevicesEventListener {

	public static final String TAG = InputDevicesFragment.class.getSimpleName();

	private InputDevicesAdapter adapter;
	private InputDevicesController controller;

	private ApplicationMode appMode;
	private InputDevicesHelper deviceHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		String appModeKey = arguments != null ? arguments.getString(APP_MODE_KEY) : "";
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		controller = new InputDevicesController(app, appMode, isUsedOnMap());
		deviceHelper = app.getInputDeviceHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_external_input_device_type, container);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);

		adapter = new InputDevicesAdapter(app, appMode, controller, isUsedOnMap());
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
		updateViewContent();
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageResource(R.drawable.ic_action_close);
		closeButton.setOnClickListener(v -> dismiss());

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(getString(R.string.shared_string_type));
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);

		View actionButton = toolbar.findViewById(R.id.action_button);
		ImageView actionButtonImage = toolbar.findViewById(R.id.action_button_icon);
		actionButtonImage.setImageDrawable(getContentIcon(R.drawable.ic_action_add_no_bg));
		actionButton.setOnClickListener(v -> {
			controller.askAddNewCustomDevice();
		});
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	@Override
	public void processInputDevicesEvent(@NonNull ApplicationMode appMode, @NonNull EventType event) {
		if (event.isDeviceRelated()) {
			updateViewContent();
		}
		if (event == EventType.SELECT_DEVICE) {
			dismiss();
		}
	}

	private void updateViewContent() {
		adapter.setScreenItems(controller.populateScreenItems());
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
		deviceHelper.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
		deviceHelper.removeListener(this);
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			InputDevicesFragment fragment = new InputDevicesFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
