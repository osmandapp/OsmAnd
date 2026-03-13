package net.osmand.plus.settings.fragments.search.ctxmenu;

import java.util.List;

record CategoryItem<T, U>(T category, List<SimpleItem<U>> children) {
}
