package btools.routingapp;


interface IBRouterService {


    //param params--> Map of params:
    //  "pathToFileResult"-->String with the path to where the result must be saved, including file name and extension
    //                    -->if null, the track is passed via the return argument
    //  "maxRunningTime"-->String with a number of seconds for the routing timeout, default = 60
    //  "trackFormat"-->[kml|gpx] default = gpx
    //  "lats"-->double[] array of latitudes; 2 values at least.
    //  "lons"-->double[] array of longitudes; 2 values at least.
    //  "nogoLats"-->double[] array of nogo latitudes; may be null.
    //  "nogoLons"-->double[] array of nogo longitudes; may be null.
    //  "nogoRadi"-->double[] array of nogo radius in meters; may be null.
    //  "fast"-->[0|1]
    //  "v"-->[motorcar|bicycle|foot]
    //return null if all ok and no path given, the track if ok and path given, an error message if it was wrong
    //call in a background thread, heavy task!

    String getTrackFromParams(in Bundle params);
}