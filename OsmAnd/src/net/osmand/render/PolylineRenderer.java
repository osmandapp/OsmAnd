package net.osmand.render;

import net.osmand.osm.MapRenderingTypes;
import net.osmand.render.OsmandRenderer.RenderingContext;
import android.graphics.Color;
import android.graphics.PathEffect;

public class PolylineRenderer {
	
	public static void renderPolyline(int type, int subtype, int objType, RenderingContext rc, OsmandRenderer o){
		int zoom = rc.zoom;
		
		int color = Color.BLACK;
		boolean showText = true;
		PathEffect pathEffect = null;
		int shadowLayer = 0;
		int shadowColor = 0;
		float strokeWidth = zoom >= 15 ? 1 : 0;
		
		float secondStrokeWidth = 0;
		int secondColor = 0;
		PathEffect secondEffect = null;
		
		switch (type) {
		case MapRenderingTypes.HIGHWAY: {
			int hwType = subtype;
			boolean carRoad = true;
			int layer = MapRenderingTypes.getWayLayer(objType);
			boolean tunnel = layer == 1;
			boolean bridge = layer == 2;
			if (hwType == MapRenderingTypes.PL_HW_TRUNK || hwType == MapRenderingTypes.PL_HW_MOTORWAY) {
				if (hwType == MapRenderingTypes.PL_HW_TRUNK) {
					color = Color.rgb(168, 218, 168);
				} else {
					color = Color.rgb(128, 155, 192);
				}
				if(zoom < 10){
					if (zoom >= 7) {
						strokeWidth = 3.5f;
					} else if (zoom == 6) {
						strokeWidth = 2;
					} else if (zoom == 5) {
						strokeWidth = 1;
					} else {
						strokeWidth = 0;
					}
				} else if (zoom <= 12) {
					strokeWidth = 3f;
				} else if (zoom <= 14) {
					strokeWidth = 7f;
				}
			} else if (hwType == MapRenderingTypes.PL_HW_PRIMARY) {
				color = Color.rgb(235, 152, 154);
				if (zoom < 7) {
					strokeWidth = 0;
				} else if (zoom == 7) {
					strokeWidth = 1.5f;
				} else if (zoom == 8 || zoom == 9) {
					strokeWidth = 2f;
				} else if (zoom <= 12) {
					strokeWidth = 3f;
				} else if (zoom <= 14) {
					strokeWidth = 7f;
				}
			} else if (hwType == MapRenderingTypes.PL_HW_SECONDARY) {
				color = Color.rgb(253, 214, 164);
				if(zoom < 8){
					strokeWidth = 0;
				} else if(zoom <= 10){
					strokeWidth = 1;
				} else if(zoom <= 12){
					strokeWidth = 2;
				} else if(zoom <= 14){
					strokeWidth = 6;
				}
			} else if (hwType == MapRenderingTypes.PL_HW_TERTIARY) {
				color = Color.rgb(254, 254, 179);
				shadowLayer = 2;
				shadowColor = Color.rgb(186, 186, 186);
				if(zoom < 13){
					strokeWidth = 0;
				} else if(zoom < 14){
					strokeWidth = 4;
					shadowLayer = 1;
				} else if(zoom < 15){
					strokeWidth = 6;
					shadowLayer = 1;
				}
			} else if (hwType == MapRenderingTypes.PL_HW_RESIDENTIAL || hwType == MapRenderingTypes.PL_HW_UNCLASSIFIED){
				if(zoom < 14){
					strokeWidth = 0;
				} else if(zoom < 15){
					strokeWidth = 4;
				}
				shadowLayer = 1;
				shadowColor = Color.rgb(194, 194, 194);
				color = Color.WHITE;
			} else if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_LIVING_STREET) {
				shadowLayer = 1;
				shadowColor = Color.rgb(194, 194, 194);
				if(zoom < 15){
					strokeWidth = 0;
				}
				color = Color.WHITE;
			} else if (hwType == MapRenderingTypes.PL_HW_PEDESTRIAN) {
				shadowLayer = 1;
				shadowColor = Color.rgb(176, 176, 176);
				color = Color.rgb(236, 236, 236);
			} else {
				carRoad = false;
				 if (hwType == MapRenderingTypes.PL_HW_CONSTRUCTION || hwType == MapRenderingTypes.PL_HW_PROPOSED) {
					strokeWidth = zoom >= 15 ? (zoom == 15 ? 6 : 8) : 0;
					color = 0xff99cccc;
					secondColor = Color.WHITE;
					secondStrokeWidth = strokeWidth - 1;
					secondEffect = o.getDashEffect("8_6"); //$NON-NLS-1$
				} else {
					if (hwType == MapRenderingTypes.PL_HW_TRACK) {
						strokeWidth = zoom >= 14 ? 2f : 0;
						color = 0xff996600;
						pathEffect = o.getDashEffect("4_3"); //$NON-NLS-1$
					} else if (hwType == MapRenderingTypes.PL_HW_PATH) {
						strokeWidth = zoom >= 14 ? 1f : 0;
						color = Color.BLACK;
						pathEffect = o.getDashEffect("6_3"); //$NON-NLS-1$
					} else if (hwType == MapRenderingTypes.PL_HW_CYCLEWAY) {
						strokeWidth = zoom >= 14 ? 2f : 0;
						pathEffect = o.getDashEffect("2_2"); //$NON-NLS-1$
						color = Color.BLUE;
					} else if (hwType == MapRenderingTypes.PL_HW_BRIDLEWAY) {
						strokeWidth = zoom >= 14 ? 2f : 0;
						pathEffect = o.getDashEffect("2_2"); //$NON-NLS-1$
						color = Color.GREEN;
					} else if (hwType == MapRenderingTypes.PL_HW_BYWAY) {
						strokeWidth = zoom >= 14 ? 2f : 0;
						pathEffect = o.getDashEffect("4_3"); //$NON-NLS-1$
						color = 0xffffcc00;
					} else if (hwType == MapRenderingTypes.PL_HW_STEPS) {
						color = Color.rgb(250, 128, 115);
						strokeWidth = zoom >= 15 ? 5 : 0;
						pathEffect = o.getDashEffect("1_2"); //$NON-NLS-1$
					} else if (hwType == MapRenderingTypes.PL_HW_FOOTWAY) {
						color = Color.rgb(250, 128, 115);
						strokeWidth = zoom >= 15 ? 2 : 0;
						pathEffect = o.getDashEffect("2_2"); //$NON-NLS-1$
					}
				}
			}
			showText = (carRoad && zoom > 12) || zoom > 16;
			
			if (carRoad) {
				if (zoom >= 15) {
					if (zoom < 16) {
						strokeWidth = 9;
					} else if (zoom == 16) {
						strokeWidth = 11;
					} else if (zoom == 17) {
						strokeWidth = 13;
					} else if (zoom >= 18) {
						strokeWidth = 16;
					} else if (zoom >= 19) {
						strokeWidth = 20;
					}
					if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_LIVING_STREET) {
						strokeWidth -= 3;
					}
				}
			}
			if(bridge && zoom > 12){
				if(secondStrokeWidth == 0){
					shadowLayer = 2;
					shadowColor = Color.BLACK;
				}
			}
			if (tunnel && zoom > 12 && carRoad) {
				pathEffect = o.getDashEffect("4_4"); //$NON-NLS-1$
			}
		}
			break;
		case MapRenderingTypes.RAILWAY: {
			int layer = MapRenderingTypes.getWayLayer(objType);
			boolean tunnel = layer == 1;
			boolean bridge = layer == 2;
			if (subtype == 1) {
				color = 0xffaaaaaa;
				if (zoom < 7) {
					strokeWidth = 0;
				} else if (zoom == 7) {
					strokeWidth = 1;
				} else if (zoom == 8) {
					strokeWidth = 1.5f;
				} else if (zoom <= 12) {
					strokeWidth = 2;
					if(tunnel){
						pathEffect = o.getDashEffect("5_2"); //$NON-NLS-1$
					}
				} else if(zoom == 13){
					color = 0xff999999;
					strokeWidth = 3;
					secondColor = Color.WHITE;
					secondStrokeWidth = 1;
					secondEffect = o.getDashEffect("8_12"); //$NON-NLS-1$
				} else {
					color = 0xff999999;
					strokeWidth = 3;
					secondColor = Color.WHITE;
					secondStrokeWidth = 1;
					if(tunnel){
						// TODO tunnel
					} else if(bridge){
						// TODO bridge 
						secondStrokeWidth = 5;
						strokeWidth = 7;
					} else {
						secondEffect = o.getDashEffect("0_11_8_1"); //$NON-NLS-1$
					}
					
				}
			} else if(subtype == 2 ) {
				color = 0xff44444;
				if(zoom < 13){
					strokeWidth = 0;
				} else if(zoom < 15){
					strokeWidth = 1;
					if(tunnel){
						pathEffect = o.getDashEffect("5_3"); //$NON-NLS-1$
					}
				} else {
					strokeWidth = 2;
					if(tunnel){
						pathEffect = o.getDashEffect("5_3"); //$NON-NLS-1$
					}
				}
			} else if(subtype == 3){
				color = 0xff666666;
				if(zoom < 13){
					strokeWidth = 0;
				} else {
					strokeWidth = 2;
					if(tunnel){
						pathEffect = o.getDashEffect("5_3"); //$NON-NLS-1$
					}
				}
			} else if(subtype == 4 || subtype == 5 || subtype == 9){
				if(zoom < 13){
					strokeWidth = 0;
				} else {
					if(bridge){
						strokeWidth = 4.5f;
						color = Color.BLACK;
						secondStrokeWidth = 2;
						secondColor = Color.GRAY;
						secondEffect =o.getDashEffect("4_2"); //$NON-NLS-1$
					} else {
						strokeWidth = 2;
						color = Color.GRAY;
						pathEffect = o.getDashEffect("4_2"); //$NON-NLS-1$
					}
				}
			} else if (subtype == 6) {
				color = 0xff999999;
				if(zoom < 13){
					strokeWidth = 0;
				} else {
					strokeWidth = 2;
					if(tunnel){
						pathEffect = o.getDashEffect("5_3"); //$NON-NLS-1$
					}
				}
			} else if (subtype == 7) {
				if(zoom < 13){
					strokeWidth = 0;
				} else {
					color = 0xff999999;
					strokeWidth = 3;
					secondColor = Color.WHITE;
					secondStrokeWidth = 1;
					secondEffect = o.getDashEffect("0_1_8_1"); //$NON-NLS-1$
				}
			} else if (subtype == 8 || subtype == 11) {
				if(zoom < 15){
					strokeWidth = 0;
				} else {
					strokeWidth = 2;
					color = 0xff666666;
					if(tunnel){
						strokeWidth = 5;
						pathEffect = o.getDashEffect("5_3"); //$NON-NLS-1$
						secondColor = 0xffcccccc;
						secondStrokeWidth = 3;
					}
				}
			} else if (subtype == 10) {
				if(zoom < 15){
					strokeWidth = 0;
				} else {
					strokeWidth = 3;
					color = 0xff777777;
					pathEffect = o.getDashEffect("2_3"); //$NON-NLS-1$
				}
			} else if (subtype == 12) {
				if(zoom < 15){
					strokeWidth = 0;
				} else {
					strokeWidth = 3;
					color = Color.GRAY;
				}
			}
		}
			break;
		case MapRenderingTypes.WATERWAY: {
			if (zoom <= 10) {
				strokeWidth = 0;
				// draw rivers & canals
				if (subtype == 2 || subtype == 4) {
					color = 0xffb5d0d0;
					if (zoom == 10) {
						strokeWidth = 2;
					} else if (zoom == 9) {
						strokeWidth = 1;
					}
				}
			} else {
				switch (subtype) {
				case 1:
					if (zoom >= 15) {
						color = 0xffb5d0d0;
						strokeWidth = 2;
					} else {
						strokeWidth = 0;
					}
					break;
				case 2:
				case 4:
					color = 0xffb5d0d0;

					if (zoom < 13) {
						strokeWidth = 2;
					} else {
						if (zoom == 13) {
							strokeWidth = 3;
						} else if (zoom == 14) {
							strokeWidth = 5;
						} else if (zoom == 15 || zoom == 16) {
							strokeWidth = 6;
						} else if (zoom == 17) {
							strokeWidth = 10;
						} else if (zoom == 18) {
							strokeWidth = 12;
						}
					}
					break;
				case 5:
				case 6:
					color = 0xffb5d0d0;
					if (zoom < 13) {
						strokeWidth = 0;
					} else if (zoom < 15) {
						strokeWidth = 1;
					} else {
						strokeWidth = 2;
					}
					break;

				case 11:
					if (zoom < 15) {
						strokeWidth = 0;
					} else {
						strokeWidth = 2;
						color = 0xffaaaaaa;
					}
					break;
				case 12:
					if (zoom >= 13) {
						strokeWidth = 2;
					} else {
						strokeWidth = 0;
					}
					color = 0xffb5d0d0;
				default:
					break;
				}
			}
			if(zoom > 12 && MapRenderingTypes.getWayLayer(objType) == 1){
				pathEffect = o.getDashEffect("4_2"); //$NON-NLS-1$
				secondStrokeWidth = strokeWidth - 2;
				secondColor = Color.WHITE;
			}
		}
			break;
		case MapRenderingTypes.BARRIER: {
			if (subtype == 5) {
				color = Color.GRAY;
				if (zoom == 14) {
					strokeWidth = 4;
				} else if (zoom == 15) {
					strokeWidth = 6;
				} else if (zoom > 15) {
					strokeWidth = 9;
				} else {
					strokeWidth = 0;
				}
			} else {
				if (subtype == 1) {
					strokeWidth = zoom >= 16 ? 3 : 0;
					color = 0xffaed1a0;
				} else {
					strokeWidth = zoom >= 16 ? 1 : 0;
					color = Color.BLACK;
				}
			}
		}
			break;
		case MapRenderingTypes.POWER: {
			if (zoom >= 14) {
				if (subtype == 3) {
					color = Color.rgb(186, 186, 186);
					strokeWidth = zoom == 14 ? 1 : 2;
				} else if (subtype == 4) {
					color = Color.rgb(186, 186, 186);
					strokeWidth = 1;
				}
			} else {
				strokeWidth = 0;
			}
		}
			break;
		case MapRenderingTypes.AEROWAY: {
			if(subtype == 7){
				color = 0xffbbbbcc;
				if(zoom < 11){
					strokeWidth = 0;
				} else if(zoom < 12){
					strokeWidth = 2;
				} else if(zoom < 13){
					strokeWidth = 4;
				} else if(zoom < 14){
					strokeWidth = 7;
				} else if(zoom < 15){
					strokeWidth = 12;
				} else {
					strokeWidth = 18;
				}
			} else if(subtype == 8){
				color = 0xffbbbbcc;
				if(zoom < 12){
					strokeWidth = 0;
				} else if(zoom < 14){
					strokeWidth = 1;
				} else if(zoom < 15){
					strokeWidth = 4;
				} else {
					strokeWidth = 6;
				}
			}
			if(MapRenderingTypes.getWayLayer(objType) == 2 && zoom > 12){
				if(secondStrokeWidth == 0){
					shadowLayer = 2;
					shadowColor = Color.BLACK;
				}
			}
		}
			break;
		case MapRenderingTypes.AERIALWAY: {
			// TODO shader on path doesn't work
			if (zoom >= 12) {
				if (subtype == 1 || subtype == 2) {
					color = Color.rgb(186, 186, 186);
					strokeWidth = 2;
					// paint.setShader(getShader(R.drawable.h_cable_car));
				} else if (subtype == 3 || subtype == 4 || subtype == 5) {
					color = Color.rgb(186, 186, 186);
					strokeWidth = 2;
					// paint.setShader(getShader(R.drawable.h_chair_lift));
				}
			}
		}
			break;
		case MapRenderingTypes.MAN_MADE: {
			if (subtype == 8) {
				// breakwater, groyone
				color = 0xffaaaaaa;
				if (zoom < 12) {
					strokeWidth = 0;
				} else if (zoom < 14) {
					strokeWidth = 1;
				} else if (zoom < 16) {
					strokeWidth = 2;
				} else {
					strokeWidth = 4;
				}
			} else if (subtype == 9) {
				// pier
				color = 0xfff2efe9;
				if (zoom < 12) {
					strokeWidth = 0;
				} else if (zoom < 14) {
					strokeWidth = 1;
				} else if (zoom < 16) {
					strokeWidth = 3;
				} else {
					strokeWidth = 6;
				}
			}
		}
			break;
		case MapRenderingTypes.LEISURE: {
			if (subtype == 8) {
				if (zoom < 13) {
					strokeWidth = 0;
				} else if (zoom < 16) {
					color = Color.BLUE;
					strokeWidth = 1;
					pathEffect = o.getDashEffect("6_2"); //$NON-NLS-1$
				} else {
					color = Color.BLUE;
					strokeWidth = 2;
					pathEffect = o.getDashEffect("6_2"); //$NON-NLS-1$
				}
			} else if (subtype == 5) {
				if (zoom >= 13) {
					color = 0xff888888;
					strokeWidth = 1;
				} else {
					strokeWidth = 0;
				}
			}
		}
			break;
		case MapRenderingTypes.ADMINISTRATIVE: {
			color = 0xFF800080;
			if (subtype == 29 || subtype == 30) {
				// admin level 9, 10
				if (zoom > 12) {
					pathEffect = o.getDashEffect("3_2"); //$NON-NLS-1$
					strokeWidth = 1;
					if (zoom > 16) {
						strokeWidth = 2;
					}
				} else {
					strokeWidth = 0;
				}
			} else if (subtype == 28 || subtype == 27) {
				// admin level 7, 8
				if (zoom > 11) {
					pathEffect = o.getDashEffect("5_2"); //$NON-NLS-1$
					strokeWidth = 2;
				} else {
					strokeWidth = 0;
				}
			} else if (subtype == 25 || subtype == 26) {
				// admin level 5, 6
				if (zoom > 10) {
					pathEffect = subtype == 25 ? o.getDashEffect("6_3_2_3_2_3") : o.getDashEffect("6_3_2_3"); //$NON-NLS-1$ //$NON-NLS-2$
					strokeWidth = 2;
				} else {
					strokeWidth = 0;
				}
			} else if (subtype == 24) {
				// admin level 4
				pathEffect = o.getDashEffect("4_3"); //$NON-NLS-1$
				if (zoom >= 4 && zoom <= 6) {
					strokeWidth = 0.6f;
				} else if (zoom >= 7 && zoom <= 10) {
					strokeWidth = 2;
				} else if (zoom > 10) {
					strokeWidth = 3;
				} else {
					strokeWidth = 0;
				}
			} else if (subtype == 23 || subtype == 22) {
				// admin level 2, 3
				if (zoom >= 4 && zoom <= 6) {
					strokeWidth = 2;
				} else if (zoom >= 7 && zoom <= 9) {
					strokeWidth = 3;
				} else if (zoom > 9) {
					if (subtype == 22) {
						strokeWidth = 6;
					} else {
						strokeWidth = 5;
						pathEffect = o.getDashEffect("4_2"); //$NON-NLS-1$
					}
				} else {
					strokeWidth = 0;
				}

			}
		}
			break;
		default:
			break;
		}
		
		rc.color = color;
		rc.pathEffect = pathEffect;
		rc.shadowColor = shadowColor;
		rc.shadowLayer = shadowLayer;
		rc.showText = showText;
		rc.strokeWidth = strokeWidth;
		rc.secondColor = secondColor;
		rc.secondEffect = secondEffect;
		rc.secondStrokeWidth = secondStrokeWidth;
	}
}
