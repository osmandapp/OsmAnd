package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.AttachedMediaSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.mediastorage.MediaSource;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.media.MediaFileNameFormat;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.*;

public class AttachedMediaExportType extends AbstractExportType {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaExportType.class);

	private static final String HTTP_SCHEME = "http://";
	private static final String HTTPS_SCHEME = "https://";

	@Override
	public int getTitleId() {
		return R.string.attached_media;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_folder_av_notes;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		if (!offlineBackup) {
			return Collections.emptyList();
		}
		return collectSettingsItems(app, app.getFavoritesHelper().getFavoriteGroups());
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		return Collections.singletonList(settingsItem);
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		return object instanceof AttachedMediaSettingsItem;
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.FILE;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.ATTACHED_MEDIA);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return null;
	}

	@NonNull
	public static List<AttachedMediaSettingsItem> collectSettingsItems(@NonNull OsmandApplication app, @NonNull Collection<FavoriteGroup> groups) {
		AttachedMediaDataHelper helper = new AttachedMediaDataHelper(app);
		Map<String, AttachedMediaSettingsItem> itemsBySourceId = new LinkedHashMap<>();
		Set<String> usedNames = collectExistingMediaFileNames(app);
		for (Link link : helper.collectMediaLinks(groups)) {
			String href = link.getHref() != null ? link.getHref().trim() : null;
			if (Algorithms.isEmpty(href) || isRemoteHref(href)) {
				continue;
			}
			MediaSource source = helper.resolveExportMediaSource(href);
			if (source == null) {
				continue;
			}
			AttachedMediaSettingsItem existing = itemsBySourceId.get(source.getId());
			if (existing != null) {
				existing.addHrefKey(href);
				continue;
			}
			try {
				String targetName = assignTargetFileName(app, source, usedNames);
				AttachedMediaSettingsItem item = new AttachedMediaSettingsItem(app, source, targetName);
				item.addHrefKey(href);
				itemsBySourceId.put(source.getId(), item);
			} catch (IllegalArgumentException e) {
				LOG.warn("Failed to create attached media settings item: " + href, e);
			}
		}
		if (PluginsHelper.isDevelopment()) {
			LOG.debug("Attached media export sources: items=" + itemsBySourceId.size());
		}
		return new ArrayList<>(itemsBySourceId.values());
	}

	@NonNull
	private static Set<String> collectExistingMediaFileNames(@NonNull OsmandApplication app) {
		Set<String> res = new HashSet<>();
		File[] files = app.getAppPath(IndexConstants.AV_INDEX_DIR).listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					res.add(file.getName());
				}
			}
		}
		return res;
	}

	private static boolean isRemoteHref(@NonNull String href) {
		String lowerCase = href.toLowerCase(Locale.US);
		return lowerCase.startsWith(HTTP_SCHEME) || lowerCase.startsWith(HTTPS_SCHEME);
	}

	@NonNull
	private static String assignTargetFileName(@NonNull OsmandApplication app,
			@NonNull MediaSource source, @NonNull Set<String> usedNames) {
		String name = source.getFileName();
		if (shouldGenerateTargetFileName(app, source, name, usedNames)) {
			String extension = Algorithms.isEmpty(name) ? "" : Algorithms.getFileNameExtension(name);
			if (Algorithms.isEmpty(extension)) {
				extension = source.getDirType().getExtension();
			}
			name = MediaFileNameFormat.createUniqueMediaFileName(extension, usedNames::contains);
		}
		usedNames.add(name);
		return name;
	}

	private static boolean shouldGenerateTargetFileName(@NonNull OsmandApplication app,
			@NonNull MediaSource source, @Nullable String name, @NonNull Set<String> usedNames) {
		if (isSameInternalMediaFile(app, source, name)) {
			return false;
		}
		if (Algorithms.isEmpty(name) || !MediaFileNameFormat.isManagedMediaFileName(name)) {
			return true;
		}
		if (!usedNames.contains(name)) {
			return false;
		}
		File target = new File(app.getAppPath(IndexConstants.AV_INDEX_DIR), name);
		return !Algorithms.stringsEqual(source.getId(), target.getAbsolutePath());
	}

	private static boolean isSameInternalMediaFile(@NonNull OsmandApplication app,
			@NonNull MediaSource source, @Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return false;
		}
		File target = new File(app.getAppPath(IndexConstants.AV_INDEX_DIR), name);
		return Algorithms.stringsEqual(source.getId(), target.getAbsolutePath());
	}
}
