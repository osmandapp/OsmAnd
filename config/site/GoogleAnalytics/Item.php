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
 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/ecommerce/Item.as
 */
class Item {
	
	/**
	 * Order ID, e.g. "a2343898", will be mapped to "utmtid" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmtid
	 * @var string
	 */
	protected $orderId;
	
	/**
	 * Product Code. This is the sku code for a given product, e.g. "989898ajssi",
	 * will be mapped to "utmipc" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmipc
	 * @var string
	 */
	protected $sku;
	
	/**
	 * Product Name, e.g. "T-Shirt", will be mapped to "utmipn" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmipn
	 * @var string
	 */
	protected $name;
	
	/**
	 * Variations on an item, e.g. "white", "black", "green" etc., will be mapped
	 * to "utmiva" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmiva
	 * @var string
	 */
	protected $variation;
	
	/**
	 * Unit Price. Value is set to numbers only (e.g. 19.95), will be mapped to
	 * "utmipr" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmipr
	 * @var float
	 */
	protected $price;
	
	/**
	 * Unit Quantity, e.g. 4, will be mapped to "utmiqt" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmiqt
	 * @var int
	 */
	protected $quantity = 1;
	
	
	public function validate() {
		if($this->sku === null) {
			Tracker::_raiseError('Items need to have a sku/product code defined.', __METHOD__);
		}
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
	}
	
	/**
	 * @return string
	 */
	public function getSku() {
		return $this->sku;
	}
	
	/**
	 * @param string $sku
	 */
	public function setSku($sku) {
		$this->sku = $sku;
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
	 * @return string
	 */
	public function getVariation() {
		return $this->variation;
	}
	
	/**
	 * @param string $variation
	 */
	public function setVariation($variation) {
		$this->variation = $variation;
	}
	
	/**
	 * @return float
	 */
	public function getPrice() {
		return $this->price;
	}
	
	/**
	 * @param float $price
	 */
	public function setPrice($price) {
		$this->price = (float)$price;
	}
	
	/**
	 * @return int
	 */
	public function getQuantity() {
		return $this->quantity;
	}
	
	/**
	 * @param int $quantity
	 */
	public function setQuantity($quantity) {
		$this->quantity = (int)$quantity;
	}
	
}

?>