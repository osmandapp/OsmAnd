"""Builds assets/assistant/settings.json: OsmAnd settings and map style properties a small model can find
by words, with their ids and values.

Usage: python3 build_settings_catalog.py <dir with the android and resources checkouts> <output json>
e.g.   python3 scripts/build_settings_catalog.py ../../.. assets/assistant/settings.json
"""
import json, re, sys, xml.etree.ElementTree as ET

REPOS = sys.argv[1]
OUT = sys.argv[2]
STYLES = ['default.render.xml', 'contourlines.addon.render.xml', 'routes.addon.render.xml']
SKIP = {'appMode', 'baseAppMode', 'engine_v1', 'legend', 'eleUnits', 'currentTrackColor', 'currentTrackWidth',
        'useDefaultBuildingColor', 'showRoadMaps', 'contourLabelDirection'}


def strings(lang):
    path = f'{REPOS}/android/OsmAnd/res/values{"-" + lang if lang else ""}/strings.xml'
    out = {}
    for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', open(path, encoding='utf-8').read(), re.S):
        out[m.group(1)] = re.sub(r'\\(.)', r'\1', m.group(2)).strip('"')
    return out


en = strings('')
# words people use that the app's strings do not have
SYN = {'contourLines': 'contour isolines elevation relief', 'contourColorScheme': 'contour color',
       'contourWidth': 'contour thickness', 'hideBuildings': 'buildings houses', 'hideHouseNumbers': 'house numbers addresses',
       'hideIcons': 'icons POI', 'showCycleRoutes': 'bicycle bike cycling', 'roadStyle': 'roads colors',
       'hikingRoutesOSMC': 'hiking walking trails', 'moreDetailed': 'details detailed'}
entries, seen = [], set()
for style in STYLES:
    root = ET.parse(f'{REPOS}/resources/rendering_styles/{style}').getroot()
    for p in root.iter('renderingProperty'):
        attr = p.get('attr')
        if attr in SKIP or attr in seen or p.get('category') == 'ui_hidden':
            continue
        seen.add(attr)
        values = 'true, false' if p.get('type') == 'boolean' else \
            ', '.join(v for v in (p.get('possibleValues') or '').split(',') if v)
        if attr == 'contourLines':  # the minimum zoom, -- is off
            values = '--, 11, 12, 13, 14, 15, 16'
        entries.append({
            'id': 'nrenderer_' + attr,
            'title': en.get(f'rendering_attr_{attr}_name', p.get('name')),
            'words': SYN.get(attr, ''),
            'about': en.get(f'rendering_attr_{attr}_description', p.get('description') or ''),
            'values': values,
            'group': 'map style'})

# app settings: ids and values from OsmandSettings and the shared enums
APP = [
    ('daynight_mode', 'Day / night mode, map theme, dark map',
     'DAY, NIGHT, AUTO (sunrise/sunset), SENSOR (light sensor), APP_THEME'),
    ('renderer', 'Map style',
     'OsmAnd, Touring view (contrast and details), Topo, UniRS, LightRS, Nautical, Ski map, Winter and ski, Offroad, Desert, OSM-carto'),
    ('map_preferred_locale', 'Map language, language of names on the map',
     'language code such as en, de, ru; empty = local names'),
    ('preferred_locale', 'App language, interface language',
     'language code; empty = system'),
    ('rotate_map', 'Map orientation, rotate map by direction or compass',
     '0 north is up, 1 movement direction, 2 compass, 3 manual'),
    ('auto_zoom_map_on_off', 'Auto zoom map while navigating',
     'true, false'),
    ('voice_mute', 'Mute voice guidance, sound of navigation prompts',
     'true, false'),
    ('default_metric_system', 'Units of length and distance: kilometers, miles',
     'KILOMETERS_AND_METERS, MILES_AND_FEET, MILES_AND_METERS, MILES_AND_YARDS, NAUTICAL_MILES_AND_METERS, NAUTICAL_MILES_AND_FEET'),
    ('default_speed_system', 'Units of speed',
     'KILOMETERS_PER_HOUR, MILES_PER_HOUR, METERS_PER_SECOND, MINUTES_PER_MILE, MINUTES_PER_KILOMETER, NAUTICALMILES_PER_HOUR'),
    ('altitude_metrics', 'Units of altitude, height and elevation: feet or meters',
     'METERS, FEET'),
    ('text_scale', 'Text size on the map, font size of labels',
     '0.75 to 3, 1 = normal'),
    ('map_density_n', 'Map magnifier, size of map details',
     '0.25 to 4, 1 = normal'),
    ('show_routing_alarms', 'Navigation warnings and alerts',
     'true, false'),
    ('show_speed_limit_warnings', 'Speed limit warning',
     'true, false'),
    ('show_cameras', 'Speed cameras warnings',
     'true, false'),
    ('show_traffic_warnings', 'Traffic warnings: speed bumps, crossings',
     'true, false'),
    ('speed_limit_exceed', 'Speed limit tolerance before warning, km/h',
     'number, e.g. 5'),
    ('keep_informing', 'Repeat navigation instructions every N minutes',
     'minutes, 0 = only when needed'),
    ('turn_screen_on_time_int', 'Turn the screen on for navigation instructions, seconds',
     'seconds, 0 = off'),
    ('speak_street_names', 'Announce street names',
     'true, false'),
    ('announce_wpt', 'Announce waypoints along the route',
     'true, false'),
    ('snap_to_road', 'Snap position to road while navigating',
     'true, false'),
    ('enable_3d_maps', '3D view of the map, tilt',
     'true, false'),
    ('enable_3d_map_objects', '3D buildings',
     'true, false'),
    ('terrain_layer', 'Terrain: hillshade, slope, relief shading',
     'true, false'),
    ('show_favorites', 'Show favorites on the map',
     'true, false'),
    ('show_coordinates_grid', 'Coordinates grid on the map',
     'true, false'),
    ('position_placement_on_map', 'Position of my location on the screen',
     '0 automatic, 1 center, 2 bottom'),
    ('animate_my_location', 'Animate my position',
     'true, false'),
]
for pid, title, values in APP:
    entries.append({'id': pid, 'title': title, 'words': '', 'about': '', 'values': values, 'group': 'app'})

json.dump(entries, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(entries), 'settings')
