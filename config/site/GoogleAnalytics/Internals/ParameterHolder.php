<?php

/**
 * Generic Server-Side Google Analytics PHP Client
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 * 
 * Google Analytics is a registered trademark of Google Inc.
 * 
 * @link      http://code.google.com/p/php-ga
 * 
 * @license   http://www.gnu.org/licenses/lgpl.html
 * @author    Thomas Bachem <tb@unitedprototype.com>
 * @copyright Copyright (c) 2010 United Prototype GmbH (http://unitedprototype.com)
 */

namespace UnitedPrototype\GoogleAnalytics\Internals;

use UnitedPrototype\GoogleAnalytics\Tracker;

/**
 * This simple class is mainly meant to be a well-documented overview of all
 * possible GA tracking parameters.
 * 
 * @link http://code.google.com/apis/analytics/docs/tracking/gaTrackingTroubleshooting.html#gifParameters
 */
class ParameterHolder {	
	
	// - - - - - - - - - - - - - - - - - General parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * Google Analytics client version, e.g. "4.7.2"
	 * @var string
	 */
	public $utmwv = Tracker::VERSION;
	
	/**
	 * Google Analytics account ID, e.g. "UA-1234567-8"
	 * @var string
	 */
	public $utmac;
	
	/**
	 * Host Name, e.g. "www.example.com"
	 * @var string
	 */
	public $utmhn;
	
	/**
	 * Indicates the type of request, which is one of null (for page), "event",
	 * "tran", "item", "social", "var" (deprecated) or "error" (used by ga.js
	 * for internal client error logging).
	 * @var string
	 */
	public $utmt;
	
	/**
	 * Contains the amount of requests done in this session. Added in ga.js v4.9.2.
	 * @var int
	 */
	public $utms;
	
	/**
	 * Unique ID (random number) generated for each GIF request
	 * @var int
	 */
	public $utmn;
	
	/**
	 * Contains all cookie values, see below
	 * @var string
	 */
	public $utmcc;
	
	/**
	 * Extensible Parameter, used for events and custom variables
	 * @var string
	 */
	public $utme;
	
	/**
	 * Event "non-interaction" parameter. By default, the event hit will impact a visitor's bounce rate.
	 * By setting this parameter to 1, this event hit will not be used in bounce rate calculations.
	 * @link http://code.google.com/apis/analytics/docs/gaJS/gaJSApiEventTracking.html
	 * @var int
	 */
	public $utmni;
	
	/**
	 * Whether to anonymize IP addresses within Google Analytics by stripping
	 * the last IP address block, either null or 1
	 * @var int
	 */
	public $aip;
	
	/**
	 * Used for GA-internal statistical client function usage and error tracking,
	 * not implemented in php-ga as of now, but here for documentation completeness.
	 * @link http://glucik.blogspot.com/2011/02/utmu-google-analytics-request-parameter.html
	 * @var string
	 */
	public $utmu;
	
	
	// - - - - - - - - - - - - - - - - - Page parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * Page request URI, e.g. "/path/page.html"
	 * @var string
	 */
	public $utmp;
	
	/**
	 * Page title
	 * @var string
	 */
	public $utmdt;
	
	/**
	 * Charset encoding (e.g. "UTF-8") or "-" as default
	 * @var string
	 */
	public $utmcs = '-';
	
	/**
	 * Referer URL, e.g. "http://www.example.com/path/page.html", "-" as default
	 * or "0" for internal referers
	 * @var string
	 */
	public $utmr = '-';
	
	
	// - - - - - - - - - - - - - - - - - Visitor parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * IP Address of the end user, e.g. "123.123.123.123", found in GA for Mobile examples,
	 * but sadly seems to be ignored in normal GA use
	 * 
	 * @link http://github.com/mptre/php-ga/blob/master/ga.php
	 * @var string
	 */
	public $utmip;
	
	/**
	 * Visitor's locale string (all lower-case, country part optional), e.g. "de-de"
	 * @var string
	 */
	public $utmul;
	
	/**
	 * Visitor's Flash version, e.g. "9.0 r28" or "-" as default
	 * @var string
	 */
	public $utmfl = '-';
	
	/**
	 * Visitor's Java support, either 0 or 1 or "-" as default
	 * @var int|string
	 */
	public $utmje = '-';
	
	/**
	 * Visitor's screen color depth, e.g. "32-bit"
	 * @var string
	 */
	public $utmsc;
	
	/**
	 * Visitor's screen resolution, e.g. "1024x768"
	 * @var string
	 */
	public $utmsr;
	
