package net.osmand.plus.track;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.Elevation;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TrackColoringCard extends BaseCard {

	private final static String SOLID_COLOR = "solid_color";
	private static final Log log = PlatformUtil.getLog(TrackColoringCard.class);

	private GPXTrackAnalysis gpxTrackAnalysis;
	private TrackDrawInfo trackDrawInfo;

	private TrackColoringAdapter coloringAdapter;
	private TrackAppearanceItem selectedAppearanceItem;
	private List<TrackAppearanceItem> appearanceItems;

	public TrackColoringCard(MapActivity mapActivity, GPXTrackAnalysis gpxTrackAnalysis, TrackDrawInfo trackDrawInfo) {
		super(mapActivity);
		this.trackDrawInfo = trackDrawInfo;
		this.gpxTrackAnalysis = gpxTrackAnalysis;
		appearanceItems = getTrackAppearanceItems();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_coloring_card;
	}

	@Override
	protected void updateContent() {
		updateHeader();

		coloringAdapter = new TrackColoringAdapter(appearanceItems);
		RecyclerView groupRecyclerView = view.findViewById(R.id.recycler_view);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		groupRecyclerView.setAdapter(coloringAdapter);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.top_divider), isShowDivider());
	}

	public void updateColor() {
		if (coloringAdapter != null) {
			// Provide empty object to update item without animation
			coloringAdapter.notifyItemChanged(0, new Object());
		}
	}

	public GradientScaleType getSelectedScaleType() {
		String attrName = selectedAppearanceItem.getAttrName();
		return attrName.equals(SOLID_COLOR) ? null : GradientScaleType.valueOf(attrName.toUpperCase());
	}

	private List<TrackAppearanceItem> getTrackAppearanceItems() {
		List<TrackAppearanceItem> items = new ArrayList<>();
		items.add(new TrackAppearanceItem(SOLID_COLOR, app.getString(R.string.track_coloring_solid), R.drawable.ic_action_circle, true));
		for (GradientScaleType scaleType : GradientScaleType.values()) {
			items.add(new TrackAppearanceItem(scaleType.getTypeName(),
					scaleType.getHumanString(app), scaleType.getIconId(), isScaleTypeActive(scaleType)));
		}
		return items;
	}

	private boolean isScaleTypeActive(GradientScaleType scaleType) {
		if (scaleType == GradientScaleType.SPEED) {
			return gpxTrackAnalysis.isSpeedSpecified();
		} else {
			if (!gpxTrackAnalysis.isElevationSpecified()) {
				return false;
			}
			for (Elevation elevation : gpxTrackAnalysis.elevationData) {
				if (Float.isNaN(elevation.elevation)) {
					return false;
				}
			}
			return true;
		}
	}

	private TrackAppearanceItem getSelectedAppearanceItem() {
		if (selectedAppearanceItem == null) {
			GradientScaleType scaleType = trackDrawInfo.getGradientScaleType();
			for (TrackAppearanceItem item : appearanceItems) {
				if (scaleType == null && item.getAttrName().equals(SOLID_COLOR)
						|| scaleType != null && scaleType.getTypeName().equals(item.getAttrName())) {
					selectedAppearanceItem = item;
					break;
				}
			}
		}
		return selectedAppearanceItem;
	}

	private void updateHeader() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		View headerView = view.findViewById(R.id.header_view);
		headerView.setBackgroundDrawable(null);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.shared_string_color);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setText(getSelectedAppearanceItem().getLocalizedValue());
	}

	public void setGradientScaleType(TrackAppearanceItem item) {
		selectedAppearanceItem = item;
		if (item.getAttrName().equals(SOLID_COLOR)) {
			trackDrawInfo.setGradientScaleType(null);
		} else {
			trackDrawInfo.setGradientScaleType(GradientScaleType.valueOf(item.getAttrName().toUpperCase()));
		}
		mapActivity.refreshMap();

		updateHeader();
	}

	private class TrackColoringAdapter extends RecyclerView.Adapter<AppearanceViewHolder> {

		private List<TrackAppearanceItem> items;

		private TrackColoringAdapter(List<TrackAppearanceItem> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public AppearanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater themedInflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_height);
			return new AppearanceViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull final AppearanceViewHolder holder, int position) {
			final TrackAppearanceItem item = items.get(position);

			if (item.isActive() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, holder.button, nightMode, R.drawable.ripple_solid_light_6dp,
						R.drawable.ripple_solid_dark_6dp);
			}

			updateButtonBg(holder, item);
			updateTextAndIconColor(holder, item);

			holder.title.setText(item.getLocalizedValue());
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (!item.isActive()) {
						showSnackbar(view, item.getAttrName());
						return;
					}

					int prevSelectedPosition = getItemPosition(getSelectedAppearanceItem());
					selectedAppearanceItem = items.get(holder.getAdapterPosition());
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(prevSelectedPosition);
					setGradientScaleType(selectedAppearanceItem);
					if (getListener() != null) {
						getListener().onCardPressed(TrackColoringCard.this);
					}
				}
			});
		}

		private void updateButtonBg(AppearanceViewHolder holder, TrackAppearanceItem item) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				Context ctx = holder.itemView.getContext();
				boolean itemSelected = getSelectedAppearanceItem() != null && getSelectedAppearanceItem().equals(item);

				int strokeColor;
				int backgroundColor;
				int strokeWidth;

				if (itemSelected) {
					strokeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.colorPrimary);
					backgroundColor = 0;
					strokeWidth = 2;
				} else if (!item.isActive()) {
					strokeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.stroked_buttons_and_links_outline);
					backgroundColor = AndroidUtils.getColorFromAttr(ctx, R.attr.ctx_menu_card_btn);
					strokeWidth = 2;
				} else {
					strokeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.stroked_buttons_and_links_outline);
					backgroundColor = 0;
					strokeWidth = 1;
				}

				rectContourDrawable.mutate();
				rectContourDrawable.setColor(backgroundColor);
				rectContourDrawable.setStroke(AndroidUtils.dpToPx(ctx, strokeWidth), strokeColor);
				holder.button.setImageDrawable(rectContourDrawable);
			}
		}

		private void updateTextAndIconColor(AppearanceViewHolder holder, TrackAppearanceItem item) {
			Context ctx = holder.itemView.getContext();
			boolean isSelected = item.getAttrName().equals(getSelectedAppearanceItem().getAttrName());
			int iconColorId;
			int textColorId;

			if (isSelected) {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.default_icon_color);
				textColorId = AndroidUtils.getColorFromAttr(ctx, android.R.attr.textColor);
			} else if (!item.isActive()) {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.default_icon_color);
				textColorId = AndroidUtils.getColorFromAttr(ctx, android.R.attr.textColorSecondary);
			} else {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.colorPrimary);
				textColorId = iconColorId;
			}

			if (item.getAttrName().equals(SOLID_COLOR)) {
				iconColorId = trackDrawInfo.getColor();
			}

			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(item.getIconId(), iconColorId));
			holder.title.setTextColor(textColorId);
		}

		private void showSnackbar(View view, String attrName) {
			if (view == null || mapActivity == null) {
				return;
			}
			String text = attrName.equals(GradientScaleType.SPEED.getTypeName()) ?
					app.getString(R.string.track_has_no_speed) : app.getString(R.string.track_has_no_altitude);
			text += " " + app.getString(R.string.select_another_colorization);
			Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
					.setAnchorView(mapActivity.findViewById(R.id.dismiss_button));
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		int getItemPosition(TrackAppearanceItem name) {
			return items.indexOf(name);
		}
	}

	public static class TrackAppearanceItem {

		private String attrName;
		private String localizedValue;

		@DrawableRes
		private int iconId;

		private boolean isActive;

		public TrackAppearanceItem(String attrName, String localizedValue, int iconId, boolean isActive) {
			this.attrName = attrName;
			this.localizedValue = localizedValue;
			this.iconId = iconId;
			this.isActive = isActive;
		}

		public String getAttrName() {
			return attrName;
		}

		public String getLocalizedValue() {
			return localizedValue;
		}

		public int getIconId() {
			return iconId;
		}

		public boolean isActive() {
			return isActive;
		}
	}
}