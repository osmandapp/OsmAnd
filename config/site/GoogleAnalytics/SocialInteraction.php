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

class SocialInteraction {
	
	/**
	 * Required. A string representing the social network being tracked (e.g. "Facebook", "Twitter", "LinkedIn", ...),
	 * will be mapped to "utmsn" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmsn
	 * @var string
	 */
	protected $network;
	
	/**
	 * Required. A string representing the social action being tracked (e.g. "Like", "Share", "Tweet", ...),
	 * will be mapped to "utmsa" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmsa
	 * @var string
	 */
	protected $action;
	
	/**
	 * Optional. A string representing the URL (or resource) which receives the action. For example,
	 * if a user clicks the Like button on a page on a site, the the target might be set to the title
	 * of the page, or an ID used to identify the page in a content management system. In many cases,
	 * the page you Like is the same page you are on. So if this parameter is not given, we will default
	 * to using the path of the corresponding Page object.
	 * 
	 * @see Internals\ParameterHolder::$utmsid
	 * @var string
	 */
	protected $target;
	
	
	/**
	 * @param string $path
	 */
	public function __construct($network = null, $action = null, $target = null) {
		if($network !== null) $this->setNetwork($network);
		if($action  !== null) $this->setAction($action);
		if($target  !== null) $this->setTarget($target);
	}
	
	public function validate() {
		if($this->network === null || $this->action === null) {
			Tracker::_raiseError('Social interactions need to have at least the "network" and "action" attributes defined.', __METHOD__);
		}
	}
	
	/**
	 * @param string $network
	 */
	public function setNetwork($network) {
		$this->network = $network;
	}
	
	/**
	 * @return string
	 */
	public function getNetwork() {
		return $this->network;
	}
	
	/**
	 * @param string $action
	 */
	public function setAction($action) {
		$this->action = $action;
	}
	
	/**
	 * @return string
	 */
	public function getAction() {
		return $this->action;
	}
	
	/**
	 * @param string $target
	 */
	public function setTarget($target) {
		$this->target = $target;
	}
	
	/**
	 * @return string
	 */
	public function getTarget() {
		return $this->target;
	}
	
}

?>