# OsmAnd flyover videos (osmand_flyover)

How to make a map flyover video with the OsmAnd AI Connector. Read this before the first `osmand_flyover` call.
Sections and fields are marked **works** or **planned**. Keys this OsmAnd does not know come back in the result
as `warnings`; nothing is silently dropped.

## How it renders

OsmAnd takes its OpenGL map renderer off the screen (as Android Auto does) and renders the video frame by frame
into an MP4 file on the phone. The phone's map pauses meanwhile and comes back afterwards; the map is unlinked
from the location for the recording and linked again after it.

- Works with OsmAnd in the background and the screen off. Needs the OpenGL renderer (the default) and the
  Screenshots permission group.
- Every frame waits until the map data and labels are loaded, and frames get exact timestamps: the video is
  smooth whatever the speed of the phone.
- Rendering is slower than real time: about 0.3 s per 1280x720 frame on a fast phone with the map data cached
  (about 19 s per second of video at 60 fps, 10 s at 30 fps), 0.7-0.8 s per frame when an area is drawn for the
  first time. Tell the user the expected wait before long videos.
- Map frames are rendered at twice the size and scaled down (smooth lines) while the doubled size fits 3840 px.
- Map labels and icons are laid out again every 2.5 s of video time and fade in and out in 0.4 s.

### Keep the map slow on the screen

Sharp map lines strobe when the map moves fast across the screen. The result reports `max_screen_speed_px_s`;
keep it at or below about 250 px/s. If a part is too fast, give it more time or a lower zoom (the camera higher).
At zoom 15 and 1280x720 a camera move of 1 km takes roughly 8-10 s to stay smooth; one zoom level lower halves it.

## The call

| Field | Status | Meaning |
|---|---|---|
| `keyframes[]` | works | camera key frames, see below; or `camera.follow`, `camera.drone`, `camera.poses` |
| `camera` | works | `{"follow": n, "zoom": 15, "tilt": 50, "look_ahead_s": 3}`: fly along track n of `gpx[]`; `"follow": [1, 2]` keeps several tracks in the frame (ghost mode: tracks with `"time": {"mode": "track"}` move by their own times), zooming out down to `min_zoom` (default 10) when they part; center, heading and zoom are averaged over `smooth_s` of video (default 2) |
| `duration_s` | works | video length; needed with `camera.follow` and `camera.drone`, else the last key frame's or pose's time |
| `width`, `height` | works | even, 16-3840; default 1280x720 |
| `fps` | works | 1-60, default 30; 60 is smoother on fast parts and takes twice as long |
| `name` | works | file name; default `flyover_<date-time>.mp4` |
| `ease_in_s`, `ease_out_s` | works | the camera speeds up after the start and slows down before the end; default 1 s each (at most a third of the video) |
| `scale` | works | `video` (default): map symbols sized for the video; `screen`: the phone's own map scale, so a video can continue a screen recording |
| `load_timeout_s` | works | how long a frame may wait for map data, default 30 s for the first frame and 5 s for the others; frames recorded before the map finished loading are counted in `warnings` |
| `smooth_s` | works | point values in widgets (speed, heart rate, altitude...) are averaged over this much video time, default 1.5; 0 shows raw values |
| `refresh_s` | works | numbers in widgets change at most this often, default 0.25 (as a bike computer); 0 changes them every frame |
| `detail` | works | `low`, `normal` (default) or `high`: how sharp the map is. The recorder picks the map magnifier and how far into a tilted map tiles keep full detail from the camera's zoom over the video, and the tracks' relief, and says what it chose in `warnings`. Higher is slower |
| `map_magnifier` | works | sets OsmAnd's map magnifier for this video by hand (0.25-4) instead of `detail`'s choice; the user's value is put back |
| `ui_scale` | works | size of everything drawn over the map (OsmAnd elements, panels, callouts); default 1; the frame's long side is 800 dp |
| `gpx[]` | works | tracks to show, see below |
| `osmand_widgets` | works | `["profile_icon", "ruler", "compass"]` |
| `profile` | works | profile key for the profile icon; default the current profile |
| `attribution` | works | default "OsmAnd · © OpenStreetMap"; it must credit OpenStreetMap |
| `panels[]` | works | panels of widgets, see below |
| `data` | works | your own values for panels, see Values |

