package net.osmand.shared.media.library

enum class MediaLibrarySortMode(val group: Group) {
	NEAREST(Group.LOCATION), LAST_MODIFIED(Group.LOCATION),
	NAME_A_Z(Group.NAME), NAME_Z_A(Group.NAME),
	NEWEST_FIRST(Group.DATE), OLDEST_FIRST(Group.DATE),
	SIZE_LARGE_SMALL(Group.SIZE), SIZE_SMALL_LARGE(Group.SIZE),
	DURATION_LONG_SHORT(Group.DURATION), DURATION_SHORT_LONG(Group.DURATION);

	enum class Group { LOCATION, NAME, DATE, SIZE, DURATION }
}
