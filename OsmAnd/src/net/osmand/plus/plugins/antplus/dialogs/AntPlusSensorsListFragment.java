package net.osmand.plus.plugins.antplus.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class AntPlusSensorsListFragment extends AntPlusBaseFragment {

	public static final String TAG = AntPlusSensorsListFragment.class.getSimpleName();
	protected View dismissButton;

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_ant_plus_sensors_list;
	}

	@Override
	protected void setupUI(@NonNull View view) {
		super.setupUI(view);
		ImageView sensorIcon = view.findViewById(R.id.sensor_icon);
		sensorIcon.setBackgroundResource(nightMode ? R.drawable.img_help_sensors_night : R.drawable.img_help_sensors_day);
		TextView learnMore = view.findViewById(R.id.learn_more_button);
		String docsLinkText = app.getString(R.string.learn_more_about_sensors_link);
		UiUtilities.setupClickableText(app, learnMore, docsLinkText, docsLinkText, nightMode, new CallbackWithObject<Void>() {
			@Override
			public boolean processResult(Void unused) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					String docsUrl = getString(R.string.docs_external_sensors);
					intent.setData(Uri.parse(docsUrl));
					AndroidUtils.startActivityIfSafe(activity, intent);
				}
				return false;
			}
		});
		setupPairSensorButton(view);
	}

	private void setupPairSensorButton(@NonNull View view) {
		dismissButton = view.findViewById(R.id.dismiss_button);
		int buttonTextId = R.string.ant_plus_pair_new_sensor;
		ViewGroup.LayoutParams layoutParams = dismissButton.getLayoutParams();
		UiUtilities.setupDialogButton(nightMode, dismissButton, UiUtilities.DialogButtonType.SECONDARY, buttonTextId);
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		dismissButton.setLayoutParams(layoutParams);
		view.requestLayout();
		dismissButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPairNewSensorBottomSheet();
			}
		});
		AndroidUiHelper.updateVisibility(dismissButton, true);
	}

	private void showPairNewSensorBottomSheet() {
		dismiss();
		PairNewSensorBottomSheet.showInstance(requireActivity().getSupportFragmentManager());
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.id.action_add) {
				showPairNewSensorBottomSheet();
			}
			return false;
		});
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AntPlusSensorsListFragment fragment = new AntPlusSensorsListFragment();
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}