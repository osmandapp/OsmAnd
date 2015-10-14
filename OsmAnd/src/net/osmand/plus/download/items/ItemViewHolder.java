package net.osmand.plus.download.items;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import java.text.DateFormat;
import java.util.Map;

public class ItemViewHolder {
	private final TextView nameTextView;
	private final TextView descrTextView;
	private final ImageView leftImageView;
	private final ImageView rightImageButton;
	private final Button rightButton;
	private final ProgressBar progressBar;
	private final TextView mapDateTextView;

	private final Map<String, String> indexFileNames;
	private final Map<String, String> indexActivatedFileNames;
	private final java.text.DateFormat dateFormat;

	private boolean srtmDisabled;
	private boolean nauticalPluginDisabled;
	private boolean freeVersion;
	private int textColorPrimary;
	private int textColorSecondary;
	private RightButtonAction rightButtonAction;

	private enum RightButtonAction {
		UNKNOWN,
		ASK_FOR_SEAMARKS_PLUGIN,
		ASK_FOR_SRTM_PLUGIN_PURCHASE,
		ASK_FOR_SRTM_PLUGIN_ENABLE,
		ASK_FOR_FULL_VERSION_PURCHASE
	}

	public ItemViewHolder(View convertView,
						  DateFormat dateFormat,
						  Map<String, String> indexFileNames,
						  Map<String, String> indexActivatedFileNames) {
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
		this.dateFormat = dateFormat;
		nameTextView = (TextView) convertView.findViewById(R.id.name);
		descrTextView = (TextView) convertView.findViewById(R.id.description);
		leftImageView = (ImageView) convertView.findViewById(R.id.leftImageView);
		rightImageButton = (ImageView) convertView.findViewById(R.id.rightImageButton);
		rightButton = (Button) convertView.findViewById(R.id.rightButton);
		progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
		mapDateTextView = (TextView) convertView.findViewById(R.id.mapDateTextView);

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = convertView.getContext().getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		textColorPrimary = typedValue.data;
		theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
		textColorSecondary = typedValue.data;
	}

	public void setSrtmDisabled(boolean srtmDisabled) {
		this.srtmDisabled = srtmDisabled;
	}

	public void setNauticalPluginDisabled(boolean nauticalPluginDisabled) {
		this.nauticalPluginDisabled = nauticalPluginDisabled;
	}

	public void setFreeVersion(boolean freeVersion) {
		this.freeVersion = freeVersion;
	}

	public void bindIndexItem(final IndexItem indexItem, final DownloadActivity context,
							  boolean showTypeInTitle, boolean showTypeInDesc) {
		boolean disabled = false;
		String textButtonCaption = "GET";
		rightButtonAction = RightButtonAction.UNKNOWN;

		if (indexItem.getType() == DownloadActivityType.VOICE_FILE) {
			nameTextView.setText(indexItem.getVisibleName(context,
					context.getMyApplication().getRegions(), false));
		} else {
			if (indexItem.getSimplifiedFileName().equals(ItemsListBuilder.WORLD_SEAMARKS_KEY) && nauticalPluginDisabled) {
				rightButtonAction = RightButtonAction.ASK_FOR_SEAMARKS_PLUGIN;
				disabled = true;
			}
			if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
					indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
				OsmandPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
				if (srtmPlugin == null || srtmPlugin.needsInstallation()) {
					rightButtonAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_PURCHASE;
				} else if (!srtmPlugin.isActive()) {
					rightButtonAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_ENABLE;
				}

				disabled = true;
			} else if (indexItem.getType() == DownloadActivityType.WIKIPEDIA_FILE && freeVersion) {
				rightButtonAction = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
				disabled = true;
			}
			if (showTypeInTitle) {
				nameTextView.setText(indexItem.getType().getString(context));
			} else {
				nameTextView.setText(indexItem.getVisibleName(context, context.getMyApplication().getRegions(), false));
			}
		}

