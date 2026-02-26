package net.osmand.plus.myplaces.favorites.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static net.osmand.plus.settings.enums.FavoriteListSortMode.*;
import static net.osmand.plus.utils.AndroidUtils.dpToPx;
import static net.osmand.plus.utils.UpdateLocationUtils.getFormattedDistance;
import static net.osmand.plus.utils.UpdateLocationUtils.updateDirectionDrawable;

import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.settings.enums.FavoriteListSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationInfo;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.Date;

public class FavoriteViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final boolean nightMode;
	private final UpdateLocationViewCache locationViewCache;

	private final TextView title;
	private final TextView description;
	private final TextView prefixDescription;
	private final TextView suffixDescription;
	private final ImageView imageView;
	private final CompoundButton checkbox;
	protected final View checkboxContainer;
	private final View menuButton;
	private final View divider;
	private final ImageView directionIcon;
	private final LinearLayout infoContainer;

	public FavoriteViewHolder(@NonNull View itemView, UpdateLocationViewCache locationViewCache, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.nightMode = nightMode;
		this.locationViewCache = locationViewCache;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		directionIcon = itemView.findViewById(R.id.direction_icon);
		checkbox = itemView.findViewById(R.id.checkbox);
		menuButton = itemView.findViewById(R.id.menu_button);
		checkboxContainer = itemView.findViewById(R.id.checkbox_container);
		imageView = itemView.findViewById(R.id.icon);
		divider = itemView.findViewById(R.id.divider);
		prefixDescription = itemView.findViewById(R.id.prefix_description);
		suffixDescription = itemView.findViewById(R.id.suffix_description);
		infoContainer = itemView.findViewById(R.id.info_container);
		LinearLayout contentContainer = itemView.findViewById(R.id.content_container);

		LinearLayout.LayoutParams contentParams = (LinearLayout.LayoutParams) contentContainer.getLayoutParams();
		contentParams.setMarginStart(dpToPx(app, 10));
		contentContainer.setLayoutParams(contentParams);

		setupSelectionMode();
	}

	private void setupSelectionMode() {
		AndroidUiHelper.updateVisibility(menuButton, true);
		AndroidUiHelper.updateVisibility(checkboxContainer, false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.direction_icon), false);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkbox);
	}

	public void bindView(@NonNull FavoriteListSortMode sortMode, @NonNull FavouritePoint favouritePoint,
	                     boolean showDivider, boolean selectionMode, FavoriteAdapterListener listener) {
		itemView.setOnLongClickListener(v -> {
			listener.onItemLongClick(favouritePoint);
			return true;
		});
		itemView.setOnClickListener(v -> listener.onItemSingleClick(favouritePoint));
		menuButton.setOnClickListener(v -> listener.onActionButtonClick(favouritePoint, menuButton));

		title.setText(favouritePoint.getDisplayName(app), TextView.BufferType.SPANNABLE);
		title.setMaxLines(2);

		int color = app.getFavoritesHelper().getColorWithCategory(favouritePoint, ColorUtilities.getColor(app, R.color.color_favorite));
		imageView.setImageDrawable(PointImageUtils.getFromPoint(app, color, false, favouritePoint));

		int iconSize = (int) app.getResources().getDimension(R.dimen.favorites_my_places_icon_size);
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) imageView.getLayoutParams();
		lp.width = iconSize;
		lp.height = iconSize;
		imageView.setLayoutParams(lp);
		imageView.setVisibility(VISIBLE);

		bindLocation(sortMode, favouritePoint);

		boolean showSuffix = sortMode == NAME_ASCENDING || sortMode == NAME_DESCENDING
				|| sortMode == NEAREST || sortMode == FARTHEST;
		if (showSuffix) {
			String category = " | " + AndroidUtils.truncateWithEllipsis(favouritePoint.getCategoryDisplayName(app), 12);
			if (!Algorithms.isEmpty(category)) {
				suffixDescription.setText(category);
				suffixDescription.setVisibility(VISIBLE);
			} else {
				suffixDescription.setVisibility(GONE);
			}
		} else {
			suffixDescription.setVisibility(GONE);
		}

		prefixDescription.setMaxLines(1);
		description.setMaxLines(1);
		suffixDescription.setMaxLines(1);
		directionIcon.setVisibility(VISIBLE);

		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.checkbox_container), selectionMode);
		AndroidUiHelper.updateVisibility(divider, showDivider);

		bindSelectionMode(selectionMode, listener, favouritePoint);
	}

	@Nullable
	public static String prepareAddress(@Nullable String address) {
		if (Algorithms.isEmpty(address)) {
			return address;
		}
		int commaIndex = address.indexOf(',');

		if (commaIndex == -1) {
			return address;
		}

		String street = address.substring(0, commaIndex).trim();
		String city = address.substring(commaIndex + 1).trim();

		if (street.isEmpty() || city.isEmpty()) {
			return address;
		}

		return city + ", " + street;
	}

	public void bindSelectionMode(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavouritePoint favouritePoint) {
		AndroidUiHelper.updateVisibility(checkboxContainer, selectionMode);
		AndroidUiHelper.updateVisibility(menuButton, !selectionMode);

		LinearLayout.LayoutParams infoParams = (LinearLayout.LayoutParams) infoContainer.getLayoutParams();
		infoParams.setMarginEnd(dpToPx(app, selectionMode ? 16 : 0));
		infoContainer.setLayoutParams(infoParams);

		checkbox.setChecked(listener.isItemSelected(favouritePoint));
	}

	public void bindSelectionToggle(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavouritePoint favouritePoint) {
		if (selectionMode) {
			checkbox.setChecked(listener.isItemSelected(favouritePoint));
		}
	}

	public void bindLocation(@NonNull FavoriteListSortMode sortMode, @NonNull FavouritePoint favouritePoint) {
		SpannableStringBuilder spannable = new SpannableStringBuilder();

		LatLon toLoc = new LatLon(favouritePoint.getLatitude(), favouritePoint.getLongitude());
		UpdateLocationInfo info = new UpdateLocationInfo(app, null, toLoc);
		CharSequence distance = getFormattedDistance(app, info, locationViewCache);
		updateDirectionDrawable(app, directionIcon, info, locationViewCache);
		spannable.append(distance);

		if (favouritePoint.isAddressSpecified()) {
			spannable.append(" • ");
			spannable.append(prepareAddress(favouritePoint.getAddress()));
		}
		if (sortMode == DATE_ASCENDING || sortMode == DATE_DESCENDING) {
			StringBuilder dateString = new StringBuilder();
			long creationTime = favouritePoint.getTimestamp();
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			dateString.append(format.format(new Date(creationTime)));
			dateString.append(" | ");
			prefixDescription.setText(dateString);
			prefixDescription.setVisibility(VISIBLE);
		} else {
			prefixDescription.setVisibility(GONE);
		}
		description.setText(spannable);
	}
}