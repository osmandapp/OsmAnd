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

namespace UnitedPrototype\GoogleAnalytics;

use UnitedPrototype\GoogleAnalytics\Internals\Util;

use DateTime;

/**
 * You should serialize this object and store it in the user database to keep it
 * persistent for the same user permanently (similar to the "__umta" cookie of
 * the GA Javascript client).
 */
class Visitor {
	
	/**
	 * Unique user ID, will be part of the "__utma" cookie parameter
	 * 
	 * @see Internals\ParameterHolder::$__utma
	 * @var int
	 */
	protected $uniqueId;
	
	/**
	 * Time of the very first visit of this user, will be part of the "__utma"
	 * cookie parameter
	 * 
	 * @see Internals\ParameterHolder::$__utma
	 * @var DateTime
	 */
	protected $firstVisitTime;
	
	/**
	 * Time of the previous visit of this user, will be part of the "__utma"
	 * cookie parameter
	 * 
	 * @see Internals\ParameterHolder::$__utma
	 * @see addSession
	 * @var DateTime
	 */
	protected $previousVisitTime;
	
	/**
	 * Time of the current visit of this user, will be part of the "__utma"
	 * cookie parameter
	 * 
	 * @see Internals\ParameterHolder::$__utma
	 * @see addSession
	 * @var DateTime
	 */
	protected $currentVisitTime;
	
	/**
	 * Amount of total visits by this user, will be part of the "__utma"
	 * cookie parameter
	 * 
	 * @see Internals\ParameterHolder::$__utma
	 * @var int
	 */
	protected $visitCount;
	
	/**
	 * IP Address of the end user, e.g. "123.123.123.123", will be mapped to "utmip" parameter
	 * and "X-Forwarded-For" request header
	 * 
	 * @see Internals\ParameterHolder::$utmip
	 * @see Internals\Request\HttpRequest::$xForwardedFor
	 * @var string
	 */
	protected $ipAddress;
	
	/**
	 * User agent string of the end user, will be mapped to "User-Agent" request header
	 * 
	 * @see Internals\Request\HttpRequest::$userAgent
	 * @var string
	 */
	protected $userAgent;
	
	/**
	 * Locale string (country part optional), e.g. "de-DE", will be mapped to "utmul" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmul
	 * @var string
	 */
	protected $locale;
	
	/**
	 * Visitor's Flash version, e.g. "9.0 r28", will be maped to "utmfl" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmfl
	 * @var string
	 */
	protected $flashVersion;
	
	/**
	 * Visitor's Java support, will be mapped to "utmje" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmje
	 * @var bool
	 */
	protected $javaEnabled;
	
	/**
	 * Visitor's screen color depth, e.g. 32, will be mapped to "utmsc" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmsc
	 * @var string
	 */
	protected $screenColorDepth;
	
	/**
	 * Visitor's screen resolution, e.g. "1024x768", will be mapped to "utmsr" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmsr
	 * @var string
	 */
	protected $screenResolution;
	
	
	/**
	 * Creates a new visitor without any previous visit information.
	 */
	public function __construct() {
		// ga.js sets all three timestamps to now for new visitors, so we do the same
		$now = new DateTime();
		$this->setFirstVisitTime($now);
		$this->setPreviousVisitTime($now);
		$this->setCurrentVisitTime($now);
		
		$this->setVisitCount(1);
	}
	
	/**
	 * Will extract information for the "uniqueId", "firstVisitTime", "previousVisitTime",
	 * "currentVisitTime" and "visitCount" properties from the given "__utma" cookie
	 * value.
	 * 
	 * @see Internals\ParameterHolder::$__utma
	 * @see Internals\Request\Request::buildCookieParameters()
	 * @param string $value
	 * @return $this
	 */
	public function fromUtma($value) {
		$parts = explode('.', $value);
		if(count($parts) != 6) {
			Tracker::_raiseError('The given "__utma" cookie value is invalid.', __METHOD__);
			return $this;
		}
		
		$this->setUniqueId($parts[1]);
		$this->setFirstVisitTime(new DateTime('@' . $parts[2]));
		$this->setPreviousVisitTime(new DateTime('@' . $parts[3]));
		$this->setCurrentVisitTime(new DateTime('@' . $parts[4]));
		$this->setVisitCount($parts[5]);
		
		// Allow chaining
		return $this;
	}
	
	/**
	 * Will extract information for the "ipAddress", "userAgent" and "locale" properties
	 * from the given $_SERVER variable.
	 * 
	 * @param array $value
	 * @return $this
	 */
	public function fromServerVar(array $value) {
		if(!empty($value['REMOTE_ADDR'])) {
			$ip = null;
			foreach(array('X_FORWARDED_FOR', 'REMOTE_ADDR') as $key) {
				if(!empty($value[$key]) && !$ip) {
					$ips = explode(',', $value[$key]);
					$ip  = trim($ips[(count($ips) - 1)]);
					
					// Double-check if the address has a valid format
					if(!preg_match('/^[\d+]{1,3}\.[\d+]{1,3}\.[\d+]{1,3}\.[\d+]{1,3}$/i', $ip)) {
						$ip = null;
					}
					// Exclude private IP address ranges
					if(preg_match('#^(?:127\.0\.0\.1|10\.|192\.168\.|172\.(?:1[6-9]|2[0-9]|3[0-1])\.)#', $ip)) {
						$ip = null;
					}
				}
			}
			
			if($ip) {
				$this->setIpAddress($ip);
			}
		}
		
		if(!empty($value['HTTP_USER_AGENT'])) {
			$this->setUserAgent($value['HTTP_USER_AGENT']);
		}
		
		if(!empty($value['HTTP_ACCEPT_LANGUAGE'])) {
			$parsedLocales = array();
			if(preg_match_all('/(^|\s*,\s*)([a-zA-Z]{1,8}(-[a-zA-Z]{1,8})*)\s*(;\s*q\s*=\s*(1(\.0{0,3})?|0(\.[0-9]{0,3})))?/i', $value['HTTP_ACCEPT_LANGUAGE'], $matches)) {
				$matches[2] = array_map(function($part) { return str_replace('-', '_', $part); }, $matches[2]);
				$matches[5] = array_map(function($part) { return $part === '' ? 1 : $part; }, $matches[5]);
				$parsedLocales = array_combine($matches[2], $matches[5]);
				arsort($parsedLocales, SORT_NUMERIC);
				$parsedLocales = array_keys($parsedLocales);
			}
			
			if($parsedLocales) {
				$this->setLocale($parsedLocales[0]);
			}
		}
		
		// Allow chaining
		return $this;
	}
	
