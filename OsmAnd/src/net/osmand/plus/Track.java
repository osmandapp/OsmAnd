package net.osmand.plus;

import java.util.ArrayList;
import java.util.List;

public class Track extends GPXExtensions {
    public String name = null;
    public String desc = null;
    public List<TrkSegment> segments = new ArrayList<TrkSegment>();
}
