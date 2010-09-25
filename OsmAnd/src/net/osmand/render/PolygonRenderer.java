package net.osmand.render;

import net.osmand.R;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.render.OsmandRenderer.RenderingContext;
import android.graphics.Color;

public class PolygonRenderer {

	public static void renderPolygon(RenderingContext rc, int zoom, int type, int subtype, OsmandRenderer o) {
		if (type == MapRenderingTypes.HIGHWAY) {
			if (subtype == MapRenderingTypes.PL_HW_SERVICE || subtype == MapRenderingTypes.PL_HW_UNCLASSIFIED
				|| subtype == MapRenderingTypes.PL_HW_RESIDENTIAL) {
				rc.second.color = Color.rgb(194, 194, 194);
				rc.second.strokeWidth = 1;
				rc.main.color= Color.WHITE;
			} else if(subtype == MapRenderingTypes.PL_HW_PEDESTRIAN || subtype == MapRenderingTypes.PL_HW_FOOTWAY){
				rc.main.color = Color.rgb(236, 236, 236);
				rc.second.color = Color.rgb(176, 176, 176);
				rc.second.strokeWidth = 1;
			}
		} else if (type == MapRenderingTypes.RAILWAY) {
			if(subtype == 13){
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xffd4aaaa;
			}
		} else if (type == MapRenderingTypes.WATERWAY) {
			if(subtype == 3){
				rc.main.fillArea = zoom >= 7;
				rc.main.color = 0xffb5d0d0;
			} else if(subtype == 4 || subtype == 7 || subtype == 13){
				rc.main.fillArea = zoom >= 10;
				rc.main.color = 0xffb5d0d0;
			}
		} else if (type == MapRenderingTypes.AEROWAY) {
			if(subtype == 1 || subtype == 10){
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0x80cccccc;
			} else if(subtype == 2){
				rc.main.fillArea = zoom >= 15;
				rc.main.color = 0xffcc99ff;
			} else if(subtype == 9){
				// apron
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xffe9d1ff;
			}
		} else if (type == MapRenderingTypes.POWER) {
			if(subtype == 5 || subtype == 6){
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xffbbbbbb;
			}
		} else if (type == MapRenderingTypes.MAN_MADE) {
			rc.main.fillArea = zoom > 15;
			if (subtype == MapRenderingTypes.SUBTYPE_BUILDING) {
				rc.main.color = Color.rgb(188, 169, 169);
			} else if (subtype == MapRenderingTypes.SUBTYPE_GARAGES) {
				rc.main.color = Color.rgb(221, 221, 221);
			}
		} else  if (type == MapRenderingTypes.TOURISM) {
			if (subtype == 2 || subtype == 7) {
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xfff2caea;
				if(subtype == 7){
					rc.second.color = 0xff734a08;
					rc.second.pathEffect = o.getDashEffect("9_3"); //$NON-NLS-1$
					if(zoom <= 15){
						rc.second.strokeWidth = 1;
					} else {
						rc.second.strokeWidth = 2;
					}
				}
			} else if(subtype >= 4 && subtype <= 6){
				rc.main.fillArea = zoom >= 15;
				rc.main.color = 0xa0ccff99;
			} else if(subtype == 8){
				rc.main.fillArea = zoom >= 13;
				rc.main.shader = o.getShader(R.drawable.h_zoo);
			}
			
		} else if (type == MapRenderingTypes.NATURAL) {
			switch(subtype){
			case 2 :
				rc.main.fillArea = zoom >= 13;
				rc.main.shader = o.getShader(R.drawable.h_beach);
			case 5:
				rc.main.fillArea = zoom >= 6;
				rc.main.color = 0xffb5d0d0;
				break;
			case 7:
				rc.main.fillArea = zoom >= 8;
				rc.main.shader = o.getShader(R.drawable.h_glacier);
				if(zoom >= 10){
					rc.second.color = 0xff99ccff;
					rc.second.strokeWidth = 2;
				}
				break;
			case 8:
				rc.main.fillArea = zoom >= 10;
				rc.main.color = 0xffffffc0;
				break;
			case 9:
				rc.main.fillArea = zoom >= 11;
				rc.main.color = 0xfff2efe9;
				break;
			case 11:
			case 22:
				rc.main.fillArea = zoom >= 13;
				rc.main.shader = o.getShader(R.drawable.h_marsh);
				break;
			case 12 :
				rc.main.fillArea = zoom >= 13;
				rc.main.shader = o.getShader(R.drawable.h_mud);
				break;
			case 16 :
				rc.main.fillArea = zoom >= 13;
				rc.main.shader = o.getShader(R.drawable.h_scrub);
				break;
			case 21:
				rc.main.fillArea = zoom >= 7;
				rc.main.color = 0xffb5d0d0;
				break;
			case 23 :
				rc.main.fillArea = zoom >= 8;
				rc.main.color = 0xffaed1a0;
				break;
			}
		} else if (type == MapRenderingTypes.LANDUSE) {
			switch (subtype) {
			case 1:
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xffc8b084;
				break;
			case 2:
				rc.main.fillArea = zoom >= 10;
				rc.main.color = 0xffb5d0d0;
				break;
			case 4:
				rc.main.fillArea = zoom >= 12;
				if(zoom >= 12 && zoom <= 14){
					rc.main.color = 0xffaacbaf;
				} else if (zoom > 14) {
					rc.main.shader = o.getShader(R.drawable.h_grave_yard);
				}
				break;
			case 5:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xffefc8c8;
				break;
			case 3: 
			case 6:
			case 13:
			case 16:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xff9d9d6c;
				break;
			case 7:
				rc.main.fillArea = zoom >= 11;
				rc.main.color = 0xffead8bd;
				break;
			case 9:
				rc.main.fillArea = zoom >= 11;
				rc.main.color = 0xffddbf92;
				break;
			case 10:
				rc.main.fillArea = zoom >= 8;
				if(zoom <= 13){
					rc.main.color = 0xff8dc56c;
				} else {
					rc.main.shader = o.getShader(R.drawable.h_forest);
				}
				break;
			case 11 :
				rc.main.color = Color.rgb(223, 209, 214);
				break;
			case 12:
			case 17:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xffcfeca8;
				break;
			case 15:
			case 20:
				rc.main.color = 0xffdfd1d6;
				rc.main.fillArea = zoom >= 12;
				break;
			case 18:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xa0ffa8a8;
				break;
			case 19:
				rc.main.fillArea = zoom >= 10;
				rc.main.shader = o.getShader(R.drawable.h_orchard);
				break;
			case 21:
				rc.main.color = 0xffcfeca8;
				rc.main.fillArea = zoom >= 12;
				break;
			case 22 :
				rc.main.fillArea = zoom >= 7;
				rc.main.color = 0xffb5d0d0;
				break;
			case 23:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xffdddddd;
				break;
			case 24:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xffcfeca8;
				if(zoom >= 15){
					rc.second.color = Color.RED;
					rc.second.strokeWidth = 1;
				}
				break;
			case 26:
				rc.main.fillArea = zoom >= 13;
				rc.main.shader = o.getShader(R.drawable.h_quarry2);
				break;
			case 27:
				rc.main.fillArea = zoom >= 10;
				if(zoom < 14){
					rc.main.color = 0xffabdf96;
				} else {
					rc.main.shader = o.getShader(R.drawable.h_vineyard);
				}
				break;
			}
		} else if (type == MapRenderingTypes.MILITARY) {
			if(subtype == 3){
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xffff8f8f;
			} else if(subtype == 4){
				rc.main.fillArea = zoom >= 10;
				rc.main.shader = o.getShader(R.drawable.h_danger);
			}
		} else if (type == MapRenderingTypes.LEISURE) {
			switch (subtype) {
			case 2:
			case 4:
				rc.main.fillArea = zoom >= 13;
				rc.main.color = 0xff33cc99;
				break;
			case 3:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xffb5e3b5;
				break;
			case 5:
				rc.main.fillArea = zoom >= 12;
				rc.second.color = 0xff888888;
				rc.second.strokeWidth = 1;
				rc.main.color = 0xff74dcba;
				break;
			case 6:
				rc.main.color = 0xff8ad3af;
				rc.main.fillArea = zoom >= 13;
				rc.second.color = 0xff888888;
				rc.second.strokeWidth = 1;
				break;
			case 11:
				if(zoom < 8){
					rc.main.fillArea = false;
				} else if(zoom >= 8 && zoom <= 12){
					rc.main.fillArea = true;
					rc.main.color = 0xffabdf96;
				} else {
					rc.main.fillArea = true;
					rc.main.shader = o.getShader(R.drawable.h_nr);
				}
				break;
			case 12:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xb0b6fdb6;
				break;
			case 13:
				rc.main.color = 0xffccfff1;
				rc.main.fillArea = zoom >= 15;
				break;
			case 14:
			case 15:
				rc.main.fillArea = zoom >= 12;
				rc.main.color = 0xffcfeca8;
				break;
			}
		} else if (type == MapRenderingTypes.AMENITY_HEALTHCARE) {
			if (subtype == 2) {
				rc.main.fillArea = zoom >= 15;
				rc.main.color = 0xfff0f0d8;
				rc.second.color = Color.rgb(212, 168, 158);
				rc.second.strokeWidth = 1;
			}
		} else if (type == MapRenderingTypes.AMENITY_TRANSPORTATION) {
			if (subtype == 1 || subtype == 2) {
				rc.main.color = Color.rgb(246, 238, 183);
			}
		} else if (type == MapRenderingTypes.AMENITY_ENTERTAINMENT) {
			if (subtype == 3) {
				rc.main.color = Color.rgb(204, 153, 153);
			}
		} else if (type == MapRenderingTypes.AMENITY_EDUCATION) {
			if(subtype == 1 || subtype == 2 || subtype == 3 || subtype == 5){
				rc.main.fillArea = zoom >= 15;
				rc.main.color = 0xfff0f0d8;
				rc.second.color = Color.rgb(212, 168, 158);
				rc.second.strokeWidth = 1;
			} else {
				// draw as building education
				rc.main.color = Color.rgb(188, 169, 169);
			}
		}
	}
}
