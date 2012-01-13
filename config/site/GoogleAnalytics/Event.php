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

/**
 * @link http://code.google.com/apis/analytics/docs/tracking/eventTrackerOverview.html
 * @link http://code.google.com/apis/analytics/docs/gaJS/gaJSApiEventTracking.html
 */
class Event {	
	
	/**
	 * The general event category (e.g. "Videos").
	 * 
	 * @var string
	 */
	protected $category;
	
	/**
	 * The action for the event (e.g. "Play").
	 * 
	 * @var string
	 */
	protected $action;
	
	/**
	 * An optional descriptor for the event (e.g. the video's title).
	 * 
	 * @var string
	 */
	protected $label;
	
	/**
	 * An optional value associated with the event. You can see your event values in the Overview,
	 * Categories, and Actions reports, where they are listed by event or aggregated across events,
	 * depending upon your report view.
	 * 
	 * @var int
	 */
	protected $value;
	
	/**
	 * Default value is false. By default, event hits will impact a visitor's bounce rate.
	 * By setting this parameter to true, this event hit will not be used in bounce rate calculations.
	 * 
	 * @var bool
	 */
	protected $noninteraction = false;
	
	
	/**
	 * @param string $category
	 * @param string $action
	 * @param string $label
	 * @param int $value
	 * @param bool $noninteraction
	 */
	public function __construct($category = null, $action = null, $label = null, $value = null, $noninteraction = null) {
		if($category       !== null) $this->setCategory($category);
		if($action         !== null) $this->setAction($action);
		if($label          !== null) $this->setLabel($label);
		if($value          !== null) $this->setValue($value);
		if($noninteraction !== null) $this->setNoninteraction($noninteraction);
	}
	
	public function validate() {
		if($this->category === null || $this->action === null) {
			Tracker::_raiseError('Events need at least to have a category and action defined.', __METHOD__);
		}
	}
	
	/**
	 * @return string
	 */
	public function getCategory() {
		return $this->category;
	}
	
	/**
	 * @param string $category
	 */
	public function setCategory($category) {
		$this->category = $category;
	}
	
	/**
	 * @return string
	 */
	public function getAction() {
		return $this->action;
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
	public function getLabel() {
		return $this->label;
	}
	
	/**
	 * @param string $label
	 */
	public function setLabel($label) {
		$this->label = $label;
	}
	
	/**
	 * @return int
	 */
	public function getValue() {
		return $this->value;
	}
	
	/**
	 * @param int $value
	 */
	public function setValue($value) {
		$this->value = (int)$value;
	}
	
	/**
	 * @return bool
	 */
	public function getNoninteraction() {
		return $this->noninteraction;
	}
	
	/**
	 * @param bool $value
	 */
	public function setNoninteraction($value) {
		$this->noninteraction = (bool)$value;
	}
	
}

?>