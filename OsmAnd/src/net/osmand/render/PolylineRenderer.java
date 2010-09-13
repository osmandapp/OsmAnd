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
		switch (type) {
		case MapRenderingTypes.HIGHWAY: {
			int hwType = subtype;
			boolean carRoad = true;
			if (hwType == MapRenderingTypes.PL_HW_TRUNK) {
				color = Color.rgb(168, 218, 168);
			} else if (hwType == MapRenderingTypes.PL_HW_MOTORWAY) {
				color = Color.rgb(128, 155, 192);
			} else if (hwType == MapRenderingTypes.PL_HW_PRIMARY) {
				color = Color.rgb(235, 152, 154);
			} else if (hwType == MapRenderingTypes.PL_HW_SECONDARY) {
				color = Color.rgb(253, 214, 164);
			} else if (hwType == MapRenderingTypes.PL_HW_TERTIARY) {
				color = Color.rgb(254, 254, 179);
				shadowLayer = 2;
				shadowColor = Color.rgb(186, 186, 186);
			} else if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_UNCLASSIFIED
					|| hwType == MapRenderingTypes.PL_HW_RESIDENTIAL) {
				shadowLayer = 1;
				shadowColor = Color.rgb(194, 194, 194);
				color = Color.WHITE;
			} else if (hwType == MapRenderingTypes.PL_HW_PEDESTRIAN) {
				shadowLayer = 1;
				shadowColor = Color.rgb(176, 176, 176);
				color = Color.rgb(236, 236, 236);
			} else {
				carRoad = false;
				strokeWidth = 2;
				pathEffect = o.getDashEffect("2_2"); //$NON-NLS-1$
				if (hwType == MapRenderingTypes.PL_HW_TRACK || hwType == MapRenderingTypes.PL_HW_PATH) {
					color = Color.GRAY;
					pathEffect = o.getDashEffect("6_2"); //$NON-NLS-1$
				} else if (hwType == MapRenderingTypes.PL_HW_CYCLEWAY || hwType == MapRenderingTypes.PL_HW_BRIDLEWAY) {
					color = Color.rgb(20, 20, 250);
				} else {
					color = Color.rgb(250, 128, 115);
				}
			}
			if (carRoad) {
				if (zoom < 10) {
					// done
					strokeWidth = 0;
					if (hwType <= MapRenderingTypes.PL_HW_SECONDARY) {
						if (hwType == MapRenderingTypes.PL_HW_SECONDARY) {
							strokeWidth = zoom >= 8 ? 1 : 0;
						} else if (hwType == MapRenderingTypes.PL_HW_PRIMARY) {
							if (zoom < 7) {
								strokeWidth = 0;
							} else if (zoom == 7) {
								strokeWidth = 1.5f;
							} else if (zoom == 8 || zoom == 9) {
								strokeWidth = 2f;
							}
						} else if (hwType == MapRenderingTypes.PL_HW_TRUNK || hwType == MapRenderingTypes.PL_HW_MOTORWAY) {
							if (zoom >= 7) {
								strokeWidth = 3.5f;
							} else if (zoom == 6) {
								strokeWidth = 2;
							} else if (zoom == 5) {
								strokeWidth = 1;
							} else {
								strokeWidth = 0;
							}
						}
					}
				} else if (zoom <= 12) {
					if (hwType <= MapRenderingTypes.PL_HW_SECONDARY) {
						if (zoom < 12) {
							strokeWidth = 2;
						} else if (zoom == 12) {
							strokeWidth = 3;
						}
					} else {
						strokeWidth = 0;
					}
				} else {
					int layer = MapRenderingTypes.getWayLayer(objType);
					if (layer == 1) {
						pathEffect = o.getDashEffect("4_2"); //$NON-NLS-1$
					}
					if (zoom < 15) {
						strokeWidth = 4.5f;
					} else if (zoom < 16) {
						strokeWidth = 6;
					} else if (zoom == 16) {
						strokeWidth = 8;
					} else if (zoom == 17) {
						strokeWidth = 13;
					} else if (zoom >= 18) {
						strokeWidth = 16;
					} else if (zoom >= 19) {
						strokeWidth = 20;
					}
					if (hwType == MapRenderingTypes.PL_HW_SERVICE) {
						strokeWidth -= 2;
					}
				}
			}
			showText = (carRoad && zoom > 12) || zoom > 16;
		}
			break;
		case MapRenderingTypes.RAILWAY: {
			if (zoom < 10) {
				if (subtype == 2) {
					color = 0xffaaaaaa;
					if (zoom < 7) {
						strokeWidth = 0;
					} else if (zoom == 7) {
						strokeWidth = 1;
					} else if (zoom == 8) {
						strokeWidth = 1.5f;
					} else if (zoom == 9) {
						strokeWidth = 2;
					}
				} else {
					strokeWidth = 0;
				}
			} else {
				// TODO tunnel
				strokeWidth = 2;
				if (subtype == 6) {
					color = Color.rgb(153, 153, 153);
					if (zoom > 16) {
						strokeWidth = 3;
					}
					pathEffect = o.getDashEffect("6_3"); //$NON-NLS-1$
				} else if (subtype == 2) {
					color = Color.rgb(62, 62, 62);
				} else if (subtype == 1) {
					color = Color.rgb(153, 153, 153);
					if (zoom >= 16) {
						strokeWidth = 3;
					}
					pathEffect = o.getDashEffect("7_7"); //$NON-NLS-1$
				} else {
					color = Color.rgb(153, 153, 153);
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
						int layer = MapRenderingTypes.getWayLayer(objType);
						if (layer == 1) {
							pathEffect = o.getDashEffect("4_2"); //$NON-NLS-1$
						}
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
						int layer = MapRenderingTypes.getWayLayer(objType);
						if (layer == 1) {
							pathEffect = o.getDashEffect("4_2"); //$NON-NLS-1$
						}
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
	}
}
