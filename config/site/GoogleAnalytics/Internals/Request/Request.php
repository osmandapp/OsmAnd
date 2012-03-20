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

use UnitedPrototype\GoogleAnalytics\Tracker;
use UnitedPrototype\GoogleAnalytics\Visitor;
use UnitedPrototype\GoogleAnalytics\Session;
use UnitedPrototype\GoogleAnalytics\CustomVariable;

use UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder;
use UnitedPrototype\GoogleAnalytics\Internals\Util;
use UnitedPrototype\GoogleAnalytics\Internals\X10;

abstract class Request extends HttpRequest {
	
	/**
	 * @var \UnitedPrototype\GoogleAnalytics\Tracker
	 */
	protected $tracker;
	
	/**
	 * @var \UnitedPrototype\GoogleAnalytics\Visitor
	 */
	protected $visitor;
	
	/**
	 * @var \UnitedPrototype\GoogleAnalytics\Session
	 */
	protected $session;
	
	
	/**
	 * @const string
	 */
	const TYPE_PAGE           = null;
	/**
	 * @const string
	 */
	const TYPE_EVENT          = 'event';
	/**
	 * @const string
	 */
	const TYPE_TRANSACTION    = 'tran';
	/**
	 * @const string
	 */
	const TYPE_ITEM           = 'item';
	/**
	 * @const string
	 */
	const TYPE_SOCIAL         = 'social';
	/**
	 * This type of request is deprecated in favor of encoding custom variables
	 * within the "utme" parameter, but we include it here for completeness
	 * 
	 * @see ParameterHolder::$__utmv
	 * @link http://code.google.com/apis/analytics/docs/gaJS/gaJSApiBasicConfiguration.html#_gat.GA_Tracker_._setVar
	 * @deprecated
	 * @const string
	 */
	const TYPE_CUSTOMVARIABLE = 'var';
	
	/**
	 * @const int
	 */
	const X10_CUSTOMVAR_NAME_PROJECT_ID  = 8;
	/**
	 * @const int
	 */
	const X10_CUSTOMVAR_VALUE_PROJECT_ID = 9;
	/**
	 * @const int
	 */
	const X10_CUSTOMVAR_SCOPE_PROJECT_ID = 11;
	
	/**
	 * @const string
	 */
	const CAMPAIGN_DELIMITER = '|';
	
	
	/**
	 * Indicates the type of request, will be mapped to "utmt" parameter
	 * 
	 * @see ParameterHolder::$utmt
	 * @return string See Request::TYPE_* constants
	 */
	protected abstract function getType();
	
