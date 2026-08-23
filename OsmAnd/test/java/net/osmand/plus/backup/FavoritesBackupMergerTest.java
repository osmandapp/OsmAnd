package net.osmand.plus.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class FavoritesBackupMergerTest {
	private static final int DEFAULT_COLOR = 0xffeecc22;

	@Test
	public void mergesIndependentAdditions() {
		FavoriteGroup base = group(point("A", 1));
		FavoriteGroup local = group(point("A", 1), point("B", 2));
		FavoriteGroup remote = group(point("A", 1), point("C", 3));

		FavoriteGroup merged = FavoritesBackupMerger.mergeGroups(base, local, remote, DEFAULT_COLOR);

		assertNotNull(merged);
		assertEquals(Arrays.asList("A", "B", "C"), names(merged));
	}

	@Test
	public void rejectsSameNameAdditions() {
		FavoriteGroup base = group(point("A", 1));
		assertNull(FavoritesBackupMerger.mergeGroups(base,
				group(point("A", 1), point("B", 2)),
				group(point("A", 1), point("B", 2)), DEFAULT_COLOR));
		assertNull(FavoritesBackupMerger.mergeGroups(base,
				group(point("A", 1), point("B", 2)),
				group(point("A", 1), point("B", 3)), DEFAULT_COLOR));
	}

	@Test
	public void mergesChangesOfDifferentPoints() {
		FavoriteGroup base = group(point("A", 1), point("B", 2), point("C", 3),
				point("D", 4), point("E", 5));
		FavouritePoint editedA = point("A", 1);
		editedA.setDescription("edited");
		FavouritePoint movedC = point("C", 30);
		FavoriteGroup local = group(editedA, point("C", 3), point("D2", 4), point("E", 5));
		FavoriteGroup remote = group(point("A", 1), point("B", 2), movedC, point("D", 4), point("F", 6));

		FavoriteGroup merged = FavoritesBackupMerger.mergeGroups(base, local, remote, DEFAULT_COLOR);

		assertNotNull(merged);
		assertEquals(Arrays.asList("A", "C", "D2", "F"), names(merged));
		assertEquals("edited", merged.getPoints().get(0).getDescription());
		assertEquals(30, merged.getPoints().get(1).getLatitude(), 0);
	}

	@Test
	public void rejectsChangesOfSamePoint() {
		FavoriteGroup base = group(point("A", 1));
		FavouritePoint edited = point("A", 1);
		edited.setDescription("edited");
		assertNull(FavoritesBackupMerger.mergeGroups(base, group(edited),
				group(point("A", 2)), DEFAULT_COLOR));
		assertNull(FavoritesBackupMerger.mergeGroups(base, group(),
				group(edited), DEFAULT_COLOR));
		assertNull(FavoritesBackupMerger.mergeGroups(base, group(), group(), DEFAULT_COLOR));
	}

	@Test
	public void rejectsGroupAppearanceChanges() {
		FavoriteGroup base = group(point("A", 1));
		FavoriteGroup remote = group(point("A", 1));
		remote.setColor(0xff00ff00);
		assertNull(FavoritesBackupMerger.mergeGroups(base,
				group(point("A", 1)), remote, DEFAULT_COLOR));
	}

	@Test
	public void acceptsIosSerializationOfUnchangedPoint() {
		FavouritePoint basePoint = point("A", 1);
		basePoint.setLatitude(12.3456789);
		basePoint.setLongitude(23.4567891);
		basePoint.setAltitude(123.45);
		basePoint.setTimestamp(1_787_483_581_234L);

		FavouritePoint localPoint = new FavouritePoint(basePoint);
		FavouritePoint remotePoint = new FavouritePoint(basePoint);
		remotePoint.setLatitude(12.3456790);
		remotePoint.setLongitude(23.4567890);
		remotePoint.setAltitude(123.4);
		remotePoint.setTimestamp(1_787_483_581_000L);
		remotePoint.setDescription("");
		remotePoint.setColor(DEFAULT_COLOR);

		FavoriteGroup base = group(basePoint);
		FavoriteGroup local = group(localPoint, point("B", 2));
		FavoriteGroup remote = group(remotePoint, point("C", 3));
		remote.setColor(DEFAULT_COLOR);

		FavoriteGroup merged = FavoritesBackupMerger.mergeGroups(
				base, local, remote, DEFAULT_COLOR);

		assertNotNull(merged);
		assertEquals(Arrays.asList("A", "B", "C"), names(merged));

		int inheritedColor = 0xff336699;
		base.setColor(inheritedColor);
		local.setColor(inheritedColor);
		remote.setColor(inheritedColor);
		remotePoint.setColor(inheritedColor);
		assertNotNull(FavoritesBackupMerger.mergeGroups(base, local, remote, DEFAULT_COLOR));
	}

	@Test
	public void acceptsOneSidedPointMove() {
		FavouritePoint basePoint = point("A", 1);
		FavouritePoint movedPoint = new FavouritePoint(basePoint);
		movedPoint.setLatitude(basePoint.getLatitude() + 0.00001);

		FavoriteGroup merged = FavoritesBackupMerger.mergeGroups(group(basePoint),
				group(new FavouritePoint(basePoint)), group(movedPoint), DEFAULT_COLOR);
		assertNotNull(merged);
		assertEquals(movedPoint.getLatitude(), merged.getPoints().get(0).getLatitude(), 0);
	}

	private static FavouritePoint point(String name, int coordinate) {
		FavouritePoint point = new FavouritePoint(coordinate, coordinate, name, "group");
		point.setTimestamp(coordinate * 1_000L);
		return point;
	}

	private static FavoriteGroup group(FavouritePoint... points) {
		return new FavoriteGroup("group", new ArrayList<>(Arrays.asList(points)), 0, true, false);
	}

	private static List<String> names(FavoriteGroup group) {
		List<String> names = new ArrayList<>();
		for (FavouritePoint point : group.getPoints()) {
			names.add(point.getName());
		}
		return names;
	}
}
