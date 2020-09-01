package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.internal.FlowLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.GpxAppearanceAdapter.AppearanceListItem;
import net.osmand.plus.dialogs.GpxAppearanceAdapter.GpxAppearanceAdapterType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.dialogs.GpxAppearanceAdapter.getAppearanceItems;

public class TrackColoringCard extends BaseCard implements ColorPickerListener {

	private static final int INVALID_VALUE = -1;
	private static final String SOLID_COLOR = "solid_color";
	private static final Log log = PlatformUtil.getLog(TrackColoringCard.class);

	private TrackDrawInfo trackDrawInfo;

	private TrackColoringAdapter coloringAdapter;
	private TrackAppearanceItem selectedAppearanceItem;
	private List<TrackAppearanceItem> appearanceItems;

	private List<Integer> customColors;
	private Fragment target;

	public TrackColoringCard(MapActivity mapActivity, TrackDrawInfo trackDrawInfo, Fragment target) {
		super(mapActivity);
		this.target = target;
		this.trackDrawInfo = trackDrawInfo;
		appearanceItems = getGradientAppearanceItems();
		customColors = getCustomColors();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_coloring_card;
	}

	@Override
	protected void updateContent() {
		updateHeader();
		createColorSelector();
		updateColorSelector();

		coloringAdapter = new TrackColoringAdapter(appearanceItems);
		RecyclerView groupRecyclerView = view.findViewById(R.id.recycler_view);
		groupRecyclerView.setAdapter(coloringAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.top_divider), isShowDivider());
	}

	private List<Integer> getCustomColors() {
		List<Integer> colors = new ArrayList<>();
		List<String> colorNames = app.getSettings().CUSTOM_TRACK_COLORS.getStringsList();
		if (colorNames != null) {
			for (String colorHex : colorNames) {
				try {
					if (!Algorithms.isEmpty(colorHex)) {
						int color = Algorithms.parseColor(colorHex);
						colors.add(color);
					}
				} catch (IllegalArgumentException e) {
					log.error(e);
				}
			}
		}

		return colors;
	}

	private List<TrackAppearanceItem> getGradientAppearanceItems() {
		List<TrackAppearanceItem> items = new ArrayList<>();
		items.add(new TrackAppearanceItem(SOLID_COLOR, app.getString(R.string.track_coloring_solid), R.drawable.ic_action_circle));

//		for (GradientScaleType scaleType : GradientScaleType.values()) {
//			items.add(new TrackAppearanceItem(scaleType.getTypeName(), scaleType.getHumanString(app), scaleType.getIconId()));
//		}

		return items;
	}

	private void createColorSelector() {
		FlowLayout selectColor = view.findViewById(R.id.select_color);
		selectColor.removeAllViews();

		for (int color : customColors) {
			selectColor.addView(createColorItemView(color, selectColor, true));
		}
		if (customColors.size() < 6) {
			selectColor.addView(createAddCustomColorItemView(selectColor));
		}
		selectColor.addView(createDividerView(selectColor));

		List<Integer> colors = new ArrayList<>();
		for (AppearanceListItem appearanceListItem : getAppearanceItems(app, GpxAppearanceAdapterType.TRACK_COLOR)) {
			if (!colors.contains(appearanceListItem.getColor())) {
				colors.add(appearanceListItem.getColor());
			}
		}
		for (int color : colors) {
			selectColor.addView(createColorItemView(color, selectColor, false));
		}
		updateColorSelector(trackDrawInfo.getColor(), selectColor);
	}

	private View createColorItemView(@ColorInt final int color, final FlowLayout rootView, boolean customColor) {
		View colorItemView = createCircleView(rootView);
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);

		Drawable transparencyIcon = getTransparencyIcon(app, color);
		Drawable colorIcon = app.getUIUtilities().getPaintedIcon(R.drawable.bg_point_circle, color);
		Drawable layeredIcon = UiUtilities.getLayeredIcon(transparencyIcon, colorIcon);
		backgroundCircle.setImageDrawable(layeredIcon);
		backgroundCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateColorSelector(color, rootView);
				coloringAdapter.notifyDataSetChanged();
				trackDrawInfo.setColor(color);

				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(TrackColoringCard.this);
				}
			}
		});
		if (customColor) {
			backgroundCircle.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						CustomColorBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), target, color);
					}
					return false;
				}
			});
		}
		colorItemView.setTag(color);
		return colorItemView;
	}

	private Drawable getTransparencyIcon(OsmandApplication app, @ColorInt int color) {
		int colorWithoutAlpha = UiUtilities.removeAlpha(color);
		int transparencyColor = UiUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f);
		return app.getUIUtilities().getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor);
	}

	private View createAddCustomColorItemView(FlowLayout rootView) {
		View colorItemView = createCircleView(rootView);
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);

		int bgColorId = nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
		Drawable backgroundIcon = app.getUIUtilities().getIcon(R.drawable.bg_point_circle, bgColorId);

		ImageView icon = colorItemView.findViewById(R.id.icon);
		icon.setVisibility(View.VISIBLE);
		int activeColorResId = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_plus, activeColorResId));

		backgroundCircle.setImageDrawable(backgroundIcon);
		backgroundCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					CustomColorBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), target, null);
				}
			}
		});
		return colorItemView;
	}

	private View createDividerView(FlowLayout rootView) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		View divider = themedInflater.inflate(R.layout.simple_divider_item, rootView, false);

		LinearLayout dividerContainer = new LinearLayout(view.getContext());
		dividerContainer.addView(divider);
		dividerContainer.setPadding(0, AndroidUtils.dpToPx(app, 1), 0, AndroidUtils.dpToPx(app, 5));

		return dividerContainer;
	}

	private View createCircleView(ViewGroup rootView) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		View circleView = themedInflater.inflate(R.layout.point_editor_button, rootView, false);
		ImageView outline = circleView.findViewById(R.id.outline);
		int colorId = nightMode ? R.color.stroked_buttons_and_links_outline_dark : R.color.stroked_buttons_and_links_outline_light;
		Drawable contourIcon = app.getUIUtilities().getIcon(R.drawable.bg_point_circle_contour, colorId);
		outline.setImageDrawable(contourIcon);
		return circleView;
	}

	private void updateColorSelector(int color, View rootView) {
		View oldColor = rootView.findViewWithTag(trackDrawInfo.getColor());
		if (oldColor != null) {
			oldColor.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColor.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(icon.getDrawable(), R.color.icon_color_default_light));
		}
		View newColor = rootView.findViewWithTag(color);
		if (newColor != null) {
			newColor.findViewById(R.id.outline).setVisibility(View.VISIBLE);
		}
		mapActivity.refreshMap();
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
		titleView.setText(R.string.select_color);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setText(getSelectedAppearanceItem().getLocalizedValue());
	}

	private void updateColorSelector() {
		boolean visible = getSelectedAppearanceItem().getAttrName().equals(SOLID_COLOR);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.select_color), visible);
	}

	public void setGradientScaleType(TrackAppearanceItem item) {
		if (item.getAttrName().equals(SOLID_COLOR)) {
			trackDrawInfo.setGradientScaleType(null);
		} else {
			trackDrawInfo.setGradientScaleType(GradientScaleType.valueOf(item.getAttrName()));
		}
		mapActivity.refreshMap();

		updateHeader();
		updateColorSelector();
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		if (prevColor == null && customColors.size() < 6) {
			customColors.add(newColor);
			trackDrawInfo.setColor(newColor);
		} else if (!Algorithms.isEmpty(customColors)) {
			int index = customColors.indexOf(prevColor);
			if (index != INVALID_VALUE) {
				customColors.set(index, newColor);
			}
		}
		updateContent();
	}

	private class TrackColoringAdapter extends RecyclerView.Adapter<TrackAppearanceViewHolder> {

		private List<TrackAppearanceItem> items;

		private TrackColoringAdapter(List<TrackAppearanceItem> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public TrackAppearanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater themedInflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_height);

			TrackAppearanceViewHolder holder = new TrackAppearanceViewHolder(view);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, holder.button, nightMode, R.drawable.ripple_solid_light_6dp,
						R.drawable.ripple_solid_dark_6dp);
			}
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull final TrackAppearanceViewHolder holder, int position) {
			TrackAppearanceItem item = items.get(position);
			holder.title.setText(item.getLocalizedValue());

			updateButtonBg(holder, item);

			int colorId;
			if (item.getAttrName().equals(SOLID_COLOR)) {
				colorId = trackDrawInfo.getColor();
			} else if (item.getAttrName().equals(getSelectedAppearanceItem().getAttrName())) {
				colorId = ContextCompat.getColor(app, nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light);
			} else {
				colorId = AndroidUtils.getColorFromAttr(holder.itemView.getContext(), R.attr.default_icon_color);
			}

			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(item.getIconId(), colorId));

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int prevSelectedPosition = getItemPosition(getSelectedAppearanceItem());
					selectedAppearanceItem = items.get(holder.getAdapterPosition());
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(prevSelectedPosition);

					setGradientScaleType(selectedAppearanceItem);
				}
			});
		}

		private void updateButtonBg(TrackAppearanceViewHolder holder, TrackAppearanceItem item) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				if (getSelectedAppearanceItem() != null && getSelectedAppearanceItem().equals(item)) {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), strokeColor);
				} else {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
				}
				holder.button.setImageDrawable(rectContourDrawable);
			}
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

		public TrackAppearanceItem(String attrName, String localizedValue, int iconId) {
			this.attrName = attrName;
			this.localizedValue = localizedValue;
			this.iconId = iconId;
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
	}
}