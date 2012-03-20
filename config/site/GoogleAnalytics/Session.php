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
 * You should serialize this object and store it in the user session to keep it
 * persistent between requests (similar to the "__umtb" cookie of
 * the GA Javascript client).
 */
class Session {
	
	/**
	 * A unique per-session ID, will be mapped to "utmhid" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmhid
	 * @var int
	 */
	protected $sessionId;
	
	/**
	 * The amount of pageviews that were tracked within this session so far,
	 * will be part of the "__utmb" cookie parameter.
	 * 
	 * Will get incremented automatically upon each request.
	 * 
	 * @see Internals\ParameterHolder::$__utmb
	 * @see Internals\Request\Request::buildHttpRequest()
	 * @var int
	 */
	protected $trackCount;
	
	/**
	 * Timestamp of the start of this new session, will be part of the "__utmb"
	 * cookie parameter
	 * 
	 * @see Internals\ParameterHolder::$__utmb
	 * @var DateTime
	 */
	protected $startTime;
	
	
	public function __construct() {
		$this->setSessionId($this->generateSessionId());
		$this->setTrackCount(0);
		$this->setStartTime(new DateTime());
	}
	
	/**
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/core/DocumentInfo.as#52
	 * @return int
	 */
	protected function generateSessionId() {
		// TODO: Integrate AdSense support
		return Util::generate32bitRandom();
	}
	
	/**
	 * @return int
	 */
	public function getSessionId() {
		return $this->sessionId;
	}
	
	/**
	 * @param int $sessionId
	 */
	public function setSessionId($sessionId) {
		$this->sessionId = $sessionId;
	}
	
	/**
	 * @return int
	 */
	public function getTrackCount() {
		return $this->trackCount;
	}
	
	/**
	 * @param int $trackCount
	 */
	public function setTrackCount($trackCount) {
		$this->trackCount = (int)$trackCount;
	}
	
	/**
	 * @param int $byAmount
	 */
	public function increaseTrackCount($byAmount = 1) {
		$this->trackCount += $byAmount;
	}
	
	/**
	 * @return DateTime
	 */
	public function getStartTime() {
		return $this->startTime;
	}
	
	/**
	 * @param DateTime $startTime
	 */
	public function setStartTime(DateTime $startTime) {
		$this->startTime = $startTime;
	}

}

?>