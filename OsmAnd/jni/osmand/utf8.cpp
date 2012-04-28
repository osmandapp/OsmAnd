///////////////////////////////////////////////////////////////////
//
// Fast, small, simple, robust UTF-8 decoder in C
//
// Copyright (c) 2008-2009 Bjoern Hoehrmann <bjo...@hoehrmann.de>
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
///////////////////////////////////////////////////////////////////

#include <stdio.h>
#include <stdlib.h>

#ifdef _MSC_VER
typedef unsigned __int8  uint8_t;
typedef unsigned __int32 uint32_t;
#else
#include <stdint.h>
#endif

#define ASCII_IN_TABLE 1

/*
  The first 128 entries are tuples of 4 bit values. The lower bits
  are a mask that when xor'd with a byte removes the leading utf-8
  bits. The upper bits are a character class number. The remaining
  160 entries are a minimal deterministic finite automaton. It has
  10 states and each state has 13 character class transitions, and
  3 unused transitions for padding reasons. When the automaton en-
  ters state zero, it has found a complete valid utf-8 code point;
  if it enters state one then the input sequence is not utf-8. The
  start state is state nine. Note the mixture of octal and decimal
  for stylistic reasons. The ASCII_IN_TABLE macro makes the array
  bigger and the code simpler--but not necessarily faster--if set.
*/

static const uint8_t utf8d[] = {

#if ASCII_IN_TABLE
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
#endif

  070,070,070,070,070,070,070,070,070,070,070,070,070,070,070,070,
  050,050,050,050,050,050,050,050,050,050,050,050,050,050,050,050,
  030,030,030,030,030,030,030,030,030,030,030,030,030,030,030,030,
  030,030,030,030,030,030,030,030,030,030,030,030,030,030,030,030,
  204,204,188,188,188,188,188,188,188,188,188,188,188,188,188,188,
  188,188,188,188,188,188,188,188,188,188,188,188,188,188,188,188,
  174,158,158,158,158,158,158,158,158,158,158,158,158,142,126,126,
  111, 95, 95, 95, 79,207,207,207,207,207,207,207,207,207,207,207,

  0,1,1,1,8,7,6,4,5,4,3,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,1,1,1,1,1,1,1,1,1,1,1,1,
  1,4,4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4,4,4,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,4,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,8,7,6,4,5,4,3,2,1,1,1,1,

};
int nextWord(uint8_t* s) {
  uint8_t data, byte, stat = 9;
  uint32_t unic = 0;
  uint8_t counter = 0;

  // http://lists.w3.org/Archives/Public/www-archive/2009Apr/0000

  while ((byte = *s++)) {
	  counter ++;

    data = utf8d[ byte ];
    stat = utf8d[ 256 + (stat << 4) + (data >> 4) ];
    byte = (byte ^ (uint8_t)(data << 4));

    unic = (unic << 6) | byte;
	#if ASCII_IN_TABLE
    	data = utf8d[ byte ];
    	stat = utf8d[ 256 + (stat << 4) + (data >> 4) ];
    	byte = (byte ^ (uint8_t)(data << 4));
	#else
    	if (byte < 0x80) {
    		stat = utf8d[ 128 + (stat << 4) ];
    	} else {
    		data = utf8d[ byte ];
    		stat = utf8d[ 128 + (stat << 4) + (data >> 4) ];
    		byte = (byte ^ (uint8_t)(data << 4));
    	}
	#endif

    if (!stat) {
    	// find delimeters (is whitespace)
    	if(unic == ' ' || unic == '\t') {
    		return counter;
    	}
      // unic is now a proper code point, we just print it out.
      unic = 0;
    }

    if (stat == 1) {
      // the byte is not allowed here; the state would have to
      // be reset to continue meaningful reading of the string
    }

  }
  return -1;
}
