/*
******************************************************************************
* Copyright (C) 1996-2008, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

/* 
 * This is a port of the C++ class UConverterSelector. 
 *
 * Methods related to serialization are not ported in this version. In addition,
 * the selectForUTF8 method is not going to be ported, as UTF8 is seldom used
 * in Java.
 * 
 * @author Shaopeng Jia
 */

package com.ibm.icu.charset;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import com.ibm.icu.impl.PropsVectors;
import com.ibm.icu.impl.Trie;
import com.ibm.icu.text.UnicodeSet;

/**
 * Charset Selector
 * 
 * A charset selector is built with a list of charset names and given an input CharSequence
 * returns the list of names the corresponding charsets which can convert the CharSequence.
 *
 * @draft ICU 4.2
 */
public final class CharsetSelector {
	private Trie trie; // 16 bit trie containing offsets into pv
	private int[] pv;  // table of bits
	private int pvCount;
	private String[] encodings; // encodings users ask to use
	private int[] swapped;
	private boolean ownPv;
	private boolean ownEncodingStrings;
	
	private void generateSelectorData(PropsVectors pvec, 
			UnicodeSet excludedCodePoints, int mappingTypes) {
		int columns = (encodings.length + 31) / 32;
		
		// set errorValue to all-ones
		for (int col = 0; col < columns; ++col) {
			pvec.setValue(PropsVectors.PVEC_ERROR_VALUE_CP, 
					PropsVectors.PVEC_ERROR_VALUE_CP, col, ~0, ~0);
		}
		
		for (int i = 0; i < encodings.length; ++i) {
			Charset testCharset = CharsetICU.forNameICU(encodings[i]);
			UnicodeSet unicodePointSet = new UnicodeSet(); // empty set
			((CharsetICU) testCharset).getUnicodeSet(unicodePointSet, 
					mappingTypes);
			int column = i / 32;
			int mask = 1 << (i%32);
			// now iterate over intervals on set i
			int itemCount = unicodePointSet.getRangeCount();
			for (int j = 0; j < itemCount; ++j) {
				int startChar = unicodePointSet.getRangeStart(j);
				int endChar = unicodePointSet.getRangeEnd(j);
				pvec.setValue(startChar, endChar, column, ~0, mask);
			}
		}
		
		// handle excluded encodings
		// Simply set their values to all 1's in the pvec
		if (excludedCodePoints.size() > 0) {
			int itemCount = excludedCodePoints.getRangeCount();
			for (int j = 0; j < itemCount; ++j) {
				int startChar = excludedCodePoints.getRangeStart(j);
				int endChar = excludedCodePoints.getRangeEnd(j);
				for (int col = 0; col < columns; col++) {
					pvec.setValue(startChar, endChar, col, ~0, ~0);
				}
				
			}
		}
	}

   /**
    * Construct a CharsetSelector from a list of charset names.
    * @param charsetList a list of charset names in the form
    * of strings. If charsetList is empty, a selector for all 
    * available charset is constructed.
    * @param excludedCodePoints a set of code points to be excluded from 
    * consideration.
    * Excluded code points appearing in the input CharSequence do not 
    * change the selection result. It could be empty when no code point 
    * should be excluded.
    * @param mappingTypes an int which determines whether to consider 
    * only roundtrip mappings or also fallbacks, e.g. CharsetICU.ROUNDTRIP_SET.
    * See CharsetICU.java for the constants that are currently supported.
    * @throws IllegalArgumentException if the parameters is invalid. 
    * @throws IllegalCharsetNameException If the given charset name
    * is illegal.
    * @throws UnsupportedCharsetException If no support for the
    * named charset is available in this instance of the Java
    * virtual machine.
    * @draft ICU 4.2
    */
    public CharsetSelector(List charsetList, UnicodeSet excludedCodePoints, 
    		int mappingTypes) 
    throws IllegalArgumentException, IllegalCharsetNameException, 
           UnsupportedCharsetException {
    	if (mappingTypes != CharsetICU.ROUNDTRIP_AND_FALLBACK_SET &&
    		mappingTypes != CharsetICU.ROUNDTRIP_SET) {
    		throw new IllegalArgumentException("Unsupported mappingTypes");
    	}
    	
    	int encodingCount = charsetList.size();
    	if (encodingCount > 0) {
    		encodings = new String[encodingCount];
    		for (int i = 0; i < encodingCount; i++) {
    			encodings[i] = (String) charsetList.get(i);
    		}
    	} else {
    		Object[] availableNames = CharsetProviderICU.getAvailableNames();
    		encodingCount = availableNames.length;
    		encodings = new String[encodingCount];
    		for (int i = 0; i < encodingCount; i++) {
    			encodings[i] =  (String) availableNames[i];
    		}
    	}
    	
    	ownEncodingStrings = true;
    	PropsVectors pvec = new PropsVectors((encodingCount + 31) / 32);
    	generateSelectorData(pvec, excludedCodePoints, mappingTypes);
    }

   /**
    * Select charsets that can map all characters in a CharSequence,
    * ignoring the excluded code points.
    *
    * @param unicodeText a CharSequence. It could be empty.
    * @return a list that contains charset names in the form 
    * of strings. The returned encoding names and their order will be 
    * the same as supplied when building the selector.
    *
    * @draft ICU 4.2
    */    
    public List selectForString(CharSequence unicodeText);
}
