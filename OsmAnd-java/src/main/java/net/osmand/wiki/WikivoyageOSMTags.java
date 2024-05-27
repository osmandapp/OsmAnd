package net.osmand.wiki;

public enum WikivoyageOSMTags {
    TAG_WIKIDATA("wikidata"),
    TAG_WIKIPEDIA("wikipedia"),
    TAG_OPENING_HOURS("opening_hours"),
    TAG_ADDRESS("address"),
    TAG_EMAIL("email"),
    TAG_FAX("fax"),
    TAG_DIRECTIONS("directions"),
    TAG_PRICE("price"),
    TAG_PHONE("phone");

    private final String tg;

    private WikivoyageOSMTags(String tg) {
        this.tg = tg;
    }

    public String tag() {
        return tg;
    }

    public static boolean contains(String string) {
        for (WikivoyageOSMTags tag : values()) {
            if (tag.tg.equals(string)) {
                return true;
            }
        }
        return false;
    }
}
