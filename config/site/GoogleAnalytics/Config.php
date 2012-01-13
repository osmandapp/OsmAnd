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
 * Note: Doesn't necessarily have to be consistent across requests, as it doesn't
 * alter the actual tracking result.
 * 
 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/core/GIFRequest.as
 */
class Config {
	
	/**
	 * How strict should errors get handled? After all, we do just do some
	 * tracking stuff here, and errors shouldn't break an application's
	 * functionality in production.
	 * RECOMMENDATION: Exceptions during deveopment, warnings in production.
	 * 
	 * Assign any value of the self::ERROR_SEVERITY_* constants.
	 * 
	 * @see Tracker::_raiseError()
	 * @var int
	 */
	protected $errorSeverity = self::ERROR_SEVERITY_EXCEPTIONS;
	
	/**
	 * Ignore all errors completely.
	 */
	const ERROR_SEVERITY_SILENCE    = 0;
	/**
	 * Trigger PHP errors with a E_USER_WARNING error level.
	 */
	const ERROR_SEVERITY_WARNINGS   = 1;
	/**
	 * Throw UnitedPrototype\GoogleAnalytics\Exception exceptions.
	 */
	const ERROR_SEVERITY_EXCEPTIONS = 2;
	
	/**
	 * Whether to just queue all requests on HttpRequest::fire() and actually send
	 * them on PHP script shutdown after all other tasks are done.
	 * 
	 * This has two advantages:
	 * 1) It effectively doesn't affect app performance
	 * 2) It can e.g. handle custom variables that were set after scheduling a request
	 * 
	 * @see Internals\Request\HttpRequest::fire()
	 * @var bool
	 */
	protected $sendOnShutdown = false;
	
	/**
	 * Whether to make asynchronous requests to GA without waiting for any
	 * response (speeds up doing requests).
	 * 
	 * @see Internals\Request\HttpRequest::send()
	 * @var bool
	 */
	protected $fireAndForget = false;
	
	/**
	 * Logging callback, registered via setLoggingCallback(). Will be fired
	 * whenever a request gets sent out and receives the full HTTP request
	 * as the first and the full HTTP response (or null if the "fireAndForget"
	 * option or simulation mode are used) as the second argument.
	 * 
	 * @var \Closure
	 */
	protected $loggingCallback;
	
	/**
	 * Seconds (float allowed) to wait until timeout when connecting to the
	 * Google analytics endpoint host
	 * 
	 * @see Internals\Request\HttpRequest::send()
	 * @var float
	 */
	protected $requestTimeout = 1;
	
	// FIXME: Add SSL support, https://ssl.google-analytics.com
	
	/**
	 * Google Analytics tracking request endpoint host. Can be set to null to
	 * silently simulate (and log) requests without actually sending them.
	 * 
	 * @see Internals\Request\HttpRequest::send()
	 * @var string
	 */
	protected $endPointHost = 'www.google-analytics.com';
	
	/**
	 * Google Analytics tracking request endpoint path
	 * 
	 * @see Internals\Request\HttpRequest::send()
	 * @var string
	 */
	protected $endPointPath = '/__utm.gif';
	
	/**
	 * Whether to anonymize IP addresses within Google Analytics by stripping
	 * the last IP address block, will be mapped to "aip" parameter
	 * 
	 * @see Internals\ParameterHolder::$aip
	 * @link http://code.google.com/apis/analytics/docs/gaJS/gaJSApi_gat.html#_gat._anonymizeIp
	 * @var bool
	 */
	protected $anonymizeIpAddresses = false;
	
	
	/**
	 * @param array $properties
	 */
	public function __construct(array $properties = array()) {
		foreach($properties as $property => $value) {
			// PHP doesn't care about case in method names
			$setterMethod = 'set' . $property;
			
			if(method_exists($this, $setterMethod)) {
				$this->$setterMethod($value);
			} else {
				return Tracker::_raiseError('There is no setting "' . $property . '".', __METHOD__);
			}
		}
	}
	
	/**
	 * @return int See self::ERROR_SEVERITY_* constants
	 */
	public function getErrorSeverity() {
		return $this->errorSeverity;
	}
	
	/**
	 * @param int $errorSeverity See self::ERROR_SEVERITY_* constants
	 */
	public function setErrorSeverity($errorSeverity) {
		$this->errorSeverity = $errorSeverity;
	}
	
	/**
	 * @return bool
	 */
	public function getSendOnShutdown() {
		return $this->sendOnShutdown;
	}
	
	/**
	 * @param bool $sendOnShutdown
	 */
	public function setSendOnShutdown($sendOnShutdown) {
		$this->sendOnShutdown = $sendOnShutdown;
	}
	
	/**
	 * @return bool
	 */
	public function getFireAndForget() {
		return $this->fireAndForget;
	}
	
	/**
	 * @param bool $fireAndForget
	 */
	public function setFireAndForget($fireAndForget) {
		$this->fireAndForget = (bool)$fireAndForget;
	}
	
	/**
	 * @return \Closure|null
	 */
	public function getLoggingCallback() {
		return $this->loggingCallback;
	}
	
	/**
	 * @param \Closure $callback
	 */
	public function setLoggingCallback(\Closure $callback) {
		$this->loggingCallback = $callback;
	}
	
	/**
	 * @return float
	 */
	public function getRequestTimeout() {
		return $this->requestTimeout;
	}
	
	/**
	 * @param float $requestTimeout
	 */
	public function setRequestTimeout($requestTimeout) {
		$this->requestTimeout = (float)$requestTimeout;
	}
	
	/**
	 * @return string|null
	 */
	public function getEndPointHost() {
		return $this->endPointHost;
	}
	
	/**
	 * @param string|null $endPointHost
	 */
	public function setEndPointHost($endPointHost) {
		$this->endPointHost = $endPointHost;
	}
	
	/**
	 * @return string
	 */
	public function getEndPointPath() {
		return $this->endPointPath;
	}
	
	/**
	 * @param string $endPointPath
	 */
	public function setEndPointPath($endPointPath) {
		$this->endPointPath = $endPointPath;
	}
	
	/**
	 * @return bool
	 */
	public function getAnonymizeIpAddresses() {
		return $this->anonymizeIpAddresses;
	}
	
	/**
	 * @param bool $anonymizeIpAddresses
	 */
	public function setAnonymizeIpAddresses($anonymizeIpAddresses) {
		$this->anonymizeIpAddresses = $anonymizeIpAddresses;
	}

}

?>