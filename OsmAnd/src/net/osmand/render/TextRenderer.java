package net.osmand.render;

import net.osmand.R;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.render.OsmandRenderer.RenderingContext;
import android.graphics.Color;

public class TextRenderer {

	private static int[] trunkShields = new int[]{R.drawable.tru_shield1, R.drawable.tru_shield2, R.drawable.tru_shield3,
		R.drawable.tru_shield4, R.drawable.tru_shield5, R.drawable.tru_shield6, R.drawable.tru_shield7,};
	private static int[] motorShields = new int[]{R.drawable.mot_shield1, R.drawable.mot_shield2, R.drawable.mot_shield3,
		R.drawable.mot_shield4, R.drawable.mot_shield5, R.drawable.mot_shield6, R.drawable.mot_shield7,};
	private static int[] primaryShields = new int[]{R.drawable.pri_shield1, R.drawable.pri_shield2, R.drawable.pri_shield3,
		R.drawable.pri_shield4, R.drawable.pri_shield5, R.drawable.pri_shield6, R.drawable.pri_shield7,};
	private static int[] secondaryShields = new int[]{R.drawable.sec_shield1, R.drawable.sec_shield2, R.drawable.sec_shield3,
		R.drawable.sec_shield4, R.drawable.sec_shield5, R.drawable.sec_shield6, R.drawable.sec_shield7,};
	private static int[] tertiaryShields = new int[]{R.drawable.ter_shield1, R.drawable.ter_shield2, R.drawable.ter_shield3,
		R.drawable.ter_shield4, R.drawable.ter_shield5, R.drawable.ter_shield6, R.drawable.ter_shield7,};

