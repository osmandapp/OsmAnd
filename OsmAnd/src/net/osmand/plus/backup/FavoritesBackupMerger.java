package net.osmand.plus.backup;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.settings.backend.backup.items.FavoritesSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Three-way merge of Favorite groups using the last successfully synced local snapshot. */
final class FavoritesBackupMerger {

	private static final Log LOG = PlatformUtil.getLog(FavoritesBackupMerger.class);
	private static final String SNAPSHOT_DIR = "favorites_sync";
	// Upload callbacks run in parallel, while applying a group mutates shared Favorites state.
	private static final Object MERGE_FINISH_LOCK = new Object();

	private FavoritesBackupMerger() {
	}

	// Preparation only changes the in-memory upload payload. It never changes local Favorites.
	static void prepareMergeUploads(@NonNull OsmandApplication app,
	                                @NonNull BackupHelper backupHelper,
	                                @NonNull BackupInfo info) {
		for (Pair<LocalFile, RemoteFile> conflict : new ArrayList<>(info.filesToMerge)) {
			LocalFile localFile = conflict.first;
			if (!(localFile.item instanceof FavoritesSettingsItem localItem)) {
				continue;
			}
			try {
				FavoriteGroup local = localItem.getSingleGroup();
				if (local == null || local.isPersonal()) {
					continue;
				}
				FavoriteGroup base = loadSnapshot(app, conflict.second.getName(), localFile.uploadTime);
				if (base == null) {
					continue;
				}
				FavoriteGroup remote = downloadGroup(app, backupHelper, conflict.second);
				int defaultColor = ContextCompat.getColor(app, R.color.color_favorite);
				FavoriteGroup merged = mergeGroups(base, local, remote, defaultColor);
				if (merged != null) {
					localFile.item = new MergeUploadItem(app, localItem, local, merged,
							localFile.localModifiedTime, localFile.uploadTime);
					info.filesToUpload.add(localFile);
					info.filesToMerge.remove(conflict);
				}
			} catch (RuntimeException e) {
				LOG.warn("Failed to prepare Favorites merge for " + conflict.second.getName(), e);
			}
		}
	}

	static void onUploadSuccess(@NonNull OsmandApplication app, @NonNull SettingsItem item,
	                            @NonNull String fileName, long uploadTime) {
		if (uploadTime <= 0 || !(item instanceof FavoritesSettingsItem favoritesItem)) {
			return;
		}
		if (!fileName.equals(BackupUtils.getItemFileName(item))) {
			return;
		}
		try {
			if (favoritesItem instanceof MergeUploadItem mergeItem) {
				synchronized (MERGE_FINISH_LOCK) {
					mergeItem.finishUpload(fileName, uploadTime);
				}
			} else if (favoritesItem.getLocalModifiedTime() == favoritesItem.getLastModifiedTime()) {
				saveSnapshot(app, favoritesItem.getSingleGroup(), fileName, uploadTime);
			} else {
				favoritesItem.setLocalModifiedTime(Math.max(System.currentTimeMillis(), uploadTime + 1));
			}
		} catch (RuntimeException e) {
			LOG.warn("Failed to update Favorites sync snapshot for " + fileName, e);
		}
	}

	static void onDownloadSuccess(@NonNull OsmandApplication app, @NonNull SettingsItem item,
	                              @NonNull RemoteFile remoteFile) {
		if (!(item instanceof FavoritesSettingsItem favoritesItem)) {
			return;
		}
		FavoriteGroup downloaded = favoritesItem.getSingleGroup();
		FavoriteGroup current = downloaded != null
				? app.getFavoritesHelper().getGroup(downloaded.getName()) : null;
		int defaultColor = ContextCompat.getColor(app, R.color.color_favorite);
		if (downloaded != null && sameGroup(downloaded, current, defaultColor)) {
			saveSnapshot(app, downloaded, remoteFile.getName(), remoteFile.getUpdatetimems());
		}
	}

