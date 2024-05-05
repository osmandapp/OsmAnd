package net.osmand.plus.track.cards;

import static net.osmand.plus.track.cards.DescriptionCard.getMetadataImageLink;
import static net.osmand.plus.track.cards.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.cards.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;
import static net.osmand.gpx.GpxParameter.NEAREST_CITY_NAME;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.isGpxFileSelected;
import static net.osmand.plus.utils.AndroidUtils.dpToPx;
import static net.osmand.plus.wikipedia.WikiArticleHelper.getFirstParagraph;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.GpxBlockStatisticsBuilder;
import net.osmand.plus.track.fragments.ReadGpxDescriptionFragment;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class OverviewCard extends MapBaseCard {

	private View showButton;
	private View appearanceButton;
	private View editButton;
	private View directionsButton;
	private TextView description;
	private final SegmentActionsListener actionsListener;
	private final SelectedGpxFile selectedGpxFile;
	private final GpxBlockStatisticsBuilder blockStatisticsBuilder;
	private final GPXTrackAnalysis analysis;
	private final GpxDataItem dataItem;
	private final Fragment targetFragment;

	public GpxBlockStatisticsBuilder getBlockStatisticsBuilder() {
		return blockStatisticsBuilder;
	}

	public OverviewCard(@NonNull MapActivity mapActivity, @NonNull SegmentActionsListener actionsListener,
	                    @NonNull SelectedGpxFile selectedGpxFile, @Nullable GPXTrackAnalysis analysis,
	                    @Nullable GpxDataItem dataItem, @NonNull Fragment targetFragment) {
		super(mapActivity);
		this.actionsListener = actionsListener;
		this.selectedGpxFile = selectedGpxFile;
		this.analysis = analysis;
		this.dataItem = dataItem;
		this.targetFragment = targetFragment;
		blockStatisticsBuilder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_overview_fragment;
	}

	@Override
	public void updateContent() {
		int iconColorDef = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		int iconColorPres = R.color.active_buttons_and_links_text_dark;
		GPXFile gpxFile = getGPXFile();
		boolean fileAvailable = gpxFile.path != null && !gpxFile.showCurrentTrack;

		showButton = view.findViewById(R.id.show_button);
		appearanceButton = view.findViewById(R.id.appearance_button);
		editButton = view.findViewById(R.id.edit_button);
		directionsButton = view.findViewById(R.id.directions_button);
		description = view.findViewById(R.id.description);
		RecyclerView blocksView = view.findViewById(R.id.recycler_overview);
		blockStatisticsBuilder.setBlocksView(blocksView, true);

		setupDescription();
		initShowButton(iconColorDef, iconColorPres);
		if (!FileUtils.isTempFile(app, gpxFile.path)) {
			initAppearanceButton(iconColorDef, iconColorPres);
			if (fileAvailable) {
				initEditButton(iconColorDef, iconColorPres);
			}
		}
		if (fileAvailable) {
			initDirectionsButton(iconColorDef, iconColorPres);
		}
		GPXTrackAnalysis analysis = selectedGpxFile.getFilteredSelectedGpxFile() != null
				? selectedGpxFile.getFilteredSelectedGpxFile().getTrackAnalysis(app)
				: this.analysis;
		blockStatisticsBuilder.initStatBlocks(actionsListener, getActiveColor(), analysis);

		if (blocksView.getVisibility() == View.VISIBLE && description.getVisibility() == View.VISIBLE) {
			AndroidUtils.setPadding(description, 0, 0, 0, dpToPx(app, 12));
		}
		setupRegion();
	}

	private void setupRegion() {
		String cityName = dataItem != null ? dataItem.getParameter(NEAREST_CITY_NAME) : null;
		TextView regionText = view.findViewById(R.id.region);
		regionText.setText(cityName);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.region_container), !Algorithms.isEmpty(cityName));
	}

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	@DrawableRes
	private int getActiveShowHideIcon() {
		if (FileUtils.isTempFile(app, getGPXFile().path)) {
			return R.drawable.ic_action_gsave_dark;
		} else {
			return isGpxFileSelected(app, getGPXFile()) ? R.drawable.ic_action_hide : R.drawable.ic_action_view;
		}
	}

	private void initShowButton(int iconColorDef, int iconColorPres) {
		initButton(showButton, SHOW_ON_MAP_BUTTON_INDEX, getActiveShowHideIcon(), iconColorDef, iconColorPres);
	}

	private void initAppearanceButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(appearanceButton, APPEARANCE_BUTTON_INDEX, R.drawable.ic_action_appearance, iconColorDef, iconColorPres);
	}

	private void initEditButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(editButton, EDIT_BUTTON_INDEX, R.drawable.ic_action_edit_track, iconColorDef, iconColorPres);
	}

	private void initDirectionsButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(directionsButton, DIRECTIONS_BUTTON_INDEX, R.drawable.ic_action_gdirections_dark, iconColorDef, iconColorPres);
	}

	private void initButton(View item, int buttonIndex, @DrawableRes Integer iconResId,
	                        @ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		item.setVisibility(View.VISIBLE);
		AppCompatImageView icon = item.findViewById(R.id.image);
		AppCompatImageView filled = item.findViewById(R.id.filled);
		filled.setImageResource(nightMode ? R.drawable.bg_plugin_logo_enabled_dark : R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(0.1f);
		setImageDrawable(icon, iconResId, iconColorDef);
		setOnTouchItem(item, icon, filled, iconResId, iconColorDef, iconColorPres);
		item.setOnClickListener(v -> {
			CardListener listener = getListener();
			if (listener != null) {
				notifyButtonPressed(buttonIndex);
				if (buttonIndex == SHOW_ON_MAP_BUTTON_INDEX) {
					setImageDrawable(icon, getActiveShowHideIcon(), iconColorDef);
				}
			}
		});
	}

	private void setImageDrawable(ImageView iv, @DrawableRes Integer resId, @ColorRes int color) {
		Drawable icon = resId != null ? app.getUIUtilities().getIcon(resId, color)
				: UiUtilities.tintDrawable(iv.getDrawable(), getResolvedColor(color));
		iv.setImageDrawable(icon);
	}

	private void setupDescription() {
		GPXFile gpxFile = getGPXFile();
		String descriptionHtml = gpxFile.metadata.getDescription();
		if (Algorithms.isBlank(descriptionHtml)) {
			AndroidUiHelper.updateVisibility(description, false);
		} else {
			description.setText(getFirstParagraph(descriptionHtml));
			description.setOnClickListener(v -> {
				String title = gpxFile.getArticleTitle();
				String imageUrl = getMetadataImageLink(gpxFile.metadata);
				ReadGpxDescriptionFragment.showInstance(mapActivity, title, imageUrl, descriptionHtml, targetFragment);
			});
			AndroidUiHelper.updateVisibility(description, true);
		}
	}

	private void setOnTouchItem(View item, ImageView image, ImageView filled, @DrawableRes Integer resId, @ColorRes int colorDef, @ColorRes int colorPres) {
		item.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN: {
						filled.setAlpha(1f);
						setImageDrawable(image, resId, colorPres);
						break;
					}
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL: {
						filled.setAlpha(0.1f);
						setImageDrawable(image, resId, colorDef);
						break;
					}
				}
				return false;
			}
		});
	}
}