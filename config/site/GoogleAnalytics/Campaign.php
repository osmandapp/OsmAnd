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
 * You should serialize this object and store it in e.g. the user database to keep it
 * persistent for the same user permanently (similar to the "__umtz" cookie of
 * the GA Javascript client).
 */
class Campaign {
	
	/**
	 * See self::TYPE_* constants, will be mapped to "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $type;
	
	/**
	 * Time of the creation of this campaign, will be mapped to "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var DateTime
	 */
	protected $creationTime;
	
	/**
	 * Response Count, will be mapped to "__utmz" parameter.
	 * 
	 * Is also used to determine whether the campaign is new or repeated,
	 * which will be mapped to "utmcn" and "utmcr" parameters.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @see Internals\ParameterHolder::$utmcn
	 * @see Internals\ParameterHolder::$utmcr
	 * @var int
	 */
	protected $responseCount = 0;
	
	/**
	 * Campaign ID, a.k.a. "utm_id" query parameter for ga.js
	 * Will be mapped to "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var int
	 */
	protected $id;
	
	/**
	 * Source, a.k.a. "utm_source" query parameter for ga.js.
	 * Will be mapped to "utmcsr" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $source;
	
	/**
	 * Google AdWords Click ID, a.k.a. "gclid" query parameter for ga.js.
	 * Will be mapped to "utmgclid" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $gClickId;
	
	/**
	 * DoubleClick (?) Click ID. Will be mapped to "utmdclid" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $dClickId;
	
	/**
	 * Name, a.k.a. "utm_campaign" query parameter for ga.js.
	 * Will be mapped to "utmccn" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $name;
	
	/**
	 * Medium, a.k.a. "utm_medium" query parameter for ga.js.
	 * Will be mapped to "utmcmd" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $medium;
	
	/**
	 * Terms/Keywords, a.k.a. "utm_term" query parameter for ga.js.
	 * Will be mapped to "utmctr" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $term;
	
	/**
	 * Ad Content Description, a.k.a. "utm_content" query parameter for ga.js.
	 * Will be mapped to "utmcct" key in "__utmz" parameter.
	 * 
	 * @see Internals\ParameterHolder::$__utmz
	 * @var string
	 */
	protected $content;
	
	
	/**
	 * @const string
	 */
	const TYPE_DIRECT = 'direct';
	/**
	 * @const string
	 */
	const TYPE_ORGANIC = 'organic';
	/**
	 * @const string
	 */
	const TYPE_REFERRAL = 'referral';
	
	
	/**
	 * @see createFromReferrer
	 * @param string $type See TYPE_* constants
	 */
	public function __construct($type) {
		if(!in_array($type, array(self::TYPE_DIRECT, self::TYPE_ORGANIC, self::TYPE_REFERRAL))) {
			Tracker::_raiseError('Campaign type has to be one of the Campaign::TYPE_* constant values.', __METHOD__);
		}
		
		$this->type = $type;
		
		switch($type) {
			// See http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/campaign/CampaignManager.as#375
			case self::TYPE_DIRECT:
				$this->name   = '(direct)';
				$this->source = '(direct)';
				$this->medium = '(none)';
				break;
			// See http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/campaign/CampaignManager.as#340
			case self::TYPE_REFERRAL:
				$this->name   = '(referral)';
				$this->medium = 'referral';
				break;
			// See http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/campaign/CampaignManager.as#280
			case self::TYPE_ORGANIC:
				$this->name   = '(organic)';
				$this->medium = 'organic';
				break;
		}
		
		$this->creationTime = new DateTime();
	}
	
	/**
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/campaign/CampaignManager.as#333
	 * @param string $url
	 * @return \UnitedPrototype\GoogleAnalytics\Campaign
	 */
	public static function createFromReferrer($url) {
		$instance = new static(self::TYPE_REFERRAL);
		$urlInfo = parse_url($url);
		$instance->source  = $urlInfo['host'];
		$instance->content = $urlInfo['path'];
		
		return $instance;
	}
	
	/**
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/campaign/CampaignTracker.as#153
	 */
	public function validate() {
		// NOTE: gaforflash states that id and gClickId must also be specified,
		// but that doesn't seem to be correct
		if(!$this->source) {
			Tracker::_raiseError('Campaigns need to have at least the "source" attribute defined.', __METHOD__);
		}
	}
	
	/**
	 * @param string $type
	 */
	public function setType($type) {
		$this->type = $type;
	}
	
	/**
	 * @return string
	 */
	public function getType() {
		return $this->type;
	}
	
	/**
	 * @param DateTime $creationTime
	 */
	public function setCreationTime(DateTime $creationTime) {
		$this->creationTime = $creationTime;
	}
	
	/**
	 * @return DateTime
	 */
	public function getCreationTime() {
		return $this->creationTime;
	}
	
	/**
	 * @param int $esponseCount
	 */
	public function setResponseCount($responseCount) {
		$this->responseCount = (int)$responseCount;
	}
	
	/**
	 * @return int
	 */
	public function getResponseCount() {
		return $this->responseCount;
	}
	
	/**
	 * @param int $byAmount
	 */
	public function increaseResponseCount($byAmount = 1) {
		$this->responseCount += $byAmount;
	}
	
	/**
	 * @param int $id
	 */
	public function setId($id) {
		$this->id = $id;
	}
	
	/**
	 * @return int
	 */
	public function getId() {
		return $this->id;
	}
	
	/**
	 * @param string $source
	 */
	public function setSource($source) {
		$this->source = $source;
	}
	
	/**
	 * @return string
	 */
	public function getSource() {
		return $this->source;
	}
	
	/**
	 * @param string $gClickId
	 */
	public function setGClickId($gClickId) {
		$this->gClickId = $gClickId;
	}
	
	/**
	 * @return string
	 */
	public function getGClickId() {
		return $this->gClickId;
	}
	
	/**
	 * @param string $dClickId
	 */
	public function setDClickId($dClickId) {
		$this->dClickId = $dClickId;
	}
	
	/**
	 * @return string
	 */
	public function getDClickId() {
		return $this->dClickId;
	}
	
	/**
	 * @param string $name
	 */
	public function setName($name) {
		$this->name = $name;
	}
	
	/**
	 * @return string
	 */
	public function getName() {
		return $this->name;
	}
	
	/**
	 * @param string $medium
	 */
	public function setMedium($medium) {
		$this->medium = $medium;
	}
	
	/**
	 * @return string
	 */
	public function getMedium() {
		return $this->medium;
	}
	
	/**
	 * @param string $term
	 */
	public function setTerm($term) {
		$this->term = $term;
	}
	
	/**
	 * @return string
	 */
	public function getTerm() {
		return $this->term;
	}
	
	/**
	 * @param string $content
	 */
	public function setContent($content) {
		$this->content = $content;
	}
	
	/**
	 * @return string
	 */
	public function getContent() {
		return $this->content;
	}
	
}

?>