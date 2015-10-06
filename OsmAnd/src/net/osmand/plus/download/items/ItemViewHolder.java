package net.osmand.plus.download.items;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.srtmplugin.SRTMPlugin;

public class ItemViewHolder {
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(WorldItemsFragment.class);

	private final TextView nameTextView;
	private final TextView descrTextView;
	private final ImageView leftImageView;
	private final ImageView rightImageButton;
	private final Button rightButton;
	private final ProgressBar progressBar;

	private boolean srtmDisabled;
	private boolean nauticalPluginDisabled;
	private boolean freeVersion;
	private int textColorPrimary;
	private int textColorSecondary;

	private enum RightButtonAction {
		UNKNOWN,
		ASK_FOR_SEAMARKS_PLUGIN,
		ASK_FOR_SRTM_PLUGIN_PURCHASE,
		ASK_FOR_SRTM_PLUGIN_ENABLE,
		ASK_FOR_FULL_VERSION_PURCHASE
	}

	public ItemViewHolder(View convertView) {
		nameTextView = (TextView) convertView.findViewById(R.id.name);
		descrTextView = (TextView) convertView.findViewById(R.id.description);
		leftImageView = (ImageView) convertView.findViewById(R.id.leftImageView);
		rightImageButton = (ImageView) convertView.findViewById(R.id.rightImageButton);
		rightButton = (Button) convertView.findViewById(R.id.rightButton);
		progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);

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

	public void bindIndexItem(final IndexItem indexItem, final DownloadActivity context, boolean showTypeInTitle, boolean showTypeInDesc) {
		boolean disabled = false;
		String textButtonCaption = "GET";
		RightButtonAction rightButtonAction = RightButtonAction.UNKNOWN;

		if (indexItem.getType() == DownloadActivityType.VOICE_FILE) {
			nameTextView.setText(indexItem.getVisibleName(context,
					context.getMyApplication().getRegions()));
		} else {
			if (indexItem.getSimplifiedFileName().equals(ItemsListBuilder.WORLD_SEAMARKS_KEY) && nauticalPluginDisabled) {
				rightButtonAction = RightButtonAction.ASK_FOR_SEAMARKS_PLUGIN;
				disabled = true;
			}
			if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
					indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
				if (indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
					nameTextView.setText(context.getString(R.string.srtm_plugin_disabled));
				} else {
					nameTextView.setText(context.getString(R.string.hillshade_layer_disabled));
				}
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
			} else if (showTypeInTitle) {
				nameTextView.setText(indexItem.getType().getString(context));
			} else {
				nameTextView.setText(indexItem.getVisibleName(context, context.getMyApplication().getRegions()));
			}
		}

		if (!showTypeInTitle && (indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
				indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
			descrTextView.setText(indexItem.getType().getString(context));
		} else if (showTypeInDesc) {
			descrTextView.setText(indexItem.getType().getString(context) + "  •  " + indexItem.getSizeDescription(context));
		} else {
			descrTextView.setText(indexItem.getSizeDescription(context));
		}
		// TODO replace with imageView.
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
							AccessibleToast.makeText(context, "Please purchase Full version", Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SEAMARKS_PLUGIN:
							AccessibleToast.makeText(context.getApplicationContext(), "Please turn on Seamarks plugin", Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SRTM_PLUGIN_PURCHASE:
							AccessibleToast.makeText(context, "Please purchase SRTM plugin", Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SRTM_PLUGIN_ENABLE:
							AccessibleToast.makeText(context, "Please activate SRTM plugin", Toast.LENGTH_SHORT).show();
							break;
					}
				}
			});

		} else {
			rightButton.setVisibility(View.GONE);
			rightImageButton.setVisibility(View.VISIBLE);
		}

		if (disabled) {
			leftImageView.setImageDrawable(getContextIcon(context, indexItem.getType().getIconResource(), textColorSecondary));
			nameTextView.setTextColor(textColorSecondary);
		} else {
			leftImageView.setImageDrawable(getContextIcon(context, indexItem.getType().getIconResource()));
			nameTextView.setTextColor(textColorPrimary);
		}
	}

	public void bindRegion(WorldRegion region, DownloadActivity context) {
		nameTextView.setText(region.getName());
		if (region.getResourceTypes().size() > 0) {
			StringBuilder stringBuilder = new StringBuilder();
			for (DownloadActivityType activityType : region.getResourceTypes()) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append(", ");
				}
				stringBuilder.append(activityType.getString(context));
			}
			descrTextView.setText(stringBuilder.toString());
		} else {
			descrTextView.setText(R.string.shared_string_others);
		}
		leftImageView.setImageDrawable(getContextIcon(context, R.drawable.ic_map));
		rightImageButton.setVisibility(View.GONE);
		progressBar.setVisibility(View.GONE);
	}

	private Drawable getContextIcon(DownloadActivity context, int resourceId) {
		return context.getMyApplication().getIconsCache().getContentIcon(resourceId);
	}

	private Drawable getContextIcon(DownloadActivity context, int resourceId, int color) {
		return context.getMyApplication().getIconsCache().getPaintedContentIcon(resourceId, color);
	}
}