	@Nullable
	private static FavoriteGroup downloadGroup(@NonNull OsmandApplication app,
	                                           @NonNull BackupHelper backupHelper,
	                                           @NonNull RemoteFile remoteFile) {
		if (remoteFile.isDeleted()) {
			return null;
		}
		File tempFile = null;
		try {
			tempFile = File.createTempFile("favorites-merge-", ".gpx", FileUtils.getTempDir(app));
			String error = backupHelper.downloadFile(tempFile, remoteFile, null, false);
			return Algorithms.isEmpty(error) ? readGroup(SharedUtil.loadGpxFile(tempFile)) : null;
		} catch (IOException | UserNotRegisteredException e) {
			LOG.warn("Failed to download Favorites for merge: " + remoteFile.getName(), e);
			return null;
		} finally {
			if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
				LOG.warn("Failed to delete temporary Favorites file " + tempFile);
			}
		}
	}

	@Nullable
	private static FavoriteGroup loadSnapshot(@NonNull OsmandApplication app,
	                                          @NonNull String fileName, long syncTime) {
		File file = snapshotFile(app, fileName);
		return file != null && file.exists() && file.lastModified() == syncTime
				? readGroup(SharedUtil.loadGpxFile(file)) : null;
	}

	private static void saveSnapshot(@NonNull OsmandApplication app, @Nullable FavoriteGroup group,
	                                 @NonNull String fileName, long syncTime) {
		File file = snapshotFile(app, fileName);
		if (group == null || file == null || syncTime <= 0) {
			return;
		}
		File dir = file.getParentFile();
		if (dir == null || !dir.exists() && !dir.mkdirs()) {
			return;
		}
		Exception error = app.getFavoritesHelper().getFileHelper()
				.saveFile(Collections.singletonList(group), file);
		if (error != null || !file.setLastModified(syncTime)) {
			if (file.exists() && !file.delete()) {
				LOG.warn("Failed to delete invalid Favorites sync snapshot " + file);
			}
			if (error != null) {
				LOG.warn("Failed to save Favorites sync snapshot for " + fileName, error);
			} else {
				LOG.warn("Failed to timestamp Favorites sync snapshot for " + fileName);
			}
		}
	}

	@Nullable
	private static File snapshotFile(@NonNull OsmandApplication app,
	                                 @NonNull String fileName) {
		if (!new File(fileName).getName().equals(fileName)) {
			return null;
		}
		return new File(new File(app.getNoBackupFilesDir(), SNAPSHOT_DIR), fileName);
	}

	@Nullable
	private static FavoriteGroup readGroup(@NonNull GpxFile gpx) {
		if (gpx.getError() != null || gpx.getPointsGroups().size() != 1) {
			return null;
		}
		FavoriteGroup group = FavoriteGroup.fromPointsGroup(
				gpx.getPointsGroups().values().iterator().next());
		return group.getPoints().size() == gpx.getPointsList().size() ? group : null;
	}

	/**
	 * Merges point changes made on only one side. A conflict is returned when both sides
	 * changed the same base point, both added the same name, or group appearance changed.
	 */
	@Nullable
	static FavoriteGroup mergeGroups(@Nullable FavoriteGroup base,
	                                 @Nullable FavoriteGroup local,
	                                 @Nullable FavoriteGroup remote,
	                                 int defaultColor) {
		if (base == null || local == null || remote == null
				|| !sameAppearance(base, local, defaultColor)
				|| !sameAppearance(base, remote, defaultColor)) {
			return null;
		}
		Map<String, FavouritePoint> basePoints = pointsByName(base);
		Map<String, FavouritePoint> localPoints = pointsByName(local);
		Map<String, FavouritePoint> remotePoints = pointsByName(remote);
		if (basePoints == null || localPoints == null || remotePoints == null) {
			return null;
		}

		int inheritedColor = base.getColor() == 0 ? defaultColor : base.getColor();
		Map<String, FavouritePoint> localAdditions = new TreeMap<>(localPoints);
		Map<String, FavouritePoint> remoteAdditions = new TreeMap<>(remotePoints);
		localAdditions.keySet().removeAll(basePoints.keySet());
		remoteAdditions.keySet().removeAll(basePoints.keySet());
		List<FavouritePoint> mergedPoints = new ArrayList<>();
		for (Map.Entry<String, FavouritePoint> entry : basePoints.entrySet()) {
			FavouritePoint basePoint = entry.getValue();
			FavouritePoint localPoint = localPoints.remove(entry.getKey());
			FavouritePoint remotePoint = remotePoints.remove(entry.getKey());
			if (localPoint == null && remotePoint == null) {
				if (hasRename(basePoint, localAdditions, inheritedColor)
						|| hasRename(basePoint, remoteAdditions, inheritedColor)) {
					return null;
				}
				continue;
			}
			boolean localUnchanged = samePoint(basePoint, localPoint, inheritedColor);
			boolean remoteUnchanged = samePoint(basePoint, remotePoint, inheritedColor);
			if (!localUnchanged && !remoteUnchanged) {
				return null;
			}
			FavouritePoint result = localUnchanged ? remotePoint : localPoint;
			if (result != null) {
				mergedPoints.add(copyPoint(result));
			}
		}

		Map<String, FavouritePoint> additions = new TreeMap<>(localPoints);
		for (FavouritePoint point : remotePoints.values()) {
			if (additions.putIfAbsent(point.getName(), point) != null) {
				return null;
			}
		}
		for (FavouritePoint point : additions.values()) {
			mergedPoints.add(copyPoint(point));
		}

		FavoriteGroup merged = new FavoriteGroup(local);
		merged.setPoints(mergedPoints);
		return merged;
	}

	@Nullable
	private static Map<String, FavouritePoint> pointsByName(@NonNull FavoriteGroup group) {
		Map<String, FavouritePoint> points = new LinkedHashMap<>();
		for (FavouritePoint point : group.getPoints()) {
			String name = point.getName();
			if (Algorithms.isEmpty(name) || !Algorithms.isEmpty(point.getLinks())
					|| points.put(name, point) != null) {
				return null;
			}
		}
		return points;
	}

	private static boolean hasRename(@NonNull FavouritePoint basePoint,
	                                 @NonNull Map<String, FavouritePoint> additions,
	                                 int defaultColor) {
		for (FavouritePoint point : additions.values()) {
			if (samePointContent(basePoint, point, defaultColor)) {
				return true;
			}
		}
		return false;
	}

	private static boolean sameAppearance(@NonNull FavoriteGroup first,
	                                      @NonNull FavoriteGroup second,
	                                      int defaultColor) {
		return Objects.equals(first.getName(), second.getName())
				&& sameColor(first.getColor(), second.getColor(), defaultColor)
				&& Objects.equals(normalizeIcon(first.getIconName()), normalizeIcon(second.getIconName()))
				&& first.getBackgroundType() == second.getBackgroundType()
				&& first.isVisible() == second.isVisible()
				&& first.isPinned() == second.isPinned();
	}

	private static boolean sameGroup(@NonNull FavoriteGroup first,
	                                 @Nullable FavoriteGroup second,
	                                 int defaultColor) {
		if (second == null || !sameAppearance(first, second, defaultColor)) {
			return false;
		}
		Map<String, FavouritePoint> secondPoints = pointsByName(second);
		if (secondPoints == null || first.getPoints().size() != secondPoints.size()) {
			return false;
		}
		int inheritedColor = first.getColor() == 0 ? defaultColor : first.getColor();
		for (FavouritePoint point : first.getPoints()) {
			if (!samePoint(point, secondPoints.get(point.getName()), inheritedColor)) {
				return false;
			}
		}
		return true;
	}

	// FavouritePoint.equals() omits address/comment/visibility and compares serialized noise.
	private static boolean samePoint(@NonNull FavouritePoint first,
	                                 @Nullable FavouritePoint second,
	                                 int defaultColor) {
		return second != null
				&& Objects.equals(first.getName(), second.getName())
				&& samePointContent(first, second, defaultColor);
	}

	private static boolean samePointContent(@NonNull FavouritePoint first,
	                                        @NonNull FavouritePoint second,
	                                        int defaultColor) {
		return Objects.equals(first.getCategory(), second.getCategory())
				&& sameText(first.getDescription(), second.getDescription())
				&& sameText(first.getAddress(), second.getAddress())
				&& sameText(first.getComment(), second.getComment())
				&& MapUtils.getDistance(first.getLatitude(), first.getLongitude(),
						second.getLatitude(), second.getLongitude()) < 0.1
				&& sameTime(first.getVisitedDate(), second.getVisitedDate())
				&& sameTime(first.getPickupDate(), second.getPickupDate())
				&& first.getCalendarEvent() == second.getCalendarEvent()
				&& sameColor(first.getColor(), second.getColor(), defaultColor)
				&& Objects.equals(first.getIconName(), second.getIconName())
				&& first.getBackgroundType() == second.getBackgroundType()
				&& first.isVisible() == second.isVisible();
	}

	private static boolean sameText(@Nullable String first, @Nullable String second) {
		return Objects.equals(first, second) || Algorithms.isEmpty(first) && Algorithms.isEmpty(second);
	}

	private static boolean sameColor(int first, int second, int defaultColor) {
		return (first == 0 ? defaultColor : first) == (second == 0 ? defaultColor : second);
	}

	@NonNull
	private static String normalizeIcon(@Nullable String icon) {
		return Algorithms.isEmpty(icon) ? GpxUtilities.DEFAULT_ICON_NAME : icon;
	}

	private static boolean sameTime(long first, long second) {
		return first == second || first > 0 && second > 0 && Math.abs(first - second) < 1_000;
	}

	@NonNull
	private static FavouritePoint copyPoint(@NonNull FavouritePoint point) {
		FavouritePoint copy = new FavouritePoint(point);
		copy.setSpecialPointType(point.getSpecialPointType());
		return copy;
	}

	@NonNull
	private static FavoriteGroup copyGroup(@NonNull FavoriteGroup group) {
		FavoriteGroup copy = new FavoriteGroup(group);
		List<FavouritePoint> points = new ArrayList<>();
		for (FavouritePoint point : group.getPoints()) {
			points.add(copyPoint(point));
		}
		copy.setPoints(points);
		return copy;
	}

	private static final class MergeUploadItem extends FavoritesSettingsItem {
		private final FavoriteGroup localGroup;
		private final FavoriteGroup mergedGroup;
		private final long sourceModifiedTime;
		private final long baseSyncTime;

		MergeUploadItem(@NonNull OsmandApplication app, @NonNull FavoritesSettingsItem baseItem,
		                @NonNull FavoriteGroup localGroup, @NonNull FavoriteGroup mergedGroup,
		                long sourceModifiedTime, long baseSyncTime) {
			super(app, baseItem, Collections.singletonList(mergedGroup));
			this.localGroup = copyGroup(localGroup);
			this.mergedGroup = mergedGroup;
			this.sourceModifiedTime = sourceModifiedTime;
			this.baseSyncTime = baseSyncTime;
			setLastModifiedTime(sourceModifiedTime);
		}

		void finishUpload(@NonNull String fileName, long uploadTime) {
			int defaultColor = ContextCompat.getColor(app, R.color.color_favorite);
			FavoriteGroup current = app.getFavoritesHelper().getGroup(localGroup.getName());
			if (!sameGroup(mergedGroup, current, defaultColor)
					&& sameGroup(localGroup, current, defaultColor)) {
				setShouldReplace(true);
				processDuplicateItems();
				apply();
				current = app.getFavoritesHelper().getGroup(mergedGroup.getName());
			}
			if (sameGroup(mergedGroup, current, defaultColor)) {
				setLocalModifiedTime(sourceModifiedTime);
				saveSnapshot(app, mergedGroup, fileName, uploadTime);
			} else {
				// Preserve the old common base so the next preparation keeps both sides in conflict.
				app.getBackupHelper().updateFileUploadTime(
						getType().name(), fileName, baseSyncTime);
				setLocalModifiedTime(Math.max(System.currentTimeMillis(), uploadTime + 1));
			}
		}
	}
}
