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
 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/ecommerce/Transaction.as
 */
class Transaction {
	
	/**
	 * Order ID, e.g. "a2343898", will be mapped to "utmtid" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtid
	 * @var string
	 */
	protected $orderId;
	
	/**
	 * Affiliation, Will be mapped to "utmtst" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtst
	 * @var string
	 */
	protected $affiliation;
	
	/**
	 * Total Cost, will be mapped to "utmtto" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtto
	 * @var float
	 */
	protected $total;
	
	/**
	 * Tax Cost, will be mapped to "utmttx" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmttx
	 * @var float
	 */
	protected $tax;
	
	/**
	 * Shipping Cost, values as for unit and price, e.g. 3.95, will be mapped to
	 * "utmtsp" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtsp
	 * @var float
	 */
	protected $shipping;
	
	/**
	 * Billing City, e.g. "Cologne", will be mapped to "utmtci" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtci
	 * @var string
	 */
	protected $city;
	
	/**
	 * Billing Region, e.g. "North Rhine-Westphalia", will be mapped to "utmtrg" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtrg
	 * @var string
	 */
	protected $region;
	
	/**
	 * Billing Country, e.g. "Germany", will be mapped to "utmtco" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtco
	 * @var string
	 */
	protected $country;
	
	/**
	 * @see Transaction::addItem()
	 * @var \UnitedPrototype\GoogleAnalytics\Item[]
	 */
	protected $items = array();
	
	
	public function validate() {
		if(!$this->items) {
			Tracker::_raiseError('Transactions need to consist of at least one item.', __METHOD__);
		}
	}
	
	/**
	 * @link http://code.google.com/apis/analytics/docs/gaJS/gaJSApiEcommerce.html#_gat.GA_Tracker_._addItem
	 * @param \UnitedPrototype\GoogleAnalytics\Item $item
	 */
	public function addItem(Item $item) {
		// Associated items inherit the transaction's order ID
		$item->setOrderId($this->orderId);
		
		$sku = $item->getSku();
		$this->items[$sku] = $item;
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Item[]
	 */
	public function getItems() {
		return $this->items;
	}
	
	/**
	 * @return string
	 */
	public function getOrderId() {
		return $this->orderId;
	}
	
	/**
	 * @param string $orderId
	 */
	public function setOrderId($orderId) {
		$this->orderId = $orderId;
		
		// Update order IDs of all associated items too
		foreach($this->items as $item) {
			$item->setOrderId($orderId);
		}
	}
	
	/**
	 * @return string
	 */
	public function getAffiliation() {
		return $this->affiliation;
	}
	
	/**
	 * @param string $affiliation
	 */
	public function setAffiliation($affiliation) {
		$this->affiliation = $affiliation;
	}
	
	/**
	 * @return float
	 */
	public function getTotal() {
		return $this->total;
	}
	
	/**
	 * @param float $total
	 */
	public function setTotal($total) {
		$this->total = $total;
	}
	
	/**
	 * @return float
	 */
	public function getTax() {
		return $this->tax;
	}
	
	/**
	 * @param float $tax
	 */
	public function setTax($tax) {
		$this->tax = $tax;
	}
	
	/**
	 * @return float
	 */
	public function getShipping() {
		return $this->shipping;
	}
	
	/**
	 * @param float $shipping
	 */
	public function setShipping($shipping) {
		$this->shipping = $shipping;
	}
	
	/**
	 * @return string
	 */
	public function getCity() {
		return $this->city;
	}
	
	/**
	 * @param string $city
	 */
	public function setCity($city) {
		$this->city = $city;
	}
	
	/**
	 * @return string
	 */
	public function getRegion() {
		return $this->region;
	}
	
	/**
	 * @param string $region
	 */
	public function setRegion($region) {
		$this->region = $region;
	}
	
	/**
	 * @return string
	 */
	public function getCountry() {
		return $this->country;
	}
	
	/**
	 * @param string $country
	 */
	public function setCountry($country) {
		$this->country = $country;
	}
	
}

?>