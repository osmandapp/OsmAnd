package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.helpers.ImportHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.ImportTrackCard;
import net.osmand.plus.routepreparationmenu.cards.TracksToFollowCard;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;


public class FollowTrackFragment extends ContextMenuScrollFragment implements CardListener {

	public static final String TAG = FollowTrackFragment.class.getName();

	private static final Log log = PlatformUtil.getLog(FollowTrackFragment.class);

	private OsmandApplication app;
	private ImportHelper importHelper;

	private GPXFile gpxFile;

	private int menuTitleHeight;

	@Override
	public int getMainLayoutId() {
		return R.layout.follow_track_options;
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		MapActivity mapActivity = requireMapActivity();
		importHelper = new ImportHelper(mapActivity, getMyApplication(), null);

		GPXRouteParamsBuilder routeParamsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (routeParamsBuilder != null) {
			gpxFile = routeParamsBuilder.getFile();
		}

		Bundle arguments = getArguments();
		if (savedInstanceState != null) {

		} else if (arguments != null) {

		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			ImageButton closeButton = view.findViewById(R.id.close_button);
			closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dismiss();
				}
			});

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openMenuHeaderOnly();
				}
			});

			if (isPortrait()) {
				updateCardsLayout();
			}
			setupCards();
			setupButtons(view);
			setupScrollShadow();
			if (!isPortrait()) {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
			}
			enterTrackAppearanceMode();
			runLayoutListener();
		}
		return view;
	}

	private void setupCards() {
		final MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			ImportTrackCard importTrackCard = new ImportTrackCard(mapActivity);
			importTrackCard.setListener(this);
			cardsContainer.addView(importTrackCard.build(mapActivity));

			File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			List<String> selectedTrackNames = GpxUiHelper.getSelectedTrackNames(app);
			List<GPXInfo> list = GpxUiHelper.getSortedGPXFilesInfo(dir, selectedTrackNames, false);
			if (list.size() > 0) {
				TracksToFollowCard tracksCard = new TracksToFollowCard(mapActivity, list);
				tracksCard.setListener(this);
				cardsContainer.addView(tracksCard.build(mapActivity));
			}
		}
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.route_menu_top_shadow_all).getHeight()
				+ view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
		super.calculateLayout(view, initLayout);
	}

	@Override
	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		super.setViewY(y, animated, adjustMapPos);
		updateStatusBarColor();
	}

	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		updateStatusBarColor();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitTrackAppearanceMode();
	}

	private void enterTrackAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitTrackAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_search_button);
		}
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
				return nightMode ? R.color.divider_color_dark : R.color.divider_color_light;
			} else {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
			}
		}
		return -1;
	}

	private void updateStatusBarColor() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.updateStatusBarColor();
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof ImportTrackCard) {
				importTrack();
			}
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof TracksToFollowCard) {
				if (buttonIndex >= 0) {
					loadAndFollowTrack((TracksToFollowCard) card, buttonIndex);
				}
			}
		}
	}

	private void loadAndFollowTrack(TracksToFollowCard card, int index) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && index < card.getGpxInfoList().size()) {
			GPXInfo gpxInfo = card.getGpxInfoList().get(index);
			String fileName = gpxInfo.getFileName();
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByName(fileName);
			if (selectedGpxFile != null) {
				selectTrackToFollow(selectedGpxFile.getGpxFile());
			} else {
				CallbackWithObject<GPXFile[]> callback = new CallbackWithObject<GPXFile[]>() {
					@Override
					public boolean processResult(GPXFile[] result) {
						selectTrackToFollow(result[0]);
						return true;
					}
				};
				File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
				GpxUiHelper.loadGPXFileInDifferentThread(mapActivity, callback, dir, null, fileName);
			}
		}
	}

	private void selectTrackToFollow(GPXFile gpxFile) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().setGPXRouteParams(gpxFile);
			app.getTargetPointsHelper().updateRouteAndRefresh(true);
			app.getRoutingHelper().recalculateRouteDueToSettingsChange();
		}
	}

	public void importTrack() {
		Intent intent = ImportHelper.getImportTrackIntent();
		try {
			startActivityForResult(intent, ImportHelper.IMPORT_FILE_REQUEST);
		} catch (ActivityNotFoundException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ImportHelper.IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				importHelper.setGpxImportCompleteListener(new ImportHelper.OnGpxImportCompleteListener() {
					@Override
					public void onComplete(boolean success) {
						importHelper.setGpxImportCompleteListener(null);
					}
				});
				if (!importHelper.handleGpxImport(uri, false, false)) {
					log.debug("import completed!");
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
			}
		}
	}

	private void setupButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.route_info_bg));

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		UiUtilities.setupDialogButton(isNightMode(), cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
	}

	private void setupScrollShadow() {
		int shadowIconId = isNightMode() ? R.drawable.bg_contextmenu_shadow : R.drawable.bg_contextmenu_shadow;
		final Drawable shadowIcon = app.getUIUtilities().getIcon(shadowIconId);

		final View scrollView = getBottomScrollView();
		final FrameLayout bottomContainer = getBottomContainer();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {

			@Override
			public void onScrollChanged() {
				int scrollY = scrollView.getScrollY();
				if (scrollY <= 0 && bottomContainer.getForeground() != null) {
					bottomContainer.setForeground(null);
				} else if (scrollY > 0 && bottomContainer.getForeground() == null) {
					bottomContainer.setForeground(shadowIcon);
				}
			}
		});
	}
}