	public static String renderObjectText(String name, int subType, int type, int zoom, boolean point, RenderingContext rc) {
		if(name == null || name.length() == 0){
			return null;
		}
		int textSize = 0;
		int textColor = 0;
		int wrapWidth = 0;
		int shadowRadius = 0;
		int textMinDistance = 0;
		int textShield = 0;
		int dy = 0;
		boolean bold = false;
		boolean showTextOnPath = false;
		
		switch (type) {
		case MapRenderingTypes.HIGHWAY : {
			if(name.charAt(0) == MapRenderingTypes.REF_CHAR){
				name = name.substring(1);
				for(int k = 0; k < name.length(); k++){
					if(name.charAt(k) == MapRenderingTypes.REF_CHAR){
						if(k < name.length() - 1 && zoom > 14){
							rc.showAnotherText = name.substring(k + 1);
						}
						name = name.substring(0, k);
						break;
					}
				}
				if(rc.showAnotherText != null && zoom >= 16){
					break;
				}
				if(name.length() > 6){
					name = name.substring(0, 6);
				}
				int len = name.length();
				if(len == 0){
					// skip it
				} else {
					textSize = 10;
					textColor = Color.WHITE;
					bold = true;
					textMinDistance = 70;
					// spacing = 750
					if (subType == MapRenderingTypes.PL_HW_TRUNK) {
						textShield = trunkShields[len - 1];
						if(zoom < 10){
							textSize = 0;
						}
					} else if (subType == MapRenderingTypes.PL_HW_MOTORWAY) {
						textShield = motorShields[len - 1];
						if(zoom < 10){
							textSize = 0;
						}
					} else if (subType == MapRenderingTypes.PL_HW_PRIMARY) {
						textShield = primaryShields[len - 1];
						if(zoom < 11){
							textSize = 0;
						}
					} else if (subType == MapRenderingTypes.PL_HW_SECONDARY) {
						if(zoom < 14){
							textSize = 0;
						}
						textShield = secondaryShields[len - 1];
					} else if (subType == MapRenderingTypes.PL_HW_TERTIARY) {
						if(zoom < 15){
							textSize = 0;
						}
						textShield = tertiaryShields[len - 1];
					} else {
						if(zoom < 16){
							textSize = 0;
						} else {
							showTextOnPath = true;
							textColor = Color.BLACK;
							textSize = 10;
							textMinDistance = 40;
							shadowRadius = 1;
							// spacing = 750;
						}
					}
				}
			} else {
				if(subType == MapRenderingTypes.PL_HW_TRUNK || subType == MapRenderingTypes.PL_HW_PRIMARY 
						|| subType == MapRenderingTypes.PL_HW_SECONDARY){
					textColor = Color.BLACK;
					showTextOnPath = true;
					if(zoom == 13 && type != MapRenderingTypes.PL_HW_SECONDARY){
						textSize = 8;
					} else if(zoom == 14){
						textSize = 9;
					} else if(zoom > 14 && zoom < 17){
						textSize = 10;
					} else if(zoom > 16){
						textSize = 12;
					}
				} else if(subType == MapRenderingTypes.PL_HW_TERTIARY || subType == MapRenderingTypes.PL_HW_RESIDENTIAL
						|| subType == MapRenderingTypes.PL_HW_UNCLASSIFIED || subType == MapRenderingTypes.PL_HW_SERVICE){
					textColor = Color.BLACK;
					showTextOnPath = true;
					if(zoom < 15){
						textSize = 0;
					} else if(zoom < 17){
						textSize = 9;
					} else {
						textSize = 11;
					}
				} else if(subType < 32){
					// highway subtype
					if(zoom >= 16){
						textColor = Color.BLACK;
						showTextOnPath = true;
						textSize = 9;
					}
				} else if(subType == 40){
					// bus stop
					if(zoom >= 17){
						textMinDistance = 20;
						textColor = Color.BLACK;
						textSize = 9;
						wrapWidth = 25;
						dy = 11;
					}
				}
			}
		} break;
		case MapRenderingTypes.WATERWAY : {
			if (subType == 1) {
				if (zoom >= 15 /* && !tunnel */) {
					showTextOnPath = true;
					textSize = 8;
					shadowRadius = 1;
					textColor = 0xff6699cc;
				}
			} else if (subType == 2 || subType == 4) {
				if (zoom >= 12 /* && !tunnel */) {
					textSize = 9;
					showTextOnPath = true;
					shadowRadius = 1;
					textColor = 0xff6699cc;
					textMinDistance = 70;
				}
			} else if (subType == 5 || subType == 6) {
				if (zoom >= 15 /* && !tunnel */) {
					textSize = 8;
					showTextOnPath = true;
					shadowRadius = 1;
					textColor = 0xff6699cc;
				}
			} else if (subType == 12) {
				if(zoom >= 15){
					textColor = Color.BLACK;
					textSize = 8;
					shadowRadius = 1;
				}
			} else if (subType == 8) {
				if (zoom >= 15) {
					shadowRadius = 1;
					textSize = 9;
					textColor = 0xff0066ff;
					wrapWidth = 70;
					dy = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.AEROWAY: {
			textColor = 0xff6692da;
			shadowRadius = 1;
			if(name.charAt(0) == MapRenderingTypes.REF_CHAR){
				name = name.substring(1);
			}
			if (subType == 7 || subType == 8) {
				if (zoom >= 15) {
					showTextOnPath = true;
					textSize = 10;
					textColor = 0xff333333;
					textMinDistance = 50;
					shadowRadius = 1;
					// spacing = 750;
				}
			} else if (subType == 10) {
				// airport
				if (zoom >= 10 && zoom <= 12) {
					textSize = 9;
					dy = -12;
					bold = true;

				}
			} else if (subType == 1) {
				// aerodrome
				if (zoom >= 10 && zoom <= 12) {
					textSize = 8;
					dy = -12;
				}
			} else if (subType == 12) {
				if (zoom >= 17) {
					textSize = 10;
					textColor = 0xffaa66cc;
					shadowRadius = 1;
					wrapWidth = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.AERIALWAY: {
			if (subType == 7) {
				if (zoom >= 14) {
					textColor = 0xff6666ff;
					shadowRadius = 1;
					if (zoom == 14) {
						dy = -7;
						textSize = 8;

					} else {
						dy = -10;
						textSize = 10;
					}
				}

			}
		}
			break;
		case MapRenderingTypes.RAILWAY: {
			if (zoom >= 14) {
				textColor = 0xff6666ff;
				shadowRadius = 1;
				if (subType == 13) {
					bold = true;
					if (zoom == 14) {
						dy = -8;
						textSize = 9;
					} else {
						dy = -10;
						textSize = 11;
					}
				} else if (subType == 22 || subType == 23) {
					if (zoom == 14) {
						dy = -7;
						textSize = 8;
					} else {
						dy = -10;
						textSize = 10;
					}
				}
			}
		}
			break;
		case MapRenderingTypes.EMERGENCY: {
			if (zoom >= 17) {
				if (subType == 10) {
					dy = 9;
					textColor = 0xff734a08;
					wrapWidth = 30;
					textSize = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.NATURAL: {
			if (subType == 23) {
				if (zoom >= 12) {
					shadowRadius = 2;
					textColor = 0xff00000;
					textSize = 10;
					wrapWidth = 10;
				}
			} else if (subType == 13) {
				if (zoom >= 14) {
					shadowRadius = 1;
					textColor = 0xff654321;
					textSize = 9;
					dy = 5;
				}
			} else if (subType == 3) {
				if (zoom >= 15) {
					shadowRadius = 1;
					textColor = 0xff654321;
					textSize = 10;
					dy = 9;
					wrapWidth = 20;
				}
			} else if (subType == 21) {
				if (zoom >= 12) {
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				}
			} else if (subType == 2) {
				if (zoom >= 14) {
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				}
			} else if (subType == 17) {
				if (zoom >= 16) {
					textSize = 8;
					shadowRadius = 1;
					dy = 10;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				}
			}
		}
			break;
		case MapRenderingTypes.LANDUSE: {
			if (zoom >= 15) {
				if (subType == 22) {
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				} else if (point) {
					textColor = 0xff000000;
					shadowRadius = 2;
					wrapWidth = 10;
					textSize = 9;
				}
			}
		}
			break;
		case MapRenderingTypes.TOURISM: {
			if (subType == 9) {
				if (zoom >= 16) {
					textColor = 0xff6699cc;
					shadowRadius = 1;
					dy = 15;
					textSize = 9;
				}
			} else if (subType == 12 || subType == 13 || subType == 14) {
				if (zoom >= 17) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					dy = 14;
					textSize = 10;
				}
			} else if (subType == 11) {
				if (zoom >= 17) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					dy = 13;
					textSize = 8;
				}
			} else if (subType == 4) {
				if (zoom >= 17) {
					shadowRadius = 1;
					textSize = 10;
					textColor = 0xff0066ff;
					wrapWidth = 70;
					dy = 15;
				}
			} else if (subType == 5) {
				if (zoom >= 17) {
					shadowRadius = 1;
					textSize = 10;
					textColor = 0xff0066ff;
					wrapWidth = 70;
					dy = 19;
				}
			} else if (subType == 7) {
				if (zoom >= 15) {
					textColor = 0xff734a08;
					textSize = 9;
					wrapWidth = 30;
					shadowRadius = 1;
				}
			} else if (subType == 15) {
				if (zoom >= 17) {
					textColor = 0xff734a08;
					textSize = 10;
					dy = 12;
					shadowRadius = 1;
				}
			}
		}
			break;
		case MapRenderingTypes.LEISURE: {
			if (subType == 8) {
				if (zoom >= 15) {
					textColor = Color.BLUE;
					textSize = 9;
					wrapWidth = 30;
					shadowRadius = 1;
				}
			} else if ((zoom >= 15 && !point) || zoom >= 17) {
				textColor = 0xff000000;
				shadowRadius = 2;
				wrapWidth = 15;
				textSize = 9;
			}
		}
			break;
		case MapRenderingTypes.HISTORIC: {
			if (zoom >= 17) {
				if (subType == 6) {
					shadowRadius = 1;
					textColor = 0xff654321;
					textSize = 9;
					dy = 12;
					wrapWidth = 20;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_TRANSPORTATION: {
			if (zoom >= 17) {
				if (subType == 1) {
					dy = 9;
					textColor = 0xff0066ff;
					textSize = 9;
					wrapWidth = 34;
				} else if (subType == 4 || subType == 18) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					dy = 13;
					textSize = 9;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_EDUCATION: {
			if (subType == 4) {
				if (zoom >= 17) {
					dy = 12;
					textColor = 0xff734a08;
					bold = true;
					textSize = 10;
				}
			} else if (subType == 5) {
				if (zoom >= 15) {
					textColor = 0xff000033;
					bold = true;
					textSize = 9;
					wrapWidth = 16;
				}
			} else if (subType == 1 || subType == 2 || subType == 3) {
				if (zoom >= 16) {
					textColor = 0xff000033;
					if(subType != 1){
						dy = 11;
					}
					textSize = 9;
					wrapWidth = 16;
				}
			}
		}
			break;
		case MapRenderingTypes.MAN_MADE: {
			if (subType == 1 || subType == 5) {
				if(zoom >= 16){
					textColor = 0xff444444;
					textSize = 9;
					if(zoom >= 17){
						textSize = 11;
						if(zoom >= 18){
							textSize = 15;
						}
					} 
					wrapWidth = 16;
				}
			} else if (subType == 17) {
				if (zoom >= 15) {
					textColor = 0xff000033;
					textSize = 9;
					shadowRadius = 2;
					dy = 16;
					wrapWidth = 12;
				}
			} else if (subType == 27) {
				if (zoom >= 17) {
					textSize = 9;
					textColor = 0xff734a08;
					dy = 12;
					shadowRadius = 1;
					wrapWidth = 20;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_ENTERTAINMENT: {
			if (zoom >= 17) {
				textSize = 9;
				textColor = 0xff734a08;
				dy = 12;
				shadowRadius = 1;
				wrapWidth = 15;
			}
		} break;
		case MapRenderingTypes.AMENITY_FINANCE: {
			if (subType == 2) {
				if (zoom >= 17) {
					shadowRadius = 1;
					textSize = 9;
					textColor = Color.BLACK;
					dy = 14;
				}
			}
		}
			break;
		case MapRenderingTypes.MILITARY: {
			if (subType == 4) {
				if (zoom >= 12) {
					bold = true;
					textSize = 9;
					shadowRadius = 1;
					wrapWidth = 10;
					textColor = 0xffffc0cb;
				}
			}
		}
			break;
		case MapRenderingTypes.SHOP: {
			if (subType == 42 || subType == 13 || subType == 16 || subType == 19 || subType == 31 || subType == 48) {
				if (zoom >= 17) {
					textColor = 0xff993399;
					textSize = 8;
					dy = 13;
					shadowRadius = 1;
					wrapWidth = 14;
				}
			} else if (subType == 65 || subType == 17) {
				if (zoom >= 16) {
					textSize = 9;
					textColor = 0xff993399;
					dy = 13;
					shadowRadius = 1;
					wrapWidth = 20;
				}
			}

		}
			break;
		case MapRenderingTypes.AMENITY_HEALTHCARE: {
			if (subType == 2) {
				if (zoom >= 16) {
					textSize = 8;
					textColor = 0xffda0092;
					dy = 12;
					shadowRadius = 2;
					wrapWidth = 24;
				}
			} else if (subType == 1) {
				if (zoom >= 17) {
					textSize = 8;
					textColor = 0xffda0092;
					dy = 11;
					shadowRadius = 1;
					wrapWidth = 12;
				}
			}

		}
			break;
		case MapRenderingTypes.AMENITY_OTHER: {
			if (subType == 10) {
				if (zoom >= 17) {
					wrapWidth = 30;
					textSize = 10;
					textColor = 0xff734a08;
					dy = 10;
				}
			} else if (subType == 26) {
				if (zoom >= 17) {
					wrapWidth = 30;
					textSize = 11;
					textColor = 0x000033;
					dy = 10;
				}
			} else if (subType == 16) {
				if (zoom >= 16) {
					textColor = 0xff6699cc;
					shadowRadius = 1;
					dy = 15;
					textSize = 9;
				}
			} else if (subType == 7) {
				if (zoom >= 17) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					wrapWidth = 20;
					dy = 8;
					textSize = 9;
				}
			} else if (subType == 13) {
				if (zoom >= 17) {
					textColor = 0xff734a08;
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					dy = 16;
				}
			} else if (subType == 2) {
				if (zoom >= 16) {
					textColor = 0xff660033;
					textSize = 10;
					shadowRadius = 2;
					wrapWidth = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_SUSTENANCE: {
			if (zoom >= 17) {
				if (subType >= 1 && subType <= 4) {
					shadowRadius = 1;
					textColor = 0xff734a08;
					wrapWidth = 34;
					dy = 13;
					textSize = 10;
				} else if (subType >= 4 && subType <= 6) {
					shadowRadius = 1;
					textColor = 0xff734a08;
					wrapWidth = 34;
					dy = 13;
					textSize = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.ADMINISTRATIVE: {
			shadowRadius = 1;
			switch (subType) {
			case 11: {
				if (zoom >= 14 && zoom < 16) {
					textColor = 0xFF000000;
					textSize = 8;
				} else if (zoom >= 16) {
					textColor = 0xFF777777;
					textSize = 11;
				}
			}
				break;
			case 8:
			case 9: {
				if (zoom >= 12 && zoom < 15) {
					textColor = 0xFF000000;
					textSize = 9;
				} else if (zoom >= 15) {
					textColor = 0xFF777777;
					textSize = 12;
				}
			}
				break;
			case 10: {
				if (zoom >= 12 && zoom < 14) {
					textColor = 0xFF000000;
					textSize = 10;
				} else if (zoom >= 14) {
					textColor = 0xFF777777;
					textSize = 13;
				}
			}
				break;
			case 19: {
				if (zoom >= 8) {
					textColor = 0xFF99cc99;
					wrapWidth = 14;
					if (zoom < 10) {
						bold = true;
						textSize = 8;
					} else if (zoom < 12) {
						bold = true;
						textSize = 11;
					}
				}
			}
				break;
			case 12: {
				if (zoom >= 10) {
					textColor = 0xFF000000;
					textSize = 9;
				}
			}
				break;
			case 7: {
				wrapWidth = 20;
				if (zoom >= 9 && zoom < 11) {
					textColor = 0xFF000000;
					textSize = 8;
				} else if (zoom >= 11 && zoom < 14) {
					textColor = 0xFF000000;
					textSize = 11;
				} else if (zoom >= 14) {
					textColor = 0xFF777777;
					textSize = 13;
				}
			}
				break;
			case 6: {
				wrapWidth = 20;
				textColor = 0xFF000000;
				if (zoom >= 6 && zoom < 9) {
					textSize = 8;
				} else if (zoom >= 9 && zoom < 11) {
					textSize = 11;
				} else if (zoom >= 11 && zoom <= 14) {
					textSize = 14;
				}
			}
				break;
			case 42: {
				wrapWidth = 20;
				textColor = 0xff9d6c9d;
				if (zoom >= 2 && zoom < 4) {
					textSize = 8;
				} else if (zoom >= 4 && zoom < 7) {
					textSize = 10;
				}
			}
				break;
			case 43:
			case 44: {
				wrapWidth = 20;
				textColor = 0xff9d6c9d;
				if (zoom >= 4 && zoom < 8) {
					textSize = 9;
				} else if (zoom >= 7 && zoom < 9) {
					textSize = 11;
				}
			}
				break;
			case 33: {
				if (zoom >= 17) {
					textSize = 9;
					textColor = 0xff444444;
				}
			}
				break;
			}
		}
			break;
		}
		rc.textColor = textColor;
		rc.textSize = textSize;
		rc.textMinDistance = textMinDistance;
		rc.showTextOnPath = showTextOnPath;
		rc.textShield = textShield;
		rc.textWrapWidth = wrapWidth;
		rc.textHaloRadius = shadowRadius;
		rc.textBold = bold;
		rc.textDy = dy;
		return name;
	}
}
