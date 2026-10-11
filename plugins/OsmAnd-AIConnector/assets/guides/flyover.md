# OsmAnd flyover videos (osmand_flyover)

How to make a map flyover video with the OsmAnd AI Connector. Read this before the first `osmand_flyover` call.
Each section says whether it already works (**Status: works**) or is planned (**Status: planned**); do not pass
planned parameters yet, OsmAnd ignores or rejects them.

## How it renders

OsmAnd takes its OpenGL map renderer off the screen (the same way Android Auto does) and renders the video frame
by frame into an MP4 file on the phone. The phone's own map pauses meanwhile and comes back afterwards.

- Works with OsmAnd in the background and the screen off. Needs the OpenGL renderer (the default) and the
  Screenshots permission group.
- Every frame waits until the map data and labels are loaded, and frames get exact timestamps, so the video is
  smooth whatever the speed of the phone.
- Rendering is slower than real time: about 0.3 s per 1280x720 frame on a fast phone with the map data cached
  (about 19 s per second of video at 60 fps), 0.7-0.8 s per frame the first time an area is drawn. Higher
  resolutions take longer. Tell the user the expected wait before long videos.
- Frames are rendered at twice the size and scaled down (smooth lines) while the doubled size fits 3840 px.
- Map labels and icons are laid out again every 2.5 s of video time and fade in and out in 0.4 s.

### Keep the map slow on the screen

Sharp map lines strobe when the map moves fast across the screen. The result reports `max_screen_speed_px_s`;
keep it at or below about 250 px/s. If a part is too fast, give it more time or a lower zoom (the camera higher).
At zoom 15 and 1280x720 a camera move of 1 km takes roughly 8-10 s to stay smooth; one zoom level lower halves it.

## Call: camera key frames — Status: works

```json
{
  "keyframes": [
    {"lat": 52.3789, "lon": 4.9006, "zoom": 15, "rotation": 0, "tilt": 90, "time_s": 0},
    {"lat": 52.3740, "lon": 4.8920, "zoom": 15.5, "rotation": -60, "tilt": 55, "time_s": 5},
    {"lat": 52.3598, "lon": 4.8856, "zoom": 16.5, "rotation": -180, "tilt": 40, "time_s": 15}
  ],
  "width": 1280, "height": 720, "fps": 60, "name": "amsterdam"
}
```

| Field | Meaning |
|---|---|
| `keyframes[]` | at least 2, `time_s` ascending, last 0 < t <= 600 |
| `lat`, `lon`, `zoom` | camera center and zoom (fractional zoom is fine) |
| `rotation` | degrees, the direction that is up on the screen; 0 = north up; defaults to the previous key frame or 0 |
| `tilt` | camera angle above the horizon: 90 = flat map, 30-50 = 3D view; defaults to the previous key frame or 90 |
| `width`, `height` | even, 16-3840; default 1280x720 |
| `fps` | 1-60, default 30; 60 is smoother on fast parts and takes twice as long |
| `name` | file name; default `flyover_<date-time>.mp4` |

Between key frames the center follows a smooth curve (Catmull-Rom) and zoom, rotation and tilt ease in and out.

