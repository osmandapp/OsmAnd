package net.osmand.plus.mapcontextmenu.builders.cards.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.util.Algorithms;

public class ContextMenuCardDialogFragment extends BaseOsmAndFragment {
	public static final String TAG = "ContextMenuCardDialogFragment";

	private ContextMenuCardDialog dialog;
	private LinearLayout contentLayout;
	private View contentView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && getActivity() instanceof MapActivity) {
			dialog = ContextMenuCardDialog.restoreMenu(savedInstanceState, (MapActivity) getActivity());
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.context_menu_card_dialog, container, false);
		AndroidUtils.addStatusBarPadding21v(getActivity(), view);
		if (dialog.getType() == ContextMenuCardDialog.CardDialogType.MAPILLARY) {
			view.findViewById(R.id.dialog_layout)
					.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.mapillary_action_bar));
		}
		contentLayout = (LinearLayout) view.findViewById(R.id.content);
		contentView = dialog.getContentView();
		if (contentView != null) {
			contentLayout.addView(contentView);
		}
		view.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		if (!Algorithms.isEmpty(dialog.getTitle())) {
			((TextView) view.findViewById(R.id.title)).setText(dialog.getTitle());
		}
		if (!Algorithms.isEmpty(dialog.getDescription())) {
			((TextView) view.findViewById(R.id.description)).setText(dialog.getDescription());
		}
		AppCompatImageView moreButton = (AppCompatImageView) view.findViewById(R.id.more_button);
		if (dialog.haveMenuItems()) {
			moreButton.setVisibility(View.VISIBLE);
			moreButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final PopupMenu optionsMenu = new PopupMenu(getContext(), v);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					dialog.createMenuItems(optionsMenu.getMenu());
					optionsMenu.show();
				}
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
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (dialog != null) {
			dialog.onPause();
		}
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
	public void onSaveInstanceState(Bundle outState) {
		dialog.saveMenu(outState);
	}

	@Override
	public int getStatusBarColorId() {
		if (dialog != null && dialog.getType() == ContextMenuCardDialog.CardDialogType.MAPILLARY) {
			return R.color.status_bar_mapillary;
		}
		return -1;
	}

	public static void showInstance(ContextMenuCardDialog menu) {
		ContextMenuCardDialogFragment fragment = new ContextMenuCardDialogFragment();
		fragment.dialog = menu;
		menu.getMapActivity().getSupportFragmentManager().beginTransaction()
				.replace(R.id.topFragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commitAllowingStateLoss();
	}

	public void dismiss() {
		MapActivity activity = dialog.getMapActivity();
		if (activity != null) {
			if (dialog.getType() == ContextMenuCardDialog.CardDialogType.MAPILLARY) {
				if (!activity.getMyApplication().getSettings().SHOW_MAPILLARY.get()) {
					MapillaryPlugin mapillaryPlugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
					if (mapillaryPlugin != null) {
						mapillaryPlugin.updateLayers(activity.getMapView(), activity);
					}
				}
			}
			try {
				activity.getSupportFragmentManager().popBackStack(TAG,
						FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
