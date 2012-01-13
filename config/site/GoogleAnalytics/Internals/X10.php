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

namespace UnitedPrototype\GoogleAnalytics\Internals;

/**
 * This is nearly a 1:1 PHP port of the gaforflash X10 class code.
 * 
 * @link http://code.google.com/p/gaforflash/source/browse/trunk/src/com/google/analytics/data/X10.as
 */
class X10 {
	
	/**
	 * @var array
	 */
	protected $projectData = array();
	
	
	/**
	 * @var string
	 */
	protected $KEY = 'k';
	
	/**
	 * @var string
	 */
	protected $VALUE = 'v';
	
	/**
	 * @var array
	 */
	protected $SET = array('k', 'v');
	
	/**
	 * Opening delimiter for wrapping a set of values belonging to the same type.
	 * @var string
	 */
	protected $DELIM_BEGIN = '(';
	
	/**
	 * Closing delimiter for wrapping a set of values belonging to the same type.
	 * @var string
	 */
	protected $DELIM_END   = ')';
	
	/**
	 * Delimiter between two consecutive num/value pairs.
	 * @var string
	 */
	protected $DELIM_SET = '*';
	
	/**
	 * Delimiter between a num and its corresponding value.
	 * @var string
	 */
	protected $DELIM_NUM_VALUE = '!';
	
	/**
	 * Mapping of escapable characters to their escaped forms.
	 * 
	 * @var array
	 */
	protected $ESCAPE_CHAR_MAP = array(
		"'" => "'0",
		')' => "'1",
		'*' => "'2",
		'!' => "'3",
	);
	
	/**
	 * @var int
	 */
	protected $MINIMUM = 1;
	
	
	/**
	 * @param int $projectId
	 * @return bool
	 */
	protected function hasProject($projectId) {
		return isset($this->projectData[$projectId]);
	}
	
	/**
	 * @param int $projectId
	 * @param int $num
	 * @param mixed $value
	 */
	public function setKey($projectId, $num, $value) {
		$this->setInternal($projectId, $this->KEY, $num, $value);
	}
	
	/**
	 * @param int $projectId
	 * @param int $num
	 * @return mixed
	 */
	public function getKey($projectId, $num) {
		return $this->getInternal($projectId, $this->KEY, $num);
	}
	
	/**
	 * @param int $projectId
	 */
	public function clearKey($projectId) {
		$this->clearInternal($projectId, $this->KEY);
	}
	
	/**
	 * @param int $projectId
	 * @param int $num
	 * @param mixed $value
	 */
	public function setValue($projectId, $num, $value) {
		$this->setInternal($projectId, $this->VALUE, $num, $value);
	}
	
	/**
	 * @param int $projectId
	 * @param int $num
	 * @return mixed
	 */
	public function getValue($projectId, $num) {
		return $this->getInternal($projectId, $this->VALUE, $num);
	}
	
	/**
	 * @param int $projectId
	 */
	public function clearValue($projectId) {
		$this->clearInternal($projectId, $this->VALUE);
	}
	
	/**
	 * Shared internal implementation for setting an X10 data type.
	 * 
	 * @param int $projectId
	 * @param string $type
	 * @param int $num
	 * @param mixed $value
	 */
	protected function setInternal($projectId, $type, $num, $value) {
		if(!isset($this->projectData[$projectId])) {
			$this->projectData[$projectId] = array();
		}
		if(!isset($this->projectData[$projectId][$type])) {
			$this->projectData[$projectId][$type] = array();
		}
		
		$this->projectData[$projectId][$type][$num] = $value;
	}
	
	/**
	 * Shared internal implementation for getting an X10 data type.
	 * 
	 * @param int $projectId
	 * @param string $type
	 * @param int $num
	 * @return mixed
	 */
	protected function getInternal($projectId, $type, $num) {
		if(isset($this->projectData[$projectId][$type][$num])) {
			return $this->projectData[$projectId][$type][$num];
		} else {
			return null;
		}
	}
	
	/**
	 * Shared internal implementation for clearing all X10 data of a type from a
	 * certain project.
	 * 
	 * @param int $projectId
	 * @param string $type
	 */
	protected function clearInternal($projectId, $type) {
		if(isset($this->projectData[$projectId]) && isset($this->projectData[$projectId][$type])) {
			unset($this->projectData[$projectId][$type]);
		}
	}
	
	/**
	 * Escape X10 string values to remove ambiguity for special characters.
	 *
	 * @see X10::$escapeCharMap
	 * @param string $value
	 * @return string
	 */
	protected function escapeExtensibleValue($value) {
		$result = '';
		
		$value = (string)$value;
		$length = strlen($value);
		for($i = 0; $i < $length; $i++) {
			$char = $value[$i];
			
			if(isset($this->ESCAPE_CHAR_MAP[$char])) {
				$result .= $this->ESCAPE_CHAR_MAP[$char];
			} else {
				$result .= $char;
			}
		}
		
		return $result;
	}
	
	/**
	 * Given a data array for a certain type, render its string encoding.
	 * 
	 * @param array $data
	 * @return string
	 */
	protected function renderDataType(array $data) {
		$result = array();
		
		$lastI = 0;
		ksort($data, SORT_NUMERIC);
		foreach($data as $i => $entry) {
			if(isset($entry)) {
				$str = '';
				
				// Check if we need to append the number. If the last number was
				// outputted, or if this is the assumed minimum, then we don't.
				if($i != $this->MINIMUM && $i - 1 != $lastI) {
					$str .= $i;
					$str .= $this->DELIM_NUM_VALUE;
				}
	
				$str .= $this->escapeExtensibleValue($entry);
				$result[] = $str;
			}
			
			$lastI = $i;
		}
		
		return $this->DELIM_BEGIN . implode($this->DELIM_SET, $result) . $this->DELIM_END;
	}
	
	/**
	 * Given a project array, render its string encoding.
	 * 
	 * @param array $project
	 * @return string
	 */
	protected function renderProject(array $project) {
		$result = '';
	
		// Do we need to output the type string? As an optimization we do not
		// output the type string if it's the first type, or if the previous
		// type was present.
		$needTypeQualifier = false;
		
		$length = count($this->SET);
		for($i = 0; $i < $length; $i++) {
			if(isset($project[$this->SET[$i]])) {
				$data = $project[$this->SET[$i]];
				
				if($needTypeQualifier) {
					$result .= $this->SET[$i];
				}
				$result .= $this->renderDataType($data);
				$needTypeQualifier = false;
			} else {
				$needTypeQualifier = true;
			}
		}
		
		return $result;
	}
	
	/**
	 * Generates the URL parameter string for the current internal extensible data state.
	 * 
	 * @return string
	 */
	public function renderUrlString() {
		$result = '';
		
		foreach($this->projectData as $projectId => $project) {
			$result .= $projectId . $this->renderProject($project);
		}
		
		return $result;
	}
	
}

?>