package net.osmand.plus.myplaces.favorites.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static net.osmand.plus.settings.enums.FavoriteListSortMode.*;
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
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationInfo;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FavoriteViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;

	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final TextView prefixDescription;
	private final ImageView imageView;
	private final CompoundButton checkbox;
	protected final View checkboxContainer;
	private final View menuButton;
	private final View divider;
	private final ImageView directionIcon;

	public FavoriteViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		directionIcon = itemView.findViewById(R.id.direction_icon);
		checkbox = itemView.findViewById(R.id.checkbox);
		menuButton = itemView.findViewById(R.id.menu_button);
		checkboxContainer = itemView.findViewById(R.id.checkbox_container);
		imageView = itemView.findViewById(R.id.icon);
		divider = itemView.findViewById(R.id.divider);
		prefixDescription = itemView.findViewById(R.id.prefix_description);

		setupSelectionMode();
	}

	private void setupSelectionMode() {
		AndroidUiHelper.updateVisibility(menuButton, true);
		AndroidUiHelper.updateVisibility(checkboxContainer, false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.direction_icon), false);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkbox);
	}

	public void bindView(@NonNull FavoriteListSortMode sortMode, @NonNull FavouritePoint favouritePoint,
	                     boolean showDivider, boolean selectionMode, UpdateLocationViewCache cache, FavoriteAdapterListener listener) {
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

		SpannableStringBuilder spannable = new SpannableStringBuilder();

		LatLon toLoc = new LatLon(favouritePoint.getLatitude(), favouritePoint.getLongitude());
		LatLon specialFrom = cache == null ? null : cache.specialFrom;
		UpdateLocationInfo info = new UpdateLocationInfo(app, specialFrom, toLoc);
		updateDirectionDrawable(app, directionIcon, info, cache);
		CharSequence distance = getFormattedDistance(app, info, cache);
		spannable.append(distance);

		if (favouritePoint.isAddressSpecified()) {
			spannable.append(" • ");
			spannable.append(prepareAddress(favouritePoint.getAddress()));
		}
		if (sortMode == DATE_ASCENDING || sortMode == DATE_DESCENDING) {
			StringBuilder dateString = new StringBuilder();
			long creationTime = favouritePoint.getTimestamp();
			DateFormat format = new SimpleDateFormat("d.MM.yyyy", Locale.getDefault());
			dateString.append(format.format(new Date(creationTime)));
			dateString.append(" | ");
			prefixDescription.setText(dateString);
			prefixDescription.setVisibility(VISIBLE);
		} else {
			spannable.append(" | ");
			long creationTime = favouritePoint.getTimestamp();
			DateFormat format = new SimpleDateFormat("d.MM.yyyy", Locale.getDefault());
			spannable.append(format.format(new Date(creationTime)));
			prefixDescription.setVisibility(GONE);
		}
		description.setText(spannable);
		prefixDescription.setMaxLines(1);
		description.setMaxLines(1);
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

		checkbox.setChecked(listener.isItemSelected(favouritePoint));
	}

	public void bindSelectionToggle(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavouritePoint favouritePoint) {
		if (selectionMode) {
			checkbox.setChecked(listener.isItemSelected(favouritePoint));
		}
	}
}