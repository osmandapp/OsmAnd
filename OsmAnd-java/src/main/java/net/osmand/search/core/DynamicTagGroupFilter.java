package net.osmand.search.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DynamicTagGroupFilter {

    public DynamicTagGroupFilter(int tagGroupId) {
        addTagGroup(tagGroupId);
    }

    private HashSet<Integer> tagGroups = null;

    public boolean isAccept(Set<Integer> tagGroupIds) {
        if (tagGroups == null) {
            return true;
        }
        for (int id : tagGroupIds) {
            if (tagGroups.contains(id)) {
                return true;
            }
        }
        return false;
    }
    public boolean isAccept(int tagGroupId) {
        if (tagGroups == null) {
             return true;
        }
        return tagGroups.contains(tagGroupId);
    }

    public void addTagGroup(int tagGroupId) {
        if (tagGroups == null) {
            tagGroups = new HashSet<>();
        }
        tagGroups.add(tagGroupId);
    }
}
