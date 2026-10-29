package net.osmand.plus.mapcontextmenu.editors.icon;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController.ORIGINAL_KEY;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.data.BackgroundType;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController.DefaultFavoriteListener;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * "Original" in the icon picker (#25890): the point editor of a new favorite lists the POI icon there,
 * while the folder Default appearance keeps its own "Original" (each point keeps its icon).
 */
@RunWith(AndroidJUnit4.class)
public class EditorIconOriginalCategoryTest {

	private static final long POI_TYPES_INIT_TIMEOUT_MS = 30_000;
	// the icon of a POI type that has no icon of its own, so no category lists it
	private static final String ICON_ONLY_IN_ORIGINAL = "test_icon_only_in_original";

	private OsmandApplication app;
	private String poiIcon;

	@Before
	public void setUp() throws InterruptedException {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
		long deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS;
		while (!app.getPoiTypes().isInit() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertTrue("POI types are not initialized", app.getPoiTypes().isInit());

		PoiType pub = app.getPoiTypes().getPoiTypeByKey("pub");
		assertNotNull("No POI type pub", pub);
		AtomicReference<String> iconKey = new AtomicReference<>();
		EditorIconUtils.retrieveIconKey(pub, iconKey::set);
		poiIcon = iconKey.get();
		assertNotNull("No icon for POI type pub", poiIcon);
	}

	@Test
	public void originalCategoryListsOnlyThePoiIcon() {
		EditorIconController controller = createController(poiIcon, poiIcon);
		IconsCategory first = controller.getCategories().get(0);
		assertEquals(ORIGINAL_KEY, first.getKey());
		assertEquals(List.of(poiIcon), first.getIconKeys());

		assertNull("Original without a POI icon", findCategory(createController(poiIcon, null), ORIGINAL_KEY));
	}

	@Test
	public void poiIconOpensItsPoiCategory() {
		IconsCategory selected = createController(poiIcon, poiIcon).getSelectedCategory();
		assertNotEquals(ORIGINAL_KEY, selected.getKey());
		assertTrue(poiIcon + " is not in " + selected.getKey(), selected.containsIcon(poiIcon));
	}

	@Test
	public void poiIconInNoOtherCategoryOpensOriginal() {
		EditorIconController controller = createController(ICON_ONLY_IN_ORIGINAL, ICON_ONLY_IN_ORIGINAL);
		assertEquals(ORIGINAL_KEY, controller.getSelectedCategory().getKey());
	}

	@Test
	public void selectingOriginalKeepsThePickedIcon() {
		EditorIconController controller = createController(DEFAULT_ICON_NAME, poiIcon);
		runOnMainSync(() -> controller.setSelectedCategory(findCategory(controller, ORIGINAL_KEY)));
		assertEquals(DEFAULT_ICON_NAME, controller.getSelectedIconKey());
	}

	@Test
	public void folderEditorDoesNotShareThePointEditorPicker() {
		Fragment fragment = new Fragment();
		EditorIconController pointEditor = onMainThread(() ->
				EditorIconController.getInstance(app, fragment, poiIcon, poiIcon));
		EditorIconController folderEditor = onMainThread(() ->
				EditorIconController.getInstance(app, fragment, DEFAULT_ICON_NAME));
		try {
			assertNotSame(pointEditor, folderEditor);
			assertNull("Original in the folder editor", findCategory(folderEditor, ORIGINAL_KEY));
		} finally {
			app.getDialogManager().unregister(pointEditor.getProcessId());
			app.getDialogManager().unregister(folderEditor.getProcessId());
		}
	}

	@Test
	public void pointEditorPickerIsKeptForTheSameEditor() {
		Fragment fragment = new Fragment();
		EditorIconController controller = onMainThread(() ->
				EditorIconController.getInstance(app, fragment, poiIcon, poiIcon));
		try {
			assertSame(controller, onMainThread(() -> EditorIconController.getInstance(app, fragment, DEFAULT_ICON_NAME, poiIcon)));
		} finally {
			app.getDialogManager().unregister(controller.getProcessId());
		}
	}

	@Test
	public void pointEditorPickerOfAnotherPoiIsNotReused() {
		Fragment fragment = new Fragment();
		EditorIconController first = onMainThread(() ->
				EditorIconController.getInstance(app, fragment, poiIcon, poiIcon));
		EditorIconController second = onMainThread(() ->
				EditorIconController.getInstance(app, fragment, ICON_ONLY_IN_ORIGINAL, ICON_ONLY_IN_ORIGINAL));
		try {
			IconsCategory original = findCategory(second, ORIGINAL_KEY);
			assertNotNull("No Original for the second POI", original);
			assertEquals(List.of(ICON_ONLY_IN_ORIGINAL), original.getIconKeys());
		} finally {
			app.getDialogManager().unregister(first.getProcessId());
			app.getDialogManager().unregister(second.getProcessId());
		}
	}

	@Test
	public void folderWithoutIconOpensOriginalInDefaultAppearance() {
		FavoriteGroup group = new FavoriteGroup("EditorIconOriginalCategoryTest", new ArrayList<>(), 0, true, false);
		FavoriteAppearanceController controller = onMainThread(() ->
				new FavoriteAppearanceController(app, group, createFavoriteListener(null)));
		assertEquals(ORIGINAL_KEY, controller.getIconController().getSelectedCategory().getKey());
	}

	@NonNull
	private EditorIconController createController(@Nullable String selectedIconKey, @Nullable String originalIconKey) {
		return onMainThread(() -> {
			EditorIconController controller = new EditorIconController(app);
			controller.setSelectedIconKey(selectedIconKey);
			controller.setOriginalIconKey(originalIconKey);
			controller.init();
			return controller;
		});
	}

	@NonNull
	private DefaultFavoriteListener createFavoriteListener(@Nullable String iconKey) {
		return new DefaultFavoriteListener() {
			@SuppressWarnings("ConstantConditions")
			@NonNull
			@Override
			public String getOriginalIconKey() {
				return iconKey;
			}

			@Override
			public int getOriginalColor() {
				return 0;
			}

			@NonNull
			@Override
			public BackgroundType getOriginalShape() {
				return DEFAULT_BACKGROUND_TYPE;
			}
		};
	}

	@Nullable
	private IconsCategory findCategory(@NonNull EditorIconController controller, @NonNull String key) {
		for (IconsCategory category : controller.getCategories()) {
			if (key.equals(category.getKey())) {
				return category;
			}
		}
		return null;
	}

	@NonNull
	private <T> T onMainThread(@NonNull Supplier<T> supplier) {
		AtomicReference<T> result = new AtomicReference<>();
		runOnMainSync(() -> result.set(supplier.get()));
		return result.get();
	}

	private void runOnMainSync(@NonNull Runnable runnable) {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
	}
}
