<?xml version="1.0"?>

<!--
	Copyright © Hugo Haas <hugoh@hugoh.net>
	GNU General Public License, version 2 (GPL-2.0)
	http://opensource.org/licenses/gpl-2.0.php
-->

<xsl:stylesheet xmlns:atom="http://www.w3.org/2005/Atom"
				xmlns:ge="http://earth.google.com/kml/2.2"
				xmlns:gx="http://www.google.com/kml/ext/2.2"
				xmlns:kml="http://www.opengis.net/kml/2.2"

				xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				version="1.0">

	<xsl:output indent="yes"
				method="xml"/>

	<xsl:template match="/">
		<gpx creator="kml2gpx.xslt"
			 version="1.1"
			 xmlns="http://www.topografix.com/GPX/1/1">
			<metadata>
				<name><xsl:value-of select="kml:kml/kml:Document/kml:name"/></name>
				<author>
					<name>
						<xsl:value-of select="kml:kml/kml:Document/atom:author/atom:author"/>
					</name>
				</author>
			</metadata>
			<xsl:for-each select="//ge:Placemark">
				<xsl:variable name="lonlat" select="ge:Point/ge:coordinates"/>
				<xsl:variable name="lon" select="substring-before($lonlat,' ')"/>
				<xsl:variable name="latele" select="substring-after($lonlat,' ')"/>
				<xsl:variable name="lat">
					<xsl:choose>
						<xsl:when test="contains($latele,' ')">
							<xsl:value-of select="substring-before($latele,' ')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$latele"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="foldername" select="../ge:name"/>

				<xsl:if test="$lon">
					<wpt lon="{$lon}" lat="{$lat}">
						<xsl:choose>
							<xsl:when test="contains($latele,',')">
								<ele><xsl:value-of select="substring-after($latele,',')"/></ele>
							</xsl:when>
						</xsl:choose>
						<type>
							<xsl:value-of select="$foldername"/>
						</type>
						<name>
							<xsl:value-of select="ge:name"/>
						</name>
						<xsl:if test="ge:description">
							<desc>
								<xsl:value-of select="ge:description"/>
							</desc>
						</xsl:if>
					</wpt>
				</xsl:if>
			</xsl:for-each>

			<xsl:for-each select="//ge:Placemark">
				<xsl:variable name="lonlat" select="ge:Point/ge:coordinates"/>
				<xsl:variable name="lon" select="substring-before($lonlat,',')"/>
				<xsl:variable name="latele" select="substring-after($lonlat,',')"/>
				<xsl:variable name="lat">
					<xsl:choose>
						<xsl:when test="contains($latele,' ')">
							<xsl:value-of select="substring-before($latele,',')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$latele"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="foldername" select="../ge:name"/>

				<xsl:if test="$lon">
					<wpt lon="{$lon}" lat="{$lat}">
						<xsl:choose>
							<xsl:when test="contains($latele,',')">
								<ele><xsl:value-of select="substring-after($latele,',')"/></ele>
							</xsl:when>
						</xsl:choose>
						<type>
							<xsl:value-of select="$foldername"/>
						</type>
						<name>
							<xsl:value-of select="ge:name"/>
						</name>
						<xsl:if test="ge:description">
							<desc>
								<xsl:value-of select="ge:description"/>
							</desc>
						</xsl:if>
					</wpt>
				</xsl:if>
			</xsl:for-each>
			<xsl:for-each select="//kml:Placemark">
				<xsl:if test="kml:Point">
					<xsl:variable name="lonlat" select="kml:Point/kml:coordinates"/>
					<xsl:variable name="lon" select="substring-before($lonlat,',')"/>
					<xsl:variable name="latele" select="substring-after($lonlat,',')"/>
					<xsl:variable name="lat">
						<xsl:choose>
							<xsl:when test="contains($latele,',')">
								<xsl:value-of select="substring-before($latele,',')"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$latele"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:variable name="foldername" select="../kml:name"/>

					<xsl:if test="$lon">
						<wpt lon="{$lon}" lat="{$lat}">
							<xsl:choose>
								<xsl:when test="contains($latele,',')">
									<ele><xsl:value-of select="substring-after($latele,',')"/></ele>
								</xsl:when>
							</xsl:choose>
							<type>
								<xsl:value-of select="$foldername"/>
							</type>
							<name>
								<xsl:value-of select="kml:name"/>
							</name>
							<xsl:if test="kml:description">
								<desc>
									<xsl:value-of select="kml:description"/>
								</desc>
							</xsl:if>
						</wpt>
					</xsl:if>
				</xsl:if>

				<xsl:if test="kml:LineString/kml:coordinates">
					<trk>
						<trkseg>
							<csvattributes><xsl:value-of select="kml:LineString/kml:coordinates"/></csvattributes>
						</trkseg>
					</trk>
				</xsl:if>

				<xsl:if test="kml:MultiGeometry/kml:LineString/kml:coordinates">
					<trk>
						<trkseg>
							<csvattributes><xsl:value-of select="kml:MultiGeometry/kml:LineString/kml:coordinates"/></csvattributes>
						</trkseg>
					</trk>
				</xsl:if>

			</xsl:for-each>
			<xsl:for-each select="//gx:Track">
				<trk>
					<trkseg>
						<xsl:for-each select="gx:coord">
							<xsl:variable name="i" select="position()"/>
							<xsl:variable name="lonlat" select="."/>
							<xsl:variable name="lon" select="substring-before($lonlat,' ')"/>
							<xsl:variable name="latele" select="substring-after($lonlat,' ')"/>
							<xsl:variable name="lat">
								<xsl:choose>
									<xsl:when test="contains($latele,' ')">
										<xsl:value-of select="substring-before($latele,' ')"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$latele"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:if test="$lon">
								<trkpt lon="{$lon}" lat="{$lat}">
									<xsl:choose>
										<xsl:when test="contains($latele,' ')">
											<ele><xsl:value-of select="substring-after($latele,' ')"/></ele>
										</xsl:when>
									</xsl:choose>
									<xsl:variable name="ts" select="../kml:when[$i]"/>
									<xsl:if test="$ts">
										<time><xsl:value-of select="$ts"/></time>
									</xsl:if>
								</trkpt>
							</xsl:if>
						</xsl:for-each>
					</trkseg>
				</trk>
			</xsl:for-each>
		</gpx>
	</xsl:template>

	<xsl:template match="gx:Track">
	</xsl:template>

</xsl:stylesheet>