Result: `file` (path on the phone, in OsmAnd's `flyover` folder), `frames`, `render_s`, `max_screen_speed_px_s`,
and a `hint` when the map moved too fast.

Tips: turn on terrain and 3D relief with `osmand_set_preference` before mountain flights; show tracks with
`osmand_show_gpx` before the call so they are in the video.

---

Everything below is the planned extension of the same call.

## Camera modes — Status: planned

`camera` replaces `keyframes`:

- `{"keyframes": [...]}` — as above.
- `{"follow": 1, "zoom": 15, "tilt": 50, "look_ahead_s": 3}` — fly along track number 1 of `gpx[]`, heading
  from the track ahead, smoothed.
- Both can be combined in time: key frames first, then follow.

## Tracks on the map: gpx[] — Status: planned

A list of GPX files to show; route files are GPX too. Numbers in values (`gpx[1]`) count from 1 in this list.

| Field | Values | Meaning |
|---|---|---|
| `file` | path on the phone | the GPX (see `osmand_list_tracks`) |
| `from_s` / `to_s` / `fade_s` | seconds | when the track appears and disappears |
| `draw` | `full` / `progressive` | whole track at once, or growing up to its current point |
| `time` | `{"mode": "track", "speed_x": 60, "offset_s": 0}` or `{"mode": "duration", "duration_s": 30}` | how track time maps to video time; `duration` spreads the track by distance (routes without times) |
| `remaining` | `hidden` / `faded` / `thin` | the part not reached yet, with `progressive` |
| `tail_km` / `tail_s` | number | show only the last part of the drawn track |
| `head` | `none` / `dot` / `arrow` / `profile_icon` | marker at the current point |

Style, by default taken from the GPX file's own OsmAnd extensions, can be overridden: `color`, `width`
(`thin`/`medium`/`bold`/number), `coloring_type` (`solid`, `speed`, `altitude`, `slope`, or a point attribute such
as `hr`), `color_palette`, `gradient_scale_type`, `show_arrows`, `show_start_finish`, `split_type`,
`split_interval`, `visualization_3d` (`track_visualization_type`), `wall_coloring`
(`track_3d_wall_coloring_type`), `line_position_3d` (`track_3d_line_position_type`), `vertical_exaggeration`,
`elevation_meters`, `join_segments`, `show_wpts` (default true).

## Waypoints of a track — Status: planned

Standard tags are used as they are: `name`, `desc`, `time`, `ele`, `type`, `link`, `osmand:icon`, `osmand:color`,
`osmand:background`, `osmand:address`, `osmand:hidden`. The icon is drawn by the map; a label or bubble is drawn
over the video at the waypoint's place on the screen.

Extensions on `<wpt>` for the flyover:

| Tag | Values | Meaning |
|---|---|---|
| `osmand:flyover_show` | `auto` / `always` / `never` | `auto`: when the track's current point reaches the waypoint (`progressive`) or the camera is near it (`full`) |
| `osmand:flyover_show_s` | seconds | explicit time to appear, overrides `auto` |
| `osmand:flyover_duration_s` | seconds, default 3 | how long it stays |
| `osmand:flyover_fade_s` | seconds, 0 = none | fade in and out |
| `osmand:flyover_style` | `auto` / `icon` / `label` / `bubble` | `auto`: a short name is a label, a long text or a description is a bubble |
| `osmand:flyover_text` | text | shown instead of the name and description |
| `osmand:flyover_fields` | `name,desc,time,ele,address` | what a bubble shows |

## OsmAnd elements — Status: planned

`"osmand_widgets": ["profile_icon", "ruler", "compass"]` — the app's own profile icon, ruler and compass, drawn as
in OsmAnd. The © OpenStreetMap contributors attribution is always drawn.

## Panels and widgets — Status: planned

Panels are placed like a FrameLayout: `gravity` (`top|left`, `top|center`, `top|right`, `center|left`, `center`,
`center|right`, `bottom|left`, `bottom|center`, `bottom|right`), `margin` (px of the video), `width`/`height`
(`wrap` or a fraction of the frame), `orientation` (`vertical`/`horizontal`), `from_s`/`to_s`/`fade_s`.
Panels look like OsmAnd map widgets (day/night theme).

A panel holds widgets of three types:

- `{"type": "attribute", "title": "Distance", "value": "gpx[1].progress_dist", "unit": "auto", "digits": 1}` —
  a value at the current frame;
- `{"type": "text", "text": "Morning ride"}` — fixed text;
- `{"type": "chart", "title": "Elevation", "value": "gpx[1].ele"}` — the same value drawn over the whole track with
  a cursor at the current point.

`template` combines values: `"{gpx[1].progress_dist} / {gpx[1].dist}"`. `unit`: `auto` (the profile's units) or
`km`, `mi`, `m`, `ft`, `km/h`, `mph`, `min/km`, `min/mi`, `bpm`, `rpm`, `W`, `°C`, `%`, `h:mm:ss`. A value that the
track does not have is shown as "—".

### Values

At the current point (interpolated): `gpx[i].speed`, `.pace`, `.ele`, `.slope`, `.hr`, `.cadence`, `.power`,
`.sensor_speed`, `.temp_air`, `.temp_water`, `.bike_distance`, `.obd.vehicle_speed`, `.obd.rpm`,
`.obd.engine_load`, `.obd.throttle`, `.obd.fuel_consumption`, `.obd.fuel_remaining`, `.obd.coolant_temp`,
`.obd.oil_temp`, `.obd.intake_temp`, `.obd.ambient_temp`, `.obd.battery_voltage`, `.obd.fuel_pressure`, `.time`,
`.lat`, `.lon`, `.ext.<tag>` (any point tag).

Progress: `gpx[i].progress_dist`, `.remaining_dist`, `.progress`, `.elapsed`, `.moving_elapsed`,
`.remaining_time`, `.climb_so_far`, `.descent_so_far`, `.avg_speed_so_far`, `.max_speed_so_far`,
`.next_wpt.name`, `.next_wpt.dist`.

Whole track: `gpx[i].dist`, `.dist_moving`, `.duration`, `.duration_moving`, `.start_time`, `.end_time`, `.date`,
`.climb`, `.descent`, `.ele_min`, `.ele_max`, `.ele_avg`, `.speed_avg`, `.speed_max`, `.speed_min`, `.hr_avg`,
`.hr_max`, `.hr_min`, `.cadence_avg`, `.cadence_max`, `.power_avg`, `.power_max`, `.temp_avg`, `.temp_max`,
`.points`, `.wpt_count`, `.segments`.

Track description: `gpx[i].name`, `.desc`, `.author`, `.activity`, `.city`, `.file`, `.meta.<tag>`.

Video and camera: `video.time`, `video.progress`, `camera.zoom`, `camera.lat`, `camera.lon`, `camera.heading`,
`camera.tilt`, `camera.ele` (terrain under the camera).

Your own data: `data.<name>` — a series or constant passed in the call as
`"data": {"<name>": {"by": "time"|"distance", "gpx": 1, "points": [[x, value], ...]}}` or `{"<name>": 42}`.

## Example: trip recap — Status: planned

```json
{
  "camera": {"follow": 1, "zoom": 15, "tilt": 50},
  "gpx": [{"file": "rec/2026-10-11_ride.gpx", "draw": "progressive", "time": {"mode": "duration", "duration_s": 30}, "head": "profile_icon"}],
  "osmand_widgets": ["profile_icon", "ruler", "compass"],
  "panels": [
    {"gravity": "top|left", "margin": 16, "widgets": [
      {"type": "text", "text": "Morning ride"},
      {"type": "attribute", "title": "Distance", "template": "{gpx[1].progress_dist} / {gpx[1].dist}", "unit": "km", "digits": 1},
      {"type": "attribute", "title": "Heart rate", "value": "gpx[1].hr", "unit": "bpm"}
    ]},
    {"gravity": "bottom|center", "width": 0.9, "widgets": [
      {"type": "chart", "title": "Elevation", "value": "gpx[1].ele"}
    ]}
  ],
  "width": 1920, "height": 1080, "fps": 30
}
```