	/**
	 * @return string
	 */
	protected function buildHttpRequest() {
		$this->setXForwardedFor($this->visitor->getIpAddress());
		$this->setUserAgent($this->visitor->getUserAgent());
		
		// Increment session track counter for each request
		$this->session->increaseTrackCount();
		
		// See http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/v4/Configuration.as?r=237#48
		// and http://code.google.com/intl/de-DE/apis/analytics/docs/tracking/eventTrackerGuide.html#implementationConsiderations
		if($this->session->getTrackCount() > 500) {
			Tracker::_raiseError('Google Analytics does not guarantee to process more than 500 requests per session.', __METHOD__);
		}
		
		if($this->tracker->getCampaign()) {
			$this->tracker->getCampaign()->increaseResponseCount();
		}
		
		return parent::buildHttpRequest();
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder
	 */
	protected function buildParameters() {
		$p = new ParameterHolder();
		
		$p->utmac = $this->tracker->getAccountId();
		$p->utmhn = $this->tracker->getDomainName();
		
		$p->utmt = $this->getType();
		$p->utmn = Util::generate32bitRandom();
		
		$p->aip = $this->tracker->getConfig()->getAnonymizeIpAddresses() ? 1 : null;
		
		// The IP parameter does sadly seem to be ignored by GA, so we
		// shouldn't set it as of today but keep it here for later reference
		// $p->utmip = $this->visitor->getIpAddress();
		
		$p->utmhid = $this->session->getSessionId();
		$p->utms   = $this->session->getTrackCount();
		
		$p = $this->buildVisitorParameters($p);
		$p = $this->buildCustomVariablesParameter($p);
		$p = $this->buildCampaignParameters($p);
		$p = $this->buildCookieParameters($p);
		
		return $p;
	}
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder $p
	 * @return \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder
	 */
	protected function buildVisitorParameters(ParameterHolder $p) {
		// Ensure correct locale format, see https://developer.mozilla.org/en/navigator.language
		$p->utmul = strtolower(str_replace('_', '-', $this->visitor->getLocale()));
		
		if($this->visitor->getFlashVersion() !== null) {
			$p->utmfl = $this->visitor->getFlashVersion();
		}
		if($this->visitor->getJavaEnabled() !== null) {
			$p->utmje = $this->visitor->getJavaEnabled();
		}
		if($this->visitor->getScreenColorDepth() !== null) {
			$p->utmsc = $this->visitor->getScreenColorDepth() . '-bit';
		}
		$p->utmsr = $this->visitor->getScreenResolution();
		
		return $p;
	}
	
	/**
	 * @link http://xahlee.org/js/google_analytics_tracker_2010-07-01_expanded.js line 575
	 * @param \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder $p
	 * @return \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder
	 */
	protected function buildCustomVariablesParameter(ParameterHolder $p) {
		$customVars = $this->tracker->getCustomVariables();
		if($customVars) {
			if(count($customVars) > 5) {
				// See http://code.google.com/intl/de-DE/apis/analytics/docs/tracking/gaTrackingCustomVariables.html#usage
				Tracker::_raiseError('The sum of all custom variables cannot exceed 5 in any given request.', __METHOD__);
			}
			
			$x10 = new X10();
			
			$x10->clearKey(self::X10_CUSTOMVAR_NAME_PROJECT_ID);
			$x10->clearKey(self::X10_CUSTOMVAR_VALUE_PROJECT_ID);
			$x10->clearKey(self::X10_CUSTOMVAR_SCOPE_PROJECT_ID);
			
			foreach($customVars as $customVar) {
				// Name and value get encoded here,
				// see http://xahlee.org/js/google_analytics_tracker_2010-07-01_expanded.js line 563
				$name  = Util::encodeUriComponent($customVar->getName());
				$value = Util::encodeUriComponent($customVar->getValue());
				
				$x10->setKey(self::X10_CUSTOMVAR_NAME_PROJECT_ID, $customVar->getIndex(), $name);
				$x10->setKey(self::X10_CUSTOMVAR_VALUE_PROJECT_ID, $customVar->getIndex(), $value);
				if($customVar->getScope() !== null && $customVar->getScope() != CustomVariable::SCOPE_PAGE) {
					$x10->setKey(self::X10_CUSTOMVAR_SCOPE_PROJECT_ID, $customVar->getIndex(), $customVar->getScope());
				}
			}
			
			$p->utme .= $x10->renderUrlString();
		}
		
		return $p;
	}
	
	/**
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/core/GIFRequest.as#123
	 * @param \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder $p
	 * @return \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder
	 */
	protected function buildCookieParameters(ParameterHolder $p) {
		$domainHash = $this->generateDomainHash();
		
		$p->__utma  = $domainHash . '.';
		$p->__utma .= $this->visitor->getUniqueId() . '.';
		$p->__utma .= $this->visitor->getFirstVisitTime()->format('U') . '.';
		$p->__utma .= $this->visitor->getPreviousVisitTime()->format('U') . '.';
		$p->__utma .= $this->visitor->getCurrentVisitTime()->format('U') . '.';
		$p->__utma .= $this->visitor->getVisitCount();
		
		$p->__utmb  = $domainHash . '.';
		$p->__utmb .= $this->session->getTrackCount() . '.';
		// FIXME: What does "token" mean? I only encountered a value of 10 in my tests.
		$p->__utmb .= 10 . '.';
		$p->__utmb .= $this->session->getStartTime()->format('U');
		
		$p->__utmc = $domainHash;
		
		$cookies = array();
		$cookies[] = '__utma=' . $p->__utma . ';';
		if($p->__utmz) {
			$cookies[] = '__utmz=' . $p->__utmz . ';';
		}
		if($p->__utmv) {
			$cookies[] = '__utmv=' . $p->__utmv . ';';
		}
		
		$p->utmcc = implode('+', $cookies);
		
		return $p;
	}
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder $p
	 * @return \UnitedPrototype\GoogleAnalytics\Internals\ParameterHolder
	 */
	protected function buildCampaignParameters(ParameterHolder $p) {
		$campaign = $this->tracker->getCampaign();
		if($campaign) {
			$p->__utmz  = $this->generateDomainHash() . '.';
			$p->__utmz .= $campaign->getCreationTime()->format('U') . '.';
			$p->__utmz .= $this->visitor->getVisitCount() . '.';
			$p->__utmz .= $campaign->getResponseCount() . '.';
			
			// See http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/campaign/CampaignTracker.as#236
			$data = array(
				'utmcid'   => $campaign->getId(),
				'utmcsr'   => $campaign->getSource(),
				'utmgclid' => $campaign->getGClickId(),
				'utmdclid' => $campaign->getDClickId(),
				'utmccn'   => $campaign->getName(),
				'utmcmd'   => $campaign->getMedium(),
				'utmctr'   => $campaign->getTerm(),
				'utmcct'   => $campaign->getContent(),
			);
			foreach($data as $key => $value) {
				if($value !== null && $value !== '') {
					// Only spaces and pluses get escaped in gaforflash and ga.js, so we do the same
					$p->__utmz .= $key . '=' . str_replace(array('+', ' '), '%20', $value) . static::CAMPAIGN_DELIMITER;
				}
			}
			$p->__utmz = rtrim($p->__utmz, static::CAMPAIGN_DELIMITER);
		}
		
		return $p;
	}
	
	/**
	 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/v4/Tracker.as#585
	 * @return string
	 */
	protected function generateDomainHash() {
		$hash = 1;
		
		if($this->tracker->getAllowHash()) {
			$hash = Util::generateHash($this->tracker->getDomainName());
		}
		
		return $hash;
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Tracker
	 */
	public function getTracker() {
		return $this->tracker;
	}
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Tracker $tracker
	 */
	public function setTracker(Tracker $tracker) {
		$this->tracker = $tracker;
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Visitor
	 */
	public function getVisitor() {
		return $this->visitor;
	}
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Visitor $visitor
	 */
	public function setVisitor(Visitor $visitor) {
		$this->visitor = $visitor;
	}
	
	/**
	 * @return \UnitedPrototype\GoogleAnalytics\Session
	 */
	public function getSession() {
		return $this->session;
	}
	
	/**
	 * @param \UnitedPrototype\GoogleAnalytics\Session $session
	 */
	public function setSession(Session $session) {
		$this->session = $session;
	}
	
}

?>