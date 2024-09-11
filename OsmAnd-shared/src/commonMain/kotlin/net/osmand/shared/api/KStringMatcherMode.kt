package net.osmand.shared.api

enum class KStringMatcherMode {
	// tests only first word as base starts with part
	CHECK_ONLY_STARTS_WITH,

	// tests all words (split by space) and one of word should start with a given part
	CHECK_STARTS_FROM_SPACE,

	// tests all words except first (split by space) and one of word should start with a given part
	CHECK_STARTS_FROM_SPACE_NOT_BEGINNING,

	// tests all words (split by space) and one of word should be equal to part
	CHECK_EQUALS_FROM_SPACE,

	// simple collator contains in any part of the base
	CHECK_CONTAINS,

	// simple collator equals
	CHECK_EQUALS,
}