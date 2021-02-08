package net.osmand.plus.track;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

import static net.osmand.plus.myplaces.TrackActivityFragmentAdapter.isGpxFileSelected;
import static net.osmand.plus.track.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;

public class OverviewCard extends BaseCard {

	private View showButton;
	private View appearanceButton;
	private View editButton;
	private View directionsButton;
	private final SegmentActionsListener actionsListener;
	private final SelectedGpxFile selectedGpxFile;
	private final GpxBlockStatisticsBuilder blockStatisticsBuilder;

	public OverviewCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
						@NonNull SegmentActionsListener actionsListener, SelectedGpxFile selectedGpxFile) {
		super(mapActivity);
		this.actionsListener = actionsListener;
		this.selectedGpxFile = selectedGpxFile;
		blockStatisticsBuilder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, displayHelper);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_overview_fragment;
	}

	@Override
	protected void updateContent() {
		int iconColorDef = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		int iconColorPres = R.color.active_buttons_and_links_text_dark;
		GPXFile gpxFile = getGPXFile();
		boolean fileAvailable = gpxFile.path != null && !gpxFile.showCurrentTrack;

		showButton = view.findViewById(R.id.show_button);
		appearanceButton = view.findViewById(R.id.appearance_button);
		editButton = view.findViewById(R.id.edit_button);
		directionsButton = view.findViewById(R.id.directions_button);
		RecyclerView blocksView = view.findViewById(R.id.recycler_overview);
		blockStatisticsBuilder.setBlocksView(blocksView);

		initShowButton(iconColorDef, iconColorPres);
		initAppearanceButton(iconColorDef, iconColorPres);
		if (fileAvailable) {
			initEditButton(iconColorDef, iconColorPres);
			initDirectionsButton(iconColorDef, iconColorPres);
		}
		blockStatisticsBuilder.initStatBlocks(actionsListener, getActiveColor(), nightMode);
	}

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	@DrawableRes
	private int getActiveShowHideIcon() {
		return isGpxFileSelected(app, getGPXFile()) ? R.drawable.ic_action_view : R.drawable.ic_action_hide;
	}

	private void initShowButton(final int iconColorDef, final int iconColorPres) {
		initButton(showButton, SHOW_ON_MAP_BUTTON_INDEX, getActiveShowHideIcon(), iconColorDef, iconColorPres);
	}

	private void initAppearanceButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(appearanceButton, APPEARANCE_BUTTON_INDEX, R.drawable.ic_action_appearance, iconColorDef, iconColorPres);
	}

	private void initEditButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(editButton, EDIT_BUTTON_INDEX, R.drawable.ic_action_edit_dark, iconColorDef, iconColorPres);
	}

	private void initDirectionsButton(@ColorRes int iconColorDef, @ColorRes int iconColorPres) {
		initButton(directionsButton, DIRECTIONS_BUTTON_INDEX, R.drawable.ic_action_gdirections_dark, iconColorDef, iconColorPres);
	}

	private void initButton(View item, final int buttonIndex, @DrawableRes Integer iconResId,
							@ColorRes final int iconColorDef, @ColorRes int iconColorPres) {
		final AppCompatImageView icon = item.findViewById(R.id.image);
		final AppCompatImageView filled = item.findViewById(R.id.filled);
		filled.setImageResource(nightMode ? R.drawable.bg_plugin_logo_enabled_dark : R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(0.1f);
		setImageDrawable(icon, iconResId, iconColorDef);
		setOnTouchItem(item, icon, filled, iconResId, iconColorDef, iconColorPres);
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(OverviewCard.this, buttonIndex);
					if (buttonIndex == SHOW_ON_MAP_BUTTON_INDEX) {
						setImageDrawable(icon, getActiveShowHideIcon(), iconColorDef);
					}
				}
			}
		});
	}

	private void setImageDrawable(ImageView iv, @DrawableRes Integer resId, @ColorRes int color) {
		Drawable icon = resId != null ? app.getUIUtilities().getIcon(resId, color)
				: UiUtilities.tintDrawable(iv.getDrawable(), getResolvedColor(color));
		iv.setImageDrawable(icon);
	}

	private void setOnTouchItem(View item, final ImageView image, final ImageView filled, @DrawableRes final Integer resId, @ColorRes final int colorDef, @ColorRes final int colorPres) {
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