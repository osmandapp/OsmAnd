package net.osmand.plus.mapcontextmenu.builders.cards.dialogs;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.BoundsChangeListener;
import net.osmand.plus.helpers.MapDisplayPositionManager.ICoveredScreenRectProvider;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class ContextMenuCardDialogFragment extends BaseOsmAndFragment implements ICoveredScreenRectProvider {

	public static final String TAG = "ContextMenuCardDialogFragment";

	private MapDisplayPositionManager displayPositionManager;
	private ContextMenuCardDialog dialog;
	private LinearLayout contentLayout;
	private View contentView;
	private BoundsChangeListener boundsChangeListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && getActivity() instanceof MapActivity) {
			dialog = ContextMenuCardDialog.restoreMenu(savedInstanceState, (MapActivity) getActivity());
		}
		displayPositionManager = app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
		boundsChangeListener = new BoundsChangeListener(displayPositionManager, false);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (dialog == null) {
			return null;
		}
		View view = inflater.inflate(R.layout.context_menu_card_dialog, container, false);
		FragmentActivity activity = requireActivity();
		AndroidUtils.addStatusBarPadding21v(activity, view);
		if (dialog.getType() == ContextMenuCardDialog.CardDialogType.MAPILLARY) {
			view.findViewById(R.id.dialog_layout)
					.setBackgroundColor(ContextCompat.getColor(activity, R.color.mapillary_action_bar));
		}
		contentLayout = view.findViewById(R.id.content);
		contentView = dialog.getContentView();
		if (contentView != null) {
			contentLayout.addView(contentView);
		}
		view.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());
		if (!Algorithms.isEmpty(dialog.getTitle())) {
			((TextView) view.findViewById(R.id.title)).setText(dialog.getTitle());
		}
		if (!Algorithms.isEmpty(dialog.getDescription())) {
			((TextView) view.findViewById(R.id.description)).setText(dialog.getDescription());
		}
		AppCompatImageView moreButton = view.findViewById(R.id.more_button);
		if (dialog.haveMenuItems()) {
			moreButton.setVisibility(View.VISIBLE);
			moreButton.setOnClickListener(v -> {
				PopupMenu optionsMenu = new PopupMenu(v.getContext(), v);
				DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
				dialog.createMenuItems(optionsMenu.getMenu());
				optionsMenu.show();
			});
		} else {
			moreButton.setVisibility(View.GONE);
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (dialog != null) {
			dialog.onResume();
			updateBoundsChangeListener(true);
		} else {
			dismiss();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (dialog != null) {
			dialog.onPause();
		}
		updateBoundsChangeListener(false);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (contentLayout != null && contentView != null) {
			contentLayout.removeView(contentView);
			if (contentView instanceof WebView) {
				((WebView) contentView).loadUrl("about:blank");
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (dialog != null) {
			dialog.saveMenu(outState);
		}
	}

	@Override
	public int getStatusBarColorId() {
		if (dialog != null && dialog.getType() == ContextMenuCardDialog.CardDialogType.MAPILLARY) {
			return R.color.status_bar_mapillary;
		}
		return -1;
	}

	public static void showInstance(@NonNull ContextMenuCardDialog menu) {
		FragmentManager fragmentManager = menu.getMapActivity().getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			ContextMenuCardDialogFragment fragment = new ContextMenuCardDialogFragment();
			fragment.dialog = menu;
			fragmentManager.beginTransaction()
					.replace(R.id.topFragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	private void updateBoundsChangeListener(boolean add) {
		displayPositionManager.updateCoveredScreenRectProvider(this, add);
		View view = getView();
		if (view != null) {
			if (add) {
				view.addOnLayoutChangeListener(boundsChangeListener);
			} else {
				view.removeOnLayoutChangeListener(boundsChangeListener);
			}
			if (view.getWidth() > 0 && view.getHeight() > 0) {
				displayPositionManager.updateMapDisplayPosition();
			}
		}
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	@NonNull
	@Override
	public List<Rect> getCoveredScreenRects() {
		View view = getView();
		return view == null
				? Collections.emptyList()
				: Collections.singletonList(AndroidUtils.getViewBoundOnScreen(view));
	}
}
