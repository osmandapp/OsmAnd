// **********************************************************************
//
// <copyright>
//
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************

package com.jwetherell.openmap.common;

public class Ellipsoid {

    /** "Airy" */
    public final static Ellipsoid AIRY = new Ellipsoid("Airy", 6377563.0d, 0.00667054d);
    /** "Australian National" */
    public final static Ellipsoid AUSTRALIAN_NATIONAL = new Ellipsoid("Australian National", 6378160.0d, 0.006694542d);
    /** "Bessel 1841" */
    public final static Ellipsoid BESSEL_1841 = new Ellipsoid("Bessel 1841", 6377397.0d, 0.006674372d);
    /** "Bessel 1841 (Nambia) " */
    public final static Ellipsoid BESSEL_1841_NAMIBIA = new Ellipsoid("Bessel 1841 Namibia", 6377484.0d, 0.006674372d);
    /** "Clarke 1866" */
    public final static Ellipsoid CLARKE_1866 = new Ellipsoid("Clarke 1866", 6378206.0d, 0.006768658d);
    /** "Clarke 1880" */
    public final static Ellipsoid CLARKE_1880 = new Ellipsoid("Clarke 1880", 6378249.0d, 0.006803511d);
    /** "Everest" */
    public final static Ellipsoid EVEREST = new Ellipsoid("Everest", 6377276.0d, 0.006637847d);
    /** "Fischer 1960 (Mercury) " */
    public final static Ellipsoid FISHER_1960_MERCURY = new Ellipsoid("Fisher 1960 Mercury", 6378166.0d, 0.006693422d);
    /** "Fischer 1968" */
    public final static Ellipsoid FISHER_1968 = new Ellipsoid("Fisher 1968", 6378150.0d, 0.006693422d);
    /** "GRS 1967" */
    public final static Ellipsoid GRS_1967 = new Ellipsoid("GRS 1967", 6378160.0d, 0.006694605d);
    /** "GRS 1980" */
    public final static Ellipsoid GRS_1980 = new Ellipsoid("GRS 1980", 6378137.0d, 0.081819191d, 0.00669438d, 6356752.3141d);
    /** "Helmert 1906" */
    public final static Ellipsoid HELMERT_1906 = new Ellipsoid("Helmert 1906", 6378200.0d, 0.006693422d);
    /** "Hough" */
    public final static Ellipsoid HOUGH = new Ellipsoid("Hough", 6378270.0d, 0.00672267d);
    /** "International" */
    public final static Ellipsoid INTERNATIONAL = new Ellipsoid("International", 6378388.0d, 0.08199189, 0.00672267d, 6356911.946d);
    /** "Krassovsky" */
    public final static Ellipsoid KRASSOVSKY = new Ellipsoid("Krassovsky", 6378245.0d, 0.006693422d);
    /** "Modified Airy" */
    public final static Ellipsoid MODIFIED_AIRY = new Ellipsoid("Modified Airy", 6377340.0d, 0.00667054d);
    /** "Modified Everest" */
    public final static Ellipsoid MODIFIED_EVEREST = new Ellipsoid("Modified Everest", 6377304.0d, 0.006637847d);
    /** "Modified Fischer 1960" */
    public final static Ellipsoid MODIFIED_FISCHER_1960 = new Ellipsoid("Modified Fischer", 6378155.0d, 0.006693422d);
    /** "South American 1969" */
    public final static Ellipsoid SOUTH_AMERICAN_1969 = new Ellipsoid("South American 1969", 6378160.0d, 0.006694542d);
    /** "WGS 60" */
    public final static Ellipsoid WGS_60 = new Ellipsoid("WSG 60", 6378165.0d, 0.006693422d);
    /** "WGS 66" */
    public final static Ellipsoid WGS_66 = new Ellipsoid("WGS 66", 6378145.0d, 0.006694542d);
    /** "WGS-72" */
    public final static Ellipsoid WGS_72 = new Ellipsoid("WGS 72", 6378135.0d, 0.006694318d);
    /** "WGS-84" */
    public final static Ellipsoid WGS_84 = new Ellipsoid("WGS 84", 6378137.0d, 0.081819191d, 0.00669438d, 6356752.3142d);

    /**
     * The display name for this ellipsoid.
     */
    public final String name;

    /**
     * The equitorial radius for this ellipsoid.
     */
    public final double radius;

    /**
     * The polar radius for this ellipsoid.
     */
    public final double polarRadius;

    /**
     * The ellipsoid's eccentricity.
     */
    public final double ecc;

    /**
     * The square of this ellipsoid's eccentricity.
     */
    public final double eccsq;

    /**
     * Constructs a new Ellipsoid instance.
     * 
     * @param radius
     *            The earth radius for this ellipsoid.
     * @param eccsq
     *            The square of the eccentricity for this ellipsoid.
     */
    public Ellipsoid(String name, double radius, double eccsq) {
        this(name, radius, eccsq, Double.NaN, Double.NaN);
    }

    /**
     * Constructs a new Ellipsoid instance.
     * 
     * @param name
     *            The name of the ellipsoid.
     * @param equitorialRadius
     *            The earth equitorial radius for this ellipsoid.
     * @param ecc
     *            The eccentricity for this ellipsoid.
     * @param eccsq
     *            The square of the eccentricity for this ellipsoid.
     * @param polarRadius
     *            The earth polar radius for this ellipsoid.
     */
    public Ellipsoid(String name, double equitorialRadius, double ecc, double eccsq, double polarRadius) {
        this.name = name;
        this.radius = equitorialRadius;
        this.ecc = ecc;
        this.eccsq = eccsq;
        this.polarRadius = polarRadius;
    }

    /**
     * Returns an array of all available ellipsoids in alphabetical order by
     * name.
     * 
     * @return An Ellipsoid[] array containing all the available ellipsoids
     */
    public static Ellipsoid[] getAllEllipsoids() {
        Ellipsoid[] all = { AIRY, AUSTRALIAN_NATIONAL, BESSEL_1841, BESSEL_1841_NAMIBIA, CLARKE_1866, CLARKE_1880, EVEREST, FISHER_1960_MERCURY, FISHER_1968,
                GRS_1967, GRS_1980, HELMERT_1906, HOUGH, INTERNATIONAL, KRASSOVSKY, MODIFIED_AIRY, MODIFIED_EVEREST, MODIFIED_FISCHER_1960,
                SOUTH_AMERICAN_1969, WGS_60, WGS_66, WGS_72, WGS_84 };

        return all;
    }

    /**
     * Given the name of an Ellipsoid, find the object for it out of the
     * possible selections. Returns null if the Ellipsoid isn't found.
     * 
     * @param name
     * @return Ellipsoid
     */
    public static Ellipsoid getByName(String name) {
        Ellipsoid[] all = getAllEllipsoids();
        if (name != null && name.length() > 0) {
            name = name.replace('_', ' ');

            for (int i = 0; i < all.length; i++) {
                if (name.equalsIgnoreCase(all[i].name)) {
                    return all[i];
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Ellipsoid[name=" + name + ", radius=" + radius + ", eccsq=" + eccsq + "]";
    }
}