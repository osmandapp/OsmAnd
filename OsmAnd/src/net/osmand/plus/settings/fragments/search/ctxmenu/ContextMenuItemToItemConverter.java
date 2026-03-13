package net.osmand.plus.settings.fragments.search.ctxmenu;

import com.codepoetics.ambivalence.Either;

import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class ContextMenuItemToItemConverter {

	private ContextMenuItemToItemConverter() {
	}

	public static List<Either<SimpleItem<ContextMenuItem>, CategoryItem<ContextMenuItem, ContextMenuItem>>> asItems(final List<ContextMenuItem> items) {
		return asItems(nullValuesToEmpty(ContextMenuUtils.collectItemsByCategories(items)));
	}

	private static Map<ContextMenuItem, Optional<List<ContextMenuItem>>> nullValuesToEmpty(final Map<ContextMenuItem, List<ContextMenuItem>> childrenByCategory) {
		return childrenByCategory
				.entrySet()
				.stream()
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								entry -> Optional.ofNullable(entry.getValue())));
	}

	private static List<Either<SimpleItem<ContextMenuItem>, CategoryItem<ContextMenuItem, ContextMenuItem>>> asItems(final Map<ContextMenuItem, Optional<List<ContextMenuItem>>> childrenByCategory) {
		return childrenByCategory
				.entrySet()
				.stream()
				.map(entry -> asItem(entry.getKey(), entry.getValue()))
				.toList();
	}

	private static Either<SimpleItem<ContextMenuItem>, CategoryItem<ContextMenuItem, ContextMenuItem>> asItem(final ContextMenuItem category, final Optional<List<ContextMenuItem>> children) {
		return children
				.<Either<SimpleItem<ContextMenuItem>, CategoryItem<ContextMenuItem, ContextMenuItem>>>map(_children -> Either.ofRight(asCategoryItem(category, _children)))
				.orElseGet(() -> Either.ofLeft(new SimpleItem<>(category)));
	}

	private static CategoryItem<ContextMenuItem, ContextMenuItem> asCategoryItem(final ContextMenuItem category, final List<ContextMenuItem> children) {
		return new CategoryItem<>(category, asSimpleItems(children));
	}

	private static List<SimpleItem<ContextMenuItem>> asSimpleItems(final List<ContextMenuItem> contextMenuItems) {
		return contextMenuItems
				.stream()
				.map(SimpleItem::new)
				.toList();
	}
}