	/**
	 * Generates a hashed value from user-specific properties.
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/v4/Tracker.as#542
	 * @return int
	 */
	protected function generateHash() {
		// TODO: Emulate orginal Google Analytics client library generation more closely
		$string  = $this->userAgent;
		$string .= $this->screenResolution . $this->screenColorDepth;
		return Util::generateHash($string);
	}
	
	/**
	 * Generates a unique user ID from the current user-specific properties.
	 * 
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/v4/Tracker.as#563
	 * @return int
	 */
	protected function generateUniqueId() {
		// There seems to be an error in the gaforflash code, so we take the formula
		// from http://xahlee.org/js/google_analytics_tracker_2010-07-01_expanded.js line 711
		// instead ("&" instead of "*")
		return ((Util::generate32bitRandom() ^ $this->generateHash()) & 0x7fffffff);
	}
	
	/**
	 * @see generateUniqueId
	 * @param int $value
	 */
	public function setUniqueId($value) {
		if($value < 0 || $value > 0x7fffffff) {
			Tracker::_raiseError('Visitor unique ID has to be a 32-bit integer between 0 and ' . 0x7fffffff . '.', __METHOD__);
		}
		
		$this->uniqueId = (int)$value;
	}
	
	/**
	 * Will be generated on first call (if not set already) to include as much
	 * user-specific information as possible.
	 * 
	 * @return int
	 */
	public function getUniqueId() {
		if($this->uniqueId === null) {
			$this->uniqueId = $this->generateUniqueId();
		}
		return $this->uniqueId;
	}
	
	/**
	 * Updates the "previousVisitTime", "currentVisitTime" and "visitCount"
	 * fields based on the given session object.
	 * 
	 * @param Session $session
	 */
	public function addSession(Session $session) {
		$startTime = $session->getStartTime();
		if($startTime != $this->currentVisitTime) {
			$this->previousVisitTime = $this->currentVisitTime;
			$this->currentVisitTime  = $startTime;
			++$this->visitCount;
		}
	}
	
	/**
	 * @param DateTime $value
	 */
	public function setFirstVisitTime(DateTime $value) {
		$this->firstVisitTime = $value;
	}
	
	/**
	 * @return DateTime
	 */
	public function getFirstVisitTime() {
		return $this->firstVisitTime;
	}
	
	/**
	 * @param DateTime $value
	 */
	public function setPreviousVisitTime(DateTime $value) {
		$this->previousVisitTime = $value;
	}
	
	/**
	 * @return DateTime
	 */
	public function getPreviousVisitTime() {
		return $this->previousVisitTime;
	}
	
	/**
	 * @param DateTime $value
	 */
	public function setCurrentVisitTime(DateTime $value) {
		$this->currentVisitTime = $value;
	}
	
	/**
	 * @return DateTime
	 */
	public function getCurrentVisitTime() {
		return $this->currentVisitTime;
	}
	
	/**
	 * @param int $value
	 */
	public function setVisitCount($value) {
		$this->visitCount = (int)$value;
	}
	
	/**
	 * @return int
	 */
	public function getVisitCount() {
		return $this->visitCount;
	}
	
	/**
	 * @param string $value
	 */
	public function setIpAddress($value) {
		$this->ipAddress = $value;
	}
	
	/**
	 * @return string
	 */
	public function getIpAddress() {
		return $this->ipAddress;
	}
	
	/**
	 * @param string $value
	 */
	public function setUserAgent($value) {
		$this->userAgent = $value;
	}
	
	/**
	 * @return string
	 */
	public function getUserAgent() {
		return $this->userAgent;
	}
	
	/**
	 * @param string $value
	 */
	public function setLocale($value) {
		$this->locale = $value;
	}
	
	/**
	 * @return string
	 */
	public function getLocale() {
		return $this->locale;
	}
	
	/**
	 * @param string $value
	 */
	public function setFlashVersion($value) {
		$this->flashVersion = $value;
	}
	
	/**
	 * @return string
	 */
	public function getFlashVersion() {
		return $this->flashVersion;
	}
	
	/**
	 * @param bool $value
	 */
	public function setJavaEnabled($value) {
		$this->javaEnabled = (bool)$value;
	}
	
	/**
	 * @return bool
	 */
	public function getJavaEnabled() {
		return $this->javaEnabled;
	}
	
	/**
	 * @param int $value
	 */
	public function setScreenColorDepth($value) {
		$this->screenColorDepth = (int)$value;
	}
	
	/**
	 * @return string
	 */
	public function getScreenColorDepth() {
		return $this->screenColorDepth;
	}
	
	/**
	 * @param string $value
	 */
	public function setScreenResolution($value) {
		$this->screenResolution = $value;
	}
	
	/**
	 * @return string
	 */
	public function getScreenResolution() {
		return $this->screenResolution;
	}
	
}

?>