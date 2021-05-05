package net.osmand.binary;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;

import net.osmand.PlatformUtil;
import net.osmand.data.GeometryTile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class BinaryVectorTileReader {

	public static GeometryTile readTile(File file) throws IOException {
		GeometryFactory geomFactory = new GeometryFactory();
		return new GeometryTile(
				MvtReader.loadMvt(PlatformUtil.getFileInputStream(file), geomFactory, new TagKeyValueMapConverter()));
	}
}
