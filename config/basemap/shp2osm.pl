# Copyright (c) 2006 Gabriel Ebner <ge@gabrielebner.at>
# updated in 2008 by Tobias Wendorff <tobias.wendorff@uni-dortmund.de>
# HTML-Entities based on ideas of Hermann Schwärzler
# Gauß-Krüger implementation based on gauss.pl by Andreas Achtzehn
# version 1.3 (17. September 2008)

use Geo::ShapeFile;
use HTML::Entities qw(encode_entities_numeric);
use Math::Trig;

if(@ARGV == 0) {
   print "usage:\n";
   print "with transformation from GK to WGS84: 'shp2osm.pl shapefile GK'\n";
   print "without transformation: 'shp2osm.pl shapefile'";
   exit;
}

print <<'END';
<?xml version='1.0'?>
<osm version='0.5' generator='shp2osm.pl'>
END

#BEGIN { our %default_tags = ( natural => 'water', source => 'SWDB' ); }
BEGIN { our %default_tags = (  ); }

my $file = @ARGV[0];
$file =~ s/\.shp$//;
my $shpf = Geo::ShapeFile->new($file);
proc_shpf($shpf);

{
     BEGIN { our $i = -1; }

     sub tags_out {
         my ($tags) = @_;
         my %tags = $tags ? %$tags : ();
         #$tags{'created_by'} ||= 'shp2osm.pl';
         delete $tags{'_deleted'} unless $tags{'_deleted'};
         while ( my ( $k, $v ) = each %tags ) {
             my $key = encode_entities_numeric($k);
             my $val = encode_entities_numeric($v);
             print '    <tag k="'. $key .'" v="'. $val ."\"/>\n" if $val;
         }
     }

     sub node_out {
         my ( $lon, $lat, $tags ) = @_;
         my $id = $i--;
         if(@ARGV[1] eq 'GK') {
             my ($wgs84lon, $wgs84lat) = gk2geo($lon, $lat);
             print "  <node id='$id' visible='true' lat='$wgs84lat' lon='$wgs84lon' />\n";
         } else {
            print "  <node id='$id' visible='true' lat='$lat' lon='$lon' />\n";         
         }
         $id;
     }

     sub seg_out {
         my $id = $i+1;
         $id;
     }

     sub way_out {
         my ( $segs, $tags ) = @_;
         my $id = $i--;
         print "  <way id='$id' visible='true'>\n";
         print "    <nd ref='$_' />\n" for @$segs;
         tags_out $tags;
         print "  </way>\n";
         $id;
     }
}


print <<'END';
</osm>
END

sub polyline_out {
    my ( $pts, $tags, $connect_last_seg ) = @_;

    my ( $first_node, $last_node, @segs );
    for my $pt (@$pts) {
        my $node = node_out @$pt;
        push @segs, seg_out $last_node, $node;
        $last_node = $node;
        $first_node ||= $last_node;
    }
    push @segs, seg_out $last_node, $first_node
      if $first_node && $connect_last_seg;
    way_out \@segs, $tags;
}

sub proc_obj {
    my ( $shp, $dbf, $type ) = @_;
    my $tags = { %default_tags, %$dbf };
    my $is_polygon = $type % 10 == 5;
    for ( 1 .. $shp->num_parts ) {
        polyline_out [ map( [ $_->X(), $_->Y() ], $shp->get_part($_) ) ], $tags,
          $is_polygon;
    }
 }

sub proc_shpf {
    my ($shpf) = @_;
    my $type = $shpf->shape_type;
    for ( 1 .. $shpf->shapes() ) {
        my $shp = $shpf->get_shp_record($_);
        my %dbf = $shpf->get_dbf_record($_);
        proc_obj $shp, \%dbf, $type;
    }
}

sub gk2geo {
  my $sr  = $_[0];
  my $sx  = $_[1];

  my $bm  = int($sr/1000000);
  my $y   = $sr-($bm*1000000+500000);
  my $si  = $sx/111120.6196;
  my $px  = $si+0.143885358*sin(2*$si*0.017453292)+0.00021079*sin(4*$si*0.017453292)+0.000000423*sin(6*$si*0.017453292);
  my $t   = (sin($px*0.017453292))/(cos($px*0.017453292));
  my $v   = sqrt(1+0.006719219*cos($px*0.017453292)*cos($px*0.017453292));
  my $ys  = ($y*$v)/6398786.85;
  my $lat = $px-$ys*$ys*57.29577*$t*$v*$v*(0.5-$ys*$ys*(4.97-3*$t*$t)/24);
  my $dl  = $ys*57.29577/cos($px*0.017453292) * (1-$ys*$ys/6*($v*$v+2*$t*$t-$ys*$ys*(0.6+1.1*$t*$t)*(0.6+1.1*$t*$t)));
  my $lon = $bm*3+$dl;

  my $potsd_a  = 6377397.155;
  my $wgs84_a  = 6378137.0;
  my $potsd_f  = 1/299.152812838;
  my $wgs84_f  = 1/298.257223563;

  my $potsd_es = 2*$potsd_f - $potsd_f*$potsd_f;

  my $potsd_dx = 606.0;
  my $potsd_dy = 23.0;
  my $potsd_dz = 413.0;
  my $pi = 3.141592654;
  my $latr = $lat/180*$pi;
  my $lonr = $lon/180*$pi;

  my $sa = sin($latr);
  my $ca = cos($latr);
  my $so = sin($lonr);
  my $co = cos($lonr);

  my $bda  = 1-$potsd_f;

  my $delta_a = $wgs84_a - $potsd_a;
  my $delta_f = $wgs84_f - $potsd_f;

  my $rn = $potsd_a / sqrt(1-$potsd_es*sin($latr)*sin($latr));
  my $rm = $potsd_a * ((1-$potsd_es)/sqrt(1-$potsd_es*sin($latr)*sin($latr)*1-$potsd_es*sin($latr)*sin($latr)*1-$potsd_es*sin($latr)*sin($latr)));

  my $ta = (-$potsd_dx*$sa*$co - $potsd_dy*$sa*$so)+$potsd_dz*$ca;
  my $tb = $delta_a*(($rn*$potsd_es*$sa*$ca)/$potsd_a);
  my $tc = $delta_f*($rm/$bda+$rn*$bda)*$sa*$ca;
  my $dlat = ($ta+$tb+$tc)/$rm;

  my $dlon = (-$potsd_dx*$so + $potsd_dy*$co)/($rn*$ca);

  my $wgs84lat = ($latr + $dlat)*180/$pi;
  my $wgs84lon = ($lonr + $dlon)*180/$pi;

  return( $wgs84lon, $wgs84lat);
}
