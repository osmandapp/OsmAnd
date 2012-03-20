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

namespace UnitedPrototype\GoogleAnalytics\Internals\Request;

use UnitedPrototype\GoogleAnalytics\Config;

use UnitedPrototype\GoogleAnalytics\Internals\Util;

/**
 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/core/GIFRequest.as
 */
abstract class HttpRequest {	
	
	/**
	 * Indicates the type of request, will be mapped to "utmt" parameter
	 * 
	 * @see ParameterHolder::$utmt
	 * @var string
	 */
	protected $type;
	
	/**
	 * @var \UnitedPrototype\GoogleAnalytics\Config
	 */
	protected $config;
	
	/**
	 * @var string
	 */
	protected $xForwardedFor;
	
	/**
	 * @var string
	 */
	protected $userAgent;
	
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Config $config
	 */
	public function __construct(Config $config = null) {
		$this->setConfig($config ? $config : new Config());
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Config
	 */
	public function getConfig() {
		return $this->config;
	}
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Config $config
	 */
	public function setConfig(Config $config) {
		$this->config = $config;
	}
	
	/**
	 * @param string $value
	 */
	protected function setXForwardedFor($value) {
		$this->xForwardedFor = $value;
	}
	
	/**
	 * @param string $value
	 */
	protected function setUserAgent($value) {
		$this->userAgent = $value;
	}
	
	/**
	 * @return string
	 */
	protected function buildHttpRequest() {
		$parameters = $this->buildParameters();
		
		// This constant is supported as the 4th argument of http_build_query()
		// from PHP 5.3.6 on and will tell it to use rawurlencode() instead of urlencode()
		// internally, see http://code.google.com/p/php-ga/issues/detail?id=3
		if(defined('PHP_QUERY_RFC3986')) {
			// http_build_query() does automatically skip all array entries
			// with null values, exactly what we want here
			$queryString = http_build_query($parameters->toArray(), '', '&', PHP_QUERY_RFC3986);
		} else {
			// Manually replace "+"s with "%20" for backwards-compatibility
			$queryString = str_replace('+', '%20', http_build_query($parameters->toArray(), '', '&'));
		}
		// Mimic Javascript's encodeURIComponent() encoding for the query
		// string just to be sure we are 100% consistent with GA's Javascript client
		$queryString = Util::convertToUriComponentEncoding($queryString);
		
		// Recent versions of ga.js use HTTP POST requests if the query string is too long
		$usePost = strlen($queryString) > 2036;
		
		if(!$usePost) {
			$r = 'GET ' . $this->config->getEndpointPath() . '?' . $queryString . ' HTTP/1.0' . "\r\n";
		} else {
			// FIXME: The "/p" shouldn't be hardcoded here, instead we need a GET and a POST endpoint...
			$r = 'POST /p' . $this->config->getEndpointPath() . ' HTTP/1.0' . "\r\n";
		}
		$r .= 'Host: ' . $this->config->getEndpointHost() . "\r\n";
		
		if($this->userAgent) {
			$r .= 'User-Agent: ' . str_replace(array("\n", "\r"), '', $this->userAgent) . "\r\n";
		}
		
		if($this->xForwardedFor) {
			// Sadly "X-Fowarded-For" is not supported by GA so far,
			// see e.g. http://www.google.com/support/forum/p/Google+Analytics/thread?tid=017691c9e71d4b24,
			// but we include it nonetheless for the pure sake of correctness (and hope)
			$r .= 'X-Forwarded-For: ' . str_replace(array("\n", "\r"), '', $this->xForwardedFor) . "\r\n";
		}
		
		if($usePost) {
			// Don't ask me why "text/plain", but ga.js says so :)
			$r .= 'Content-Type: text/plain' . "\r\n";
			$r .= 'Content-Length: ' . strlen($queryString) . "\r\n";
		}
		
		$r .= 'Connection: close' . "\r\n";
		$r .= "\r\n\r\n";
		
		if($usePost) {
			$r .= $queryString;
		}
		
		return $r;
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder
	 */
	protected abstract function buildParameters();
	
	/**
	 * This method should only be called directly or indirectly by fire(), but must
	 * remain public as it can be called by a closure function.
	 * 
	 * Sends either a normal HTTP request with response or an asynchronous request
	 * to Google Analytics without waiting for the response. Will always return
	 * null in the latter case, or false if any connection problems arise.
	 * 
	 * @see HttpRequest::fire()
	 * @param string $request
	 * @return null|string|bool
	 */
	public function _send() {
		$request = $this->buildHttpRequest();
		$response = null;
		
		// Do not actually send the request if endpoint host is set to null
		if($this->config->getEndpointHost() !== null) {
			$timeout = $this->config->getRequestTimeout();
			
			$socket = fsockopen($this->config->getEndpointHost(), 80, $errno, $errstr, $timeout);
			if(!$socket) return false;
			
			if($this->config->getFireAndForget()) {
				stream_set_blocking($socket, false);
			}
			
			$timeoutS  = intval($timeout);
			$timeoutUs = ($timeout - $timeoutS) * 100000;
			stream_set_timeout($socket, $timeoutS, $timeoutUs);
			
			fwrite($socket, $request);
			
			if(!$this->config->getFireAndForget()) {
				while(!feof($socket)) {
					$response .= fgets($socket, 512);
				}
			}
			
			fclose($socket);
		}
		
		if($loggingCallback = $this->config->getLoggingCallback()) {
			$loggingCallback($request, $response);
		}
		
		return $response;
	}
	
	/**
	 * Simply delegates to send() if config option "sendOnShutdown" is disabled
	 * or enqueues the request by registering a PHP shutdown function.
	 */
	public function fire() {
		if($this->config->getSendOnShutdown()) {
			// This dumb variable assignment is needed as PHP prohibits using
			// $this in closure use statements
			$instance = $this;
			// We use a closure here to retain the current values/states of
			// this instance and $request (as the use statement will copy them
			// into its own scope)
			register_shutdown_function(function() use($instance) {
				$instance->_send();
			});
		} else {
			$this->_send();
		}
	}

}

?>