		descrTextView.setVisibility(View.VISIBLE);
		if (!showTypeInTitle && (indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
				indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
			descrTextView.setText(indexItem.getType().getString(context));
		} else if (showTypeInDesc) {
			descrTextView.setText(indexItem.getType().getString(context) + "  •  " + indexItem.getSizeDescription(context));
		} else {
			descrTextView.setText(indexItem.getSizeDescription(context));
		}
		rightImageButton.setVisibility(View.VISIBLE);
		rightImageButton.setImageDrawable(getContextIcon(context, R.drawable.ic_action_import));
		progressBar.setVisibility(View.GONE);

		if (rightButtonAction != RightButtonAction.UNKNOWN) {
			rightButton.setText(textButtonCaption);
			rightButton.setVisibility(View.VISIBLE);
			rightImageButton.setVisibility(View.GONE);

			final RightButtonAction action = rightButtonAction;

			rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (action) {
						case ASK_FOR_FULL_VERSION_PURCHASE:
							Intent intent = new Intent(Intent.ACTION_VIEW,
									Uri.parse(Version.marketPrefix(context.getMyApplication())
											+ "net.osmand.plus"));
							context.startActivity(intent);
							break;
						case ASK_FOR_SEAMARKS_PLUGIN:
							context.startActivity(new Intent(context,
									context.getMyApplication().getAppCustomization().getPluginsActivity()));
							AccessibleToast.makeText(context.getApplicationContext(),
									context.getString(R.string.activate_seamarks_plugin), Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SRTM_PLUGIN_PURCHASE:
							OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
							context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
							break;
						case ASK_FOR_SRTM_PLUGIN_ENABLE:
							context.startActivity(new Intent(context,
									context.getMyApplication().getAppCustomization().getPluginsActivity()));
							AccessibleToast.makeText(context,
									context.getString(R.string.activate_srtm_plugin), Toast.LENGTH_SHORT).show();
							break;
					}
				}
			});

		} else {
			rightButton.setVisibility(View.GONE);
			rightImageButton.setVisibility(View.VISIBLE);
		}

		if (indexFileNames != null && indexItem.isAlreadyDownloaded(indexFileNames)) {
			boolean outdated = false;
			String date;
			if (indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) {
				date = indexItem.getDate(dateFormat);
			} else {
				String sfName = indexItem.getTargetFileName();
				final boolean updatableResource = indexActivatedFileNames.containsKey(sfName);
				date = updatableResource ? indexActivatedFileNames.get(sfName) : indexFileNames.get(sfName);
				outdated = DownloadActivity.downloadListIndexThread.checkIfItemOutdated(indexItem);
			}
			String updateDescr = context.getResources().getString(R.string.local_index_installed) + ": "
					+ date;
			mapDateTextView.setText(updateDescr);
			int colorId = outdated ? R.color.color_distance : R.color.color_ok;
			final int color = context.getResources().getColor(colorId);
			mapDateTextView.setTextColor(color);
			leftImageView.setImageDrawable(getContextIcon(context,
					indexItem.getType().getIconResource(), color));
			nameTextView.setTextColor(textColorPrimary);
		} else if (disabled) {
			leftImageView.setImageDrawable(getContextIcon(context,
					indexItem.getType().getIconResource(), textColorSecondary));
			nameTextView.setTextColor(textColorSecondary);
		} else {
			leftImageView.setImageDrawable(getContextIcon(context,
					indexItem.getType().getIconResource()));
			nameTextView.setTextColor(textColorPrimary);
		}
	}

	public void bindRegion(WorldRegion region, DownloadActivity context) {
		nameTextView.setText(region.getName());
		nameTextView.setTextColor(textColorPrimary);
		if (region.getResourceTypes().size() > 0) {
			StringBuilder stringBuilder = new StringBuilder();
			for (DownloadActivityType activityType : region.getResourceTypes()) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append(", ");
				}
				stringBuilder.append(activityType.getString(context));
			}
		}
		descrTextView.setVisibility(View.GONE);
		mapDateTextView.setVisibility(View.GONE);

		Drawable leftImageDrawable = null;
		switch (region.getMapState()) {
			case NOT_DOWNLOADED:
				leftImageDrawable = getContextIcon(context, R.drawable.ic_map);
				break;
			case DOWNLOADED:
				leftImageDrawable = getContextIcon(context, R.drawable.ic_map,
						context.getResources().getColor(R.color.color_ok));
				break;
			case OUTDATED:
				leftImageDrawable = getContextIcon(context, R.drawable.ic_map,
						context.getResources().getColor(R.color.color_distance));
				break;
		}
		leftImageView.setImageDrawable(leftImageDrawable);
		rightButton.setVisibility(View.GONE);
		rightImageButton.setVisibility(View.GONE);
		progressBar.setVisibility(View.GONE);
	}

	private Drawable getContextIcon(DownloadActivity context, int resourceId) {
		return context.getMyApplication().getIconsCache().getContentIcon(resourceId);
	}

	private Drawable getContextIcon(DownloadActivity context, int resourceId, int color) {
		return context.getMyApplication().getIconsCache().getPaintedContentIcon(resourceId, color);
	}

	public boolean isItemAvailable() {
		return rightButtonAction == RightButtonAction.UNKNOWN;
	}
}