    /**
	 * Visitor tracking cookie parameter.
	 * 
	 * This cookie is typically written to the browser upon the first visit to your site from that web browser.
	 * If the cookie has been deleted by the browser operator, and the browser subsequently visits your site,
	 * a new __utma cookie is written with a different unique ID.
	 * 
	 * This cookie is used to determine unique visitors to your site and it is updated with each page view.
	 * Additionally, this cookie is provided with a unique ID that Google Analytics uses to ensure both the
	 * validity and accessibility of the cookie as an extra security measure.
	 * 
	 * Expiration:
	 * 2 years from set/update.
	 * 
	 * Format:
	 * __utma=<domainHash>.<uniqueId>.<firstTime>.<lastTime>.<currentTime>.<sessionCount>
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/UTMA.as
	 * @var int
	 */
	public $__utma;
	
	
	// - - - - - - - - - - - - - - - - - Session parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * Hit id for revenue per page tracking for AdSense, a random per-session ID
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/core/DocumentInfo.as#117
	 * @var int
	 */
	public $utmhid;
	
    /**
	 * Session timeout cookie parameter.
	 * Will never be sent with requests, but stays here for documentation completeness.
	 * 
	 * This cookie is used to establish and continue a user session with your site.
	 * When a user views a page on your site, the Google Analytics code attempts to update this cookie.
	 * If it does not find the cookie, a new one is written and a new session is established.
	 * 
	 * Each time a user visits a different page on your site, this cookie is updated to expire in 30 minutes,
	 * thus continuing a single session for as long as user activity continues within 30-minute intervals.
	 * 
	 * This cookie expires when a user pauses on a page on your site for longer than 30 minutes.
	 * You can modify the default length of a user session with the setSessionTimeout() method.
	 * 
	 * Expiration:
	 * 30 minutes from set/update.
	 * 
	 * Format:
	 * __utmb=<domainHash>.<trackCount>.<token>.<lastTime>
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/UTMB.as
	 * @var string
	 */
	public $__utmb;
	
    /**
	 * Session tracking cookie parameter.
	 * Will never be sent with requests, but stays here for documentation completeness.
	 * 
	 * This cookie operates in conjunction with the __utmb cookie to determine whether or not
	 * to establish a new session for the user.
	 * In particular, this cookie is not provided with an expiration date,
	 * so it expires when the user exits the browser.
	 * 
	 * Should a user visit your site, exit the browser and then return to your website within 30 minutes,
	 * the absence of the __utmc cookie indicates that a new session needs to be established,
	 * despite the fact that the __utmb cookie has not yet expired.
	 * 
	 * Expiration:
	 * Not set.
	 * 
	 * Format:
	 * __utmc=<domainHash>
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/UTMC.as
	 * @var string
	 */
	public $__utmc;
	
	
	// - - - - - - - - - - - - - - - - - E-Commerce parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * Product Code. This is the sku code for a given product, e.g. "989898ajssi"
	 * @var string
	 */
	public $utmipc;
	
	/**
	 * Product Name, e.g. "T-Shirt"
	 * @var string
	 */
	public $utmipn;
	
	/**
	 * Unit Price. Value is set to numbers only, e.g. 19.95
	 * @var float
	 */
	public $utmipr;
	
	/**
	 * Unit Quantity, e.g. 4
	 * @var int
	 */
	public $utmiqt;
	
	/**
	 * Variations on an item, e.g. "white", "black", "green" etc.
	 * @var string
	 */
	public $utmiva;
	
	/**
	 * Order ID, e.g. "a2343898"
	 * @var string
	 */
	public $utmtid;
	
	/**
	 * Affiliation
	 * @var string
	 */
	public $utmtst;
	
	/**
	 * Total Cost, e.g. 20.00
	 * @var float
	 */
	public $utmtto;
	
	/**
	 * Tax Cost, e.g. 4.23
	 * @var float
	 */
	public $utmttx;
	
	/**
	 * Shipping Cost, e.g. 3.95
	 * @var float
	 */
	public $utmtsp;
	
	/**
	 * Billing City, e.g. "Cologne"
	 * @var string
	 */
	public $utmtci;
	
	/**
	 * Billing Region, e.g. "North Rhine-Westphalia"
	 * @var string
	 */
	public $utmtrg;
	
	/**
	 * Billing Country, e.g. "Germany"
	 * @var string
	 */
	public $utmtco;
	
	
	// - - - - - - - - - - - - - - - - - Campaign parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * Starts a new campaign session. Either utmcn or utmcr is present on any given request,
	 * but never both at the same time. Changes the campaign tracking data; but does not start
	 * a new session. Either 1 or not set.
	 * 
	 * Found in gaforflash but not in ga.js, so we do not use it, but it will stay here for
	 * documentation completeness.
	 * 
	 * @deprecated
	 * @var int
	 */
	public $utmcn;
	