Result: `file` (path on the phone, in OsmAnd's `flyover` folder), `frames`, `render_s`, `max_screen_speed_px_s`,
`hint` when the map moved too fast, `warnings` for keys this OsmAnd ignored.

### Key frames — works

```json
{"keyframes": [
  {"lat": 52.3789, "lon": 4.9006, "zoom": 15, "rotation": 0, "tilt": 90, "time_s": 0},
  {"lat": 52.3740, "lon": 4.8920, "zoom": 15.5, "rotation": -60, "tilt": 55, "time_s": 5},
  {"lat": 52.3598, "lon": 4.8856, "zoom": 16.5, "rotation": -180, "tilt": 40, "time_s": 15}
], "width": 1280, "height": 720, "fps": 60, "name": "amsterdam"}
```

`target`: `[x, y]`, where the camera's point sits on the frame as fractions of its width and height (default OsmAnd's
own map position, e.g. `[0.5, 0.5]` or lower in navigation); interpolated like the other values, so a video can
start exactly where a screen recording of the phone ended. `camera.follow` takes `target` too.
`rotation`: degrees, the direction that is up on the screen (OsmAnd's map rotation, minus the heading); 0 = north
up, 272 (= -88) shows an eastbound track going up. `tilt`: camera angle above the horizon, 90 = flat, 30-50 = 3D. Both default to the previous key frame or 0 / 90.
The camera passes every key frame at its time without stopping: center, zoom, rotation and tilt follow smooth
curves through the key frames; it eases only at the very start and end. A key frame for every video frame also
works (your own path).

Tips: turn on terrain and 3D relief with `osmand_set_preference` before mountain flights.

### Drone camera — works

The camera is where a drone is and looks where its camera looks; OsmAnd finds the point on the relief it looks at.
Give the poses either as key frames or as a track:

```json
{"camera": {"poses": [
  {"time_s": 0, "lat": 46.035, "lon": 7.775, "alt_m": 4000, "heading": 235, "pitch": -30},
  {"time_s": 3, "lat": 46.035, "lon": 7.775, "alt_m": 4000, "heading": 325, "pitch": -30}
]}, "width": 720, "height": 1280, "fps": 30}
```

`alt_m`: height above the sea (`"alt_ref": "ground"` on a pose or on `camera`: above the relief). `heading`: where
the camera looks, 0 north. `pitch`: below 0 looks down, -90 straight down; the view cannot be flatter than 10° down.
`fov`: vertical field of view, default 33°. Poses follow smooth curves; one pose per video frame also works.
`"camera": {"drone": n}` takes the poses from track n of `gpx[]`: lat/lon/ele (`"altitude": "msl"` default,
`"takeoff"` adds the ground at the first point, `"agl"` the ground under each point) and point tags `yaw`/`heading`,
`pitch`, `fov` (without `yaw` it looks along the track, without `pitch` it takes `camera.pitch`, default -30);
averaged over `smooth_s` (default 1). The camera never goes lower than `min_agl_m` (default 5) above the relief.
The map's detail level stays the same while the camera keeps about the same distance and changes as it nears or
leaves. `warnings` tell how far the camera's height was off (usually a few meters; flights low over high mountains
are less exact). Needs the terrain maps of the area.

While a video is recorded the map on the phone is busy: a banner shows the progress with a Cancel button and the map
takes no touches (a cancelled recording returns an error). Afterwards the camera, the shown tracks, the link to the
location and the magnifier are as before; if OsmAnd crashes meanwhile, they are put back at its next start.

## Tracks: gpx[]

Route files are GPX too. Numbers in values (`gpx[1]`) count from 1 in this list. The tracks are shown for the
recording only; the user's own shown tracks are put back afterwards. Track waypoints are drawn by the map.

| Field | Status | Meaning |
|---|---|---|
| `file` | works | a saved track: path in OsmAnd's tracks folder, e.g. `rec/2026-10-11_ride.gpx` or `import/x.gpx` (see `osmand_list_tracks`) |
| `gpx_data` | works | instead of `file`: a GPX passed in the call as text (any track or route); used for this recording only, not added to the user's tracks. Up to about 300 KB (the whole call travels in one Android Binder transaction); a bigger track goes to the phone as a file first |
| `profile` | works | profile key whose map icons the head uses; default the file's `osmand:profile`, else the current profile; the first track's profile is also the profile icon's when the request has no `profile` |
| `time` | works | `{"mode": "duration", "duration_s": 30}` (default: the whole video): spread over the time, by the track's own times when it has them, else by distance; `{"mode": "track", "speed_x": 60, "offset_s": 0}`: the track's own times sped up. With `duration` the track speeds up and slows down over `ease_in_s` / `ease_out_s` (default 1 s each) |
| `from_s` | works | when the track appears and starts moving (its current point stays at the start before) |
| style | works | over the file's own: `color`, `width` (`thin`/`medium`/`bold`/number), `coloring_type` (`solid`, `speed`, `altitude`, `slope`, ...), `color_palette`, `show_arrows`, `show_start_finish`, `split_type`, `split_interval`, `visualization_3d`, `wall_coloring`, `line_position_3d`, `vertical_exaggeration`, `elevation_meters`, `show_wpts`. Drawn by OsmAnd's own track layer, so a track looks as in the app; `show_arrows` and `show_start_finish` are on unless the file or the request turns them off (a saved track keeps the look the user gave it) |
| `to_s`, `fade_s` | works | when the track disappears; it fades in after `from_s` and out before `to_s` |
| `draw` | works | `full` (default) or `progressive`: the track grows up to its current point |
| `remaining` | works | with `progressive`, the part not reached yet: `hidden` (default), `faded`, `thin` |
| `tail_km`, `tail_s` | works | with `progressive`, only the last part of the passed track is drawn (a comet) |
| `head` | works | marker at the current point: `navigation` (default with `progressive`: the profile's navigation icon, turned along the track), `location` (the profile's location icon), `arrow`, `dot`, `none` |
| `head_icon` | works | one of OsmAnd's own icons instead of the profile's: `STATIC_DEFAULT`, `STATIC_CAR`, `STATIC_BICYCLE`, `MOVEMENT_DEFAULT`, `MOVEMENT_CAR`, `MOVEMENT_NAUTICAL`, or an installed 3D model name (drawn as its flat preview) |

Every setting of a `gpx[]` entry can also be stored in the GPX file itself as an extension of the file or its first
track, `<osmand:flyover_draw>progressive</osmand:flyover_draw>`, `<osmand:flyover_head>arrow</osmand:flyover_head>`;
the request's JSON wins over the file.

### Waypoint callouts — works

A label or bubble over a waypoint, at its place on the screen (with relief and tilt). By default it shows when the
track's current point reaches the waypoint (within 300 m of the track) for 3 s; a waypoint away from the track
shows while it is on the screen. Extensions on `<wpt>`:

| Tag | Values | Meaning |
|---|---|---|
| `osmand:flyover_show` | `auto` / `always` / `never` | when to show |
| `osmand:flyover_show_s` | seconds | explicit time to appear |
| `osmand:flyover_duration_s` | seconds, default 3 | how long it stays |
| `osmand:flyover_fade_s` | seconds, default 0.4; 0 = none | fade in and out |
| `osmand:flyover_style` | `auto` / `label` / `bubble` | `auto`: a short name is a label; a description or a long name is a bubble |
| `osmand:flyover_text` | text | shown instead of the name and description |
| `osmand:flyover_fields` | `desc,address,ele,time` | what a bubble shows under the name |

The same settings without the `osmand:flyover_` prefix can come from the request: `"wpt_defaults": {...}` in a
`gpx[]` entry applies to all its waypoints, `"wpt": {"<waypoint name>": {"style": "bubble", "show_s": 4}}` to one.
Order: `wpt` in the request, then the waypoint's own tags, then `wpt_defaults`.

## OsmAnd elements — works

`"osmand_widgets": ["profile_icon", "ruler", "compass"]`: the profile icon (top left), OsmAnd's ruler (bottom left)
and compass (top right, turns with the map), drawn as in OsmAnd. The attribution is always drawn bottom right.
Keep panels clear of these corners with `margin`.

Laying out panels: the frame's corners hold the profile icon (top left), compass (top right), ruler (bottom left)
and attribution (bottom right); a chart usually goes `bottom|center` with the value panels above it or in the top
corners below the OsmAnd elements (`margin` about 88 at 1280x720). Each panel keeps the size of its widest value over
the whole video, so it does not jump. Panels covering each other or these elements are listed in `warnings`; fix the
layout before a long render (a 1 s test video at a low fps is cheap).

## Panels — works

Placed like a FrameLayout:

| Field | Meaning |
|---|---|
| `gravity` | `top|left`, `top|center`, `top|right`, `center|left`, `center`, `center|right`, `bottom|left`, `bottom|center`, `bottom|right` |
| `margin` | from the frame's edge, video pixels; default 16 |
| `width` | none: as wide as the widgets; 0-1: a fraction of the frame; above 1: video pixels |
| `orientation` | `vertical` (default) or `horizontal` |
| `size` | widget size `small` / `medium` (default) / `large`, as OsmAnd's widget sizes; a widget may set its own |
| `background` | `default` (the user's widget look), `transparent` (OsmAnd's transparent widgets, outlined text) or 0-1 opacity |
| `from_s`, `to_s`, `fade_s` | when the panel shows |
| `widgets[]` | see below |

Widgets look like OsmAnd's map widgets (icon, colours, the user's day/night widget look), laid out for video: icon,
then the title above the value and its unit. Three types:

- `{"type": "attribute", "title": "Distance", "value": "gpx[1].progress_dist", "unit": "auto", "digits": 1}` — a
  value at the current frame. `template` instead of `value` combines several: `"{gpx[1].progress_dist} / {gpx[1].dist}"`.
- `{"type": "text", "title": "Ride", "text": "Morning ride"}` — fixed text.
- `{"type": "chart", "title": "Heart rate", "value": "gpx[1].hr"}` — the value over the whole track, smoothed, the
  passed part highlighted and a cursor at the current point; `width_dp` (default 280), `height_dp` (default 56).
  `value2` adds a second value as a line on its own scale (e.g. `"value": "gpx[1].ele", "value2": "gpx[1].speed"`),
  in `color2` (default orange); put both names in the title, e.g. "Altitude · speed".

`icon`: an OsmAnd drawable name (e.g. `widget_speed`, `widget_sensor_heart_rate`); by default the icon OsmAnd's own
widget for that value has. `unit`: `auto` (the profile's units) or `km`, `mi`, `m`, `ft`, `km/h`, `mph`, `min/km`,
`min/mi`, `h:mm:ss`, or any label. A value the track does not have is shown as "—".

### Values

At the current point (interpolated): `gpx[i].speed`, `.pace`, `.ele`, `.slope`, `.hr`, `.cadence`, `.power`,
`.sensor_speed`, `.temp_air`, `.temp_water`, `.bike_distance`, `.time`, `.lat`, `.lon`, `.ext.<tag>` (any point tag).

Progress: `gpx[i].progress_dist`, `.remaining_dist`, `.progress`, `.elapsed`, `.moving_elapsed`, `.remaining_time`, `.climb_so_far`,
`.descent_so_far`, `.avg_speed_so_far`, `.max_speed_so_far`, `.next_wpt.name`, `.next_wpt.dist`.

Whole track: `gpx[i].dist`, `.dist_moving`, `.duration`, `.duration_moving`, `.start_time`, `.end_time`, `.date`,
`.climb`, `.descent`, `.<point value>_min` / `_max` / `_avg` (e.g. `.ele_max`, `.hr_avg`, `.speed_avg`), `.points`,
`.wpt_count`, `.segments`.

Track description: `gpx[i].name`, `.desc`, `.author`, `.activity`, `.city` (nearest city, from OsmAnd's track
list), `.file`, `.meta.<tag>`.

Video and camera: `video.time`, `video.progress`, `camera.zoom`, `camera.lat`, `camera.lon`, `camera.heading`,
`camera.tilt`.

Your own data: `data.<name>`, passed in the call as a constant (`"data": {"goal": 42}` or text) or a series
`{"by": "time" | "distance", "gpx": 1, "unit": "W", "points": [[x, value], ...]}` (x in video seconds or metres
along the track).

Vehicle (OBD) values are read as point tags: `gpx[i].ext.<tag>`. Planned: `camera.ele` (terrain under the camera).

## Example: trip recap

```json
{
  "camera": {"follow": 1, "zoom": 15, "tilt": 50},
  "duration_s": 30,
  "gpx": [{"file": "rec/2026-10-11_ride.gpx", "time": {"mode": "duration", "duration_s": 30}}],
  "osmand_widgets": ["profile_icon", "ruler", "compass"],
  "panels": [
    {"gravity": "top|left", "margin": 88, "size": "small", "background": 0.85, "widgets": [
      {"type": "attribute", "title": "Distance", "template": "{gpx[1].progress_dist} / {gpx[1].dist}"},
      {"type": "attribute", "title": "Heart rate", "value": "gpx[1].hr"},
      {"type": "attribute", "title": "Time", "value": "gpx[1].elapsed"}
    ]},
    {"gravity": "top|right", "margin": 88, "size": "large", "background": "transparent", "widgets": [
      {"type": "attribute", "title": "Speed", "value": "gpx[1].speed"}
    ]},
    {"gravity": "bottom|center", "margin": 40, "widgets": [
      {"type": "chart", "title": "Elevation", "value": "gpx[1].ele"}
    ]}
  ],
  "width": 1920, "height": 1080, "fps": 30
}
```
