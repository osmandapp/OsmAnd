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

/**
 * @link http://code.google.com/apis/analytics/docs/tracking/gaTrackingCustomVariables.html
 */
class CustomVariable {
	
	/**
	 * @var int
	 */
	protected $index;
	
	/**
	 * WATCH OUT: It's a known issue that GA will not decode URL-encoded characters
	 * in custom variable names and values properly, so spaces will show up
	 * as "%20" in the interface etc.
	 * 
	 * @link http://www.google.com/support/forum/p/Google%20Analytics/thread?tid=2cdb3ec0be32e078
	 * @var string
	 */
	protected $name;
	
	/**
	 * WATCH OUT: It's a known issue that GA will not decode URL-encoded characters
	 * in custom variable names and values properly, so spaces will show up
	 * as "%20" in the interface etc.
	 * 
	 * @link http://www.google.com/support/forum/p/Google%20Analytics/thread?tid=2cdb3ec0be32e078
	 * @var mixed
	 */
	protected $value;
	
	/**
	 * See SCOPE_* constants
	 * 
	 * @var int
	 */
	protected $scope = self::SCOPE_PAGE;
	
	
	/**
	 * @const int
	 */
	const SCOPE_VISITOR = 1;
	/**
	 * @const int
	 */
	const SCOPE_SESSION = 2;
	/**
	 * @const int
	 */
	const SCOPE_PAGE    = 3;
	
	
	/**
	 * @param int $index
	 * @param string $name
	 * @param mixed $value
	 * @param int $scope See SCOPE_* constants
	 */
	public function __construct($index = null, $name = null, $value = null, $scope = null) {
		if($index !== null) $this->setIndex($index);
		if($name  !== null) $this->setName($name);
		if($value !== null) $this->setValue($value);
		if($scope !== null) $this->setScope($scope);
	}
	
	public function validate() {
		// According to the GA documentation, there is a limit to the combined size of
		// name and value of 64 bytes after URL encoding,
		// see http://code.google.com/apis/analytics/docs/tracking/gaTrackingCustomVariables.html#varTypes
		// and http://xahlee.org/js/google_analytics_tracker_2010-07-01_expanded.js line 563
		if(strlen(Util::encodeUriComponent($this->name . $this->value)) > 64) {
			Tracker::_raiseError('Custom Variable combined name and value encoded length must not be larger than 64 bytes.', __METHOD__);
		}
	}
	
	/**
	 * @return int
	 */
	public function getIndex() {
		return $this->index;
	}
	
	/**
	 * @link http://code.google.com/intl/de-DE/apis/analytics/docs/tracking/gaTrackingCustomVariables.html#usage
	 * @param int $index
	 */
	public function setIndex($index) {
		// Custom Variables are limited to five slots officially, but there seems to be a
		// trick to allow for more of them which we could investigate at a later time (see
		// http://analyticsimpact.com/2010/05/24/get-more-than-5-custom-variables-in-google-analytics/)
		if($index < 1 || $index > 5) {
			Tracker::_raiseError('Custom Variable index has to be between 1 and 5.', __METHOD__);
		}
		
		$this->index = (int)$index;
	}
	
	/**
	 * @return string
	 */
	public function getName() {
		return $this->name;
	}
	
	/**
	 * @param string $name
	 */
	public function setName($name) {
		$this->name = $name;
	}
	
	/**
	 * @return mixed
	 */
	public function getValue() {
		return $this->value;
	}
	
	/**
	 * @param mixed $value
	 */
	public function setValue($value) {
		$this->value = $value;
	}
	
	/**
	 * @return int
	 */
	public function getScope() {
		return $this->scope;
	}
	
	/**
	 * @param int $scope
	 */
	public function setScope($scope) {
		if(!in_array($scope, array(self::SCOPE_PAGE, self::SCOPE_SESSION, self::SCOPE_VISITOR))) {
			Tracker::_raiseError('Custom Variable scope has to be one of the CustomVariable::SCOPE_* constant values.', __METHOD__);
		}
		
		$this->scope = (int)$scope;
	}
	
}

?>