	/**
	 * Indicates a repeat campaign visit. This is set when any subsequent clicks occur on the
	 * same link. Either utmcn or utmcr is present on any given request, but never both at the
	 * same time. Either 1 or not set.
	 * 
	 * Found in gaforflash but not in ga.js, so we do not use it, but it will stay here for
	 * documentation completeness.
	 * 
	 * @deprecated
	 * @var int
	 */
	public $utmcr;
	
	/**
	 * Campaign ID, a.k.a. "utm_id" query parameter for ga.js
	 * @var string
	 */
	public $utmcid;
	
	/**
	 * Source, a.k.a. "utm_source" query parameter for ga.js
	 * @var string
	 */
	public $utmcsr;
	
	/**
	 * Google AdWords Click ID, a.k.a. "gclid" query parameter for ga.js
	 * @var string
	 */
	public $utmgclid;
	
	/**
	 * Not known for sure, but expected to be a DoubleClick Ad Click ID.
	 * @var string
	 */
	public $utmdclid;
	
	/**
	 * Name, a.k.a. "utm_campaign" query parameter for ga.js
	 * @var string
	 */
	public $utmccn;
	
	/**
	 * Medium, a.k.a. "utm_medium" query parameter for ga.js
	 * @var string
	 */
	public $utmcmd;
	
	/**
	 * Terms/Keywords, a.k.a. "utm_term" query parameter for ga.js
	 * @var string
	 */
	public $utmctr;
	
	/**
	 * Ad Content Description, a.k.a. "utm_content" query parameter for ga.js
	 * @var string
	 */
	public $utmcct;
	
	/**
	 * Unknown so far. Found in ga.js.
	 * @var int
	 */
	public $utmcvr;
	
    /**
	 * Campaign tracking cookie parameter.
	 * 
	 * This cookie stores the type of referral used by the visitor to reach your site,
	 * whether via a direct method, a referring link, a website search, or a campaign such as an ad or an email link.
	 * 
	 * It is used to calculate search engine traffic, ad campaigns and page navigation within your own site.
	 * The cookie is updated with each page view to your site.
	 * 
	 * Expiration:
	 * 6 months from set/update.
	 * 
	 * Format:
	 * __utmz=<domainHash>.<campaignCreation>.<campaignSessions>.<responseCount>.<campaignTracking>
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/UTMZ.as
	 * @var string
	 */
	public $__utmz;
	
	
	// - - - - - - - - - - - - - - - - - Social Tracking parameters - - - - - - - - - - - - - - - - -
	
	/**
	 * The network on which the action occurs (e.g. Facebook, Twitter).
	 * @var string
	 */
	public $utmsn;
	
	/**
	 * The type of action that happens (e.g. Like, Send, Tweet).
	 * @var string
	 */
	public $utmsa;
	
	/**
	 * The page URL from which the action occurred.
	 * @var string
	 */
	public $utmsid;
	
	
	// - - - - - - - - - - - - - - - - - Google Website Optimizer (GWO) parameters - - - - - - - - - - - - - - - - -
	
	// TODO: Implementation needed
    /**
     * Website Optimizer cookie parameter.
	 * 
	 * This cookie is used by Website Optimizer and only set when Website Optimizer is used in combination
	 * with GA. See the Google Website Optimizer Help Center for details.
     *
     * Expiration:
     * 2 years from set/update.
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/UTMX.as
	 * @var string
	 */
	public $__utmx;
	
	
	// - - - - - - - - - - - - - - - - - Custom Variables parameters (deprecated) - - - - - - - - - - - - - - - - -
	
	// TODO: Implementation needed?
	/**
	 * Deprecated custom variables cookie parameter.
	 * 
	 * This cookie parameter is no longer relevant as of migration from setVar() to
	 * setCustomVar() and hence not supported by this library, but will stay here for
	 * documentation completeness.
	 * 
	 * The __utmv cookie passes the information provided via the setVar() method,
	 * which you use to create a custom user segment.
	 * 
	 * Expiration:
	 * 2 years from set/update.
	 * 
	 * Format:
	 * __utmv=<domainHash>.<value>
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/UTMV.as
	 * @deprecated
	 * @var string
	 */
	public $__utmv;
	
	
	/**
	 * Converts this parameter holder to a pure PHP array, filtering out all properties
	 * prefixed with an underscore ("_").
	 * 
	 * @return array
	 */
	public function toArray() {
		$array = array();
		foreach($this as $property => $value) {
			if($property[0] != '_') {
				$array[$property] = $value;
			}
		}
		return $array;
	}
	
}

?>