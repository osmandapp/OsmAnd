package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppHelper;
import net.osmand.plus.inapp.InAppHelper.InAppCallbacks;

public class SubscriptionFragment extends BaseOsmAndDialogFragment implements InAppCallbacks {

	public static final String TAG = "SubscriptionFragment";

	private InAppHelper inAppHelper;
	private ProgressDialog dlg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		inAppHelper = new InAppHelper(getMyApplication(), this);
		Activity activity = getActivity();
		if (activity instanceof OsmLiveActivity) {
			((OsmLiveActivity) activity).setInAppHelper(inAppHelper);
		}

		inAppHelper.start(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.subscription_fragment, container, false);
		ImageButton closeButton = (ImageButton) view.findViewById(R.id.closeButton);
		//setThemedDrawable(closeButton, R.drawable.ic_action_remove_dark);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		Button subscribeButton = (Button) view.findViewById(R.id.subscribeButton);
		subscribeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (inAppHelper != null) {
					inAppHelper.purchaseLiveUpdates(getActivity(), "1@1.1", "userName", "Ukraine");
				}
			}
		});

		dlg = new ProgressDialog(getActivity());
		dlg.setTitle("");
		dlg.setMessage(getString(R.string.wait_current_task_finished));

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		inAppHelper.stop();
		if (dlg != null && dlg.isShowing()) {
			dlg.hide();
		}
		Activity activity = getActivity();
		if (activity instanceof OsmLiveActivity) {
			((OsmLiveActivity) activity).setInAppHelper(null);
		}
	}

	@Override
	public void onError(String error) {
	}

	@Override
	public void onGetItems() {

	}

	@Override
	public void onItemPurchased(String sku) {

		if (InAppHelper.SKU_LIVE_UPDATES.equals(sku)) {
			Fragment parentFragment = getParentFragment();
			if (parentFragment instanceof LiveUpdatesFragment) {
				((LiveUpdatesFragment) parentFragment).updateSubscriptionBanner();
			}
		}

		dismiss();
	}

	@Override
	public void showHideProgress(boolean show) {
		if (show) {
			dlg.show();
		} else {
			dlg.hide();
		}
	}
}
