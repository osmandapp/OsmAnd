package net.osmand.binary;


import java.io.IOException;

public class BinaryInspectorNative {


	public static final int BUFFER_SIZE = 1 << 20;

	public static void main(String[] args) throws IOException {
		if(args == null || args.length == 0) {
			printUsage(null);
			return;
		}
		args = new String[]{"-vmap", "-bbox=11.3,47.1,11.6,47", "/home/victor/projects/OsmAnd/data/osm-gen/Austria_2.obf"};
		// test cases show info
	}

	public static void printUsage(String warning) {
		if(warning != null){
			println(warning);
		}
		println("Inspector is console utility for working with binary indexes of OsmAnd.");
		println("It allows print info about file, extract parts and merge indexes.");
		println("\nUsage for print info : inspector [-vaddress] [-vstreetgroups] [-vstreets] [-vbuildings] [-vintersections] [-vmap] [-vpoi] [-vtransport] [-zoom=Zoom] [-bbox=LeftLon,TopLat,RightLon,BottomLat] [file]");
		println("  Prints information about [file] binary index of OsmAnd.");
		println("  -v.. more verbose output (like all cities and their streets or all map objects with tags/values and coordinates)");
		println("\nUsage for combining indexes : inspector -c file_to_create (file_from_extract ((+|-)parts_to_extract)? )*");
		println("\tCreate new file of extracted parts from input file. [parts_to_extract] could be parts to include or exclude.");
		println("  Example : inspector -c output_file input_file +1,2,3\n\tExtracts 1, 2, 3 parts (could be find in print info)");
		println("  Example : inspector -c output_file input_file -2,3\n\tExtracts all parts excluding 2, 3");
		println("  Example : inspector -c output_file input_file1 input_file2 input_file3\n\tSimply combine 3 files");
		println("  Example : inspector -c output_file input_file1 input_file2 -4\n\tCombine all parts of 1st file and all parts excluding 4th part of 2nd file");
	}

	private static void println(String string) {
		System.out.println(string);
	}
}
