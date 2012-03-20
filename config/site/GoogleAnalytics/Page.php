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

class Page {
	
	/**
	 * Page request URI, e.g. "/path/page.html", will be mapped to
	 * "utmp" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmp
	 * @var string
	 */
	protected $path;
	
	/**
	 * Page title, will be mapped to "utmdt" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmdt
	 * @var string
	 */
	protected $title;
	
	/**
	 * Charset encoding (e.g. "UTF-8"), will be mapped to "utmcs" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmcs
	 * @var string
	 */
	protected $charset;
	
	/**
	 * Referer URL, e.g. "http://www.example.com/path/page.html",  will be
	 * mapped to "utmr" parameter
	 * 
	 * @see Internals\ParameterHolder::$utmr
	 * @var string
	 */
	protected $referrer;
	
	
	/**
	 * Constant to mark referrer as a site-internal one.
	 * 
	 * @see Page::$referrer
	 * @const string
	 */
	const REFERRER_INTERNAL = '0';
	
	
	/**
	 * @param string $path
	 */
	public function __construct($path) {
		$this->setPath($path);
	}
	
	/**
	 * @param string $path
	 */
	public function setPath($path) {
		if($path && $path[0] != '/') {
			Tracker::_raiseError('The page path should always start with a slash ("/").', __METHOD__);
		}
		
		$this->path = $path;
	}
	
	/**
	 * @return string
	 */
	public function getPath() {
		return $this->path;
	}
	
	/**
	 * @param string $title
	 */
	public function setTitle($title) {
		$this->title = $title;
	}
	
	/**
	 * @return string
	 */
	public function getTitle() {
		return $this->title;
	}
	
	/**
	 * @param string $charset
	 */
	public function setCharset($encoding) {
		$this->charset = $encoding;
	}
	
	/**
	 * @return string
	 */
	public function getCharset() {
		return $this->charset;
	}
	
	/**
	 * @param string $referrer
	 */
	public function setReferrer($referrer) {
		$this->referrer = $referrer;
	}
	
	/**
	 * @return string
	 */
	public function getReferrer() {
		return $this->referrer;
	}
	
}

?>