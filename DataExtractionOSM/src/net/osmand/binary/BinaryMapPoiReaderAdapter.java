package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.MapUtils;
import net.sf.junidecode.Junidecode;


import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapPoiReaderAdapter {
	
	public static final int SHIFT_BITS_CATEGORY = 7;
	private static final int CATEGORY_MASK = (1 << SHIFT_BITS_CATEGORY) - 1 ;
	
	public static class PoiRegion extends BinaryIndexPart {

		List<String> categories = new ArrayList<String>();
		List<AmenityType> categoriesType = new ArrayList<AmenityType>();
		List<List<String>> subcategories = new ArrayList<List<String>>();
	}
	
	private CodedInputStreamRAF codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryMapPoiReaderAdapter(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	private int readInt() throws IOException {
		return map.readInt();
	}
	

	protected void readPoiIndex(PoiRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndPoiIndex.CATEGORIESTABLE_FIELD_NUMBER :
				int length = codedIS.readRawVarint32();
				
				int oldLimit = codedIS.pushLimit(length);
				readCategory(region);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER :
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readCategory(PoiRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndCategoryTable.CATEGORY_FIELD_NUMBER :
				String cat = codedIS.readString();
				region.categories.add(cat);
				region.categoriesType.add(AmenityType.fromString(cat));
				region.subcategories.add(new ArrayList<String>());
				break;
			case OsmandOdb.OsmAndCategoryTable.SUBCATEGORIES_FIELD_NUMBER :
				region.subcategories.get(region.subcategories.size() - 1).add(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	protected void searchPoiIndex(int left31, int right31, int top31, int bottom31,
			SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		int indexOffset = codedIS.getTotalBytesRead();
		TIntArrayList offsets = new TIntArrayList();
		while(true){
			if(req.isInterrupted()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER :
				int length = codedIS.readFixed32();
				int oldLimit = codedIS.pushLimit(length);
				readBoxField(left31, right31, top31, bottom31, 0, 0, 0, offsets, req);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.POIDATA_FIELD_NUMBER :
				offsets.sort();
				for (int j = 0; j < offsets.size(); j++) {
					codedIS.seek(offsets.get(j) + indexOffset);
					int len = codedIS.readFixed32();
					int oldLim = codedIS.pushLimit(len);
					readPoiData(left31, right31, top31, bottom31, req, region);
					codedIS.popLimit(oldLim);
					if(req.isInterrupted()){
						return;
					}
				}
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readPoiData(int left31, int right31, int top31, int bottom31, 
			SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		int x = 0;
		int y = 0;
		int zoom = 0;
		while(true){
			if(req.isInterrupted()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiBoxData.X_FIELD_NUMBER :
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.ZOOM_FIELD_NUMBER :
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.Y_FIELD_NUMBER :
				y= codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.POIDATA_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				int oldLim = codedIS.pushLimit(len);
				readPoiPoint(left31, right31, top31, bottom31, x, y, zoom, req, region);
				codedIS.popLimit(oldLim);
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readPoiPoint(int left31, int right31, int top31, int bottom31, 
			int px, int py, int zoom, SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		Amenity am = null;
		int x = 0;
		int y = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(Algoritms.isEmpty(am.getEnName())){
					am.setEnName(Junidecode.unidecode(am.getName()));
				}
				req.getSearchResults().add(am);
				return;
			case OsmandOdb.OsmAndPoiBoxDataAtom.DX_FIELD_NUMBER :
				x = (codedIS.readSInt32() + (px << (24 - zoom))) << 7;
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.DY_FIELD_NUMBER :
				y = (codedIS.readSInt32() + (py << (24 - zoom))) << 7;
				if(left31 > x || right31 < x || top31 < y || bottom31 > y){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return;
				}
				am = new Amenity();
				am.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER :
				// TODO add many amenities
				int cat = codedIS.readUInt32();
				int subcatId = cat >> SHIFT_BITS_CATEGORY;
				int catId = cat & CATEGORY_MASK;
				if(catId < region.categoriesType.size()){
					am.setType(region.categoriesType.get(catId));
					List<String> subcats = region.subcategories.get(catId);
					if(subcatId < subcats.size()){
						am.setSubType(subcats.get(catId));
					}
				} else {
					am.setType(AmenityType.OTHER);
				}
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.ID_FIELD_NUMBER :
				am.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NAME_FIELD_NUMBER :
				am.setName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NAMEEN_FIELD_NUMBER :
				am.setEnName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.OPENINGHOURS_FIELD_NUMBER :
				am.setOpeningHours(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.SITE_FIELD_NUMBER :
				am.setSite(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.PHONE_FIELD_NUMBER:
				am.setPhone(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readBoxField(int left31, int right31, int top31, int bottom31,
			int px, int py, int pzoom, TIntArrayList offsets, SearchRequest<Amenity> req) throws IOException {
		if(pzoom > 0){
			int x1 = px << (31 - pzoom);
			int x2 = (px + 1) << (31 - pzoom);
			int y1 = py << (31 - pzoom);
			int y2 = (py + 1) << (31 - pzoom);
			// check intersection
			if(!(left31 <= x2 && x1 <= right31 && bottom31 <= y2 && y1 <= top31)){
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			}
			
		}
		int zoom = pzoom;
		int y = py;
		int x = px;
		while(true){
			if(req.isInterrupted()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiBox.ZOOM_FIELD_NUMBER :
				zoom = codedIS.readUInt32() + pzoom;
				break;
			case OsmandOdb.OsmAndPoiBox.LEFT_FIELD_NUMBER :
				x = codedIS.readSInt32() + px;
				break;
			case OsmandOdb.OsmAndPoiBox.TOP_FIELD_NUMBER:
				y = codedIS.readSInt32() + py;
				break;
				
			case OsmandOdb.OsmAndPoiBox.SUBBOXES_FIELD_NUMBER:
				int length = codedIS.readFixed32();
				int oldLimit = codedIS.pushLimit(length);
				readBoxField(left31, right31, top31, bottom31, x, y, zoom, offsets, req);
				codedIS.popLimit(oldLimit);
				break;
				
			case OsmandOdb.OsmAndPoiBox.SHIFTTODATA_FIELD_NUMBER:
				offsets.add(codedIS.readFixed32());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	

}
