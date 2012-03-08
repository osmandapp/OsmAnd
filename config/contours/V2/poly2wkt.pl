#!/usr/bin/perl

# script to convert a polygon file to a WKT file for loading it into a
# database etc.
#
# written by Frederik Ramm <frederik@remote.org>, public domain.

use strict;
use warnings;

# first line
# (employ workaround for polygon files without initial text line)
my $poly_file = <>; chomp($poly_file);
my $workaround = 0;
if ($poly_file =~ /^\d+$/)
{
    $workaround=$poly_file;
    $poly_file="none";
}

my $polygons;

while(1)
{
    my $poly_id;
    if ($workaround==0) 
    {
        $poly_id=<>;
    } 
    else 
    {
        $poly_id=$workaround;
    }
    chomp($poly_id);
    last if ($poly_id =~ /^END/); # end of file
        my $coords;

    while(my $line = <>)
    {
        last if ($line =~ /^END/); # end of poly
            my ($dummy, $x, $y) = split(/\s+/, $line);
        push(@$coords, sprintf("%f %f", $x, $y));
    }
    push(@$polygons, "((".join(",", @$coords)."))");
    $workaround=0;
}

print "MULTIPOLYGON(".join(",",@$polygons).")\n";
