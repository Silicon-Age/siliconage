package com.siliconage.util;

/**
 * Contains methods used to scrub text entered on a website for storage in a
 * database and to scrub text from a database for display on a web page.
 * <P>
 * All non-private methods are static and final.
 * <P>
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:ben@silicon-age.com">Ben Jackson</a>
 * @author	<a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
 * @author	<a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class WebDataFilter {
	public static final String NO_NAMED_ENTITY = "";
	public static final String NO_NUMBERED_ENTITY = "";
	
	public static final String QUOTE_NAMED_ENTITY = "&quot;";
	public static final String QUOTE_NUMBERED_ENTITY = getNumberedEntity('"');
	
	public static final String AMPERSAND_NAMED_ENTITY = "&amp;";
	public static final String AMPERSAND_NUMBERED_ENTITY = getNumberedEntity('&');
	
	public static final String LESS_THAN_NAMED_ENTITY = "&lt;";
	public static final String LESS_THAN_NUMBERED_ENTITY = getNumberedEntity('<');
	
	public static final String GREATER_THAN_NAMED_ENTITY = "&gt;";
	public static final String GREATER_THAN_NUMBERED_ENTITY = getNumberedEntity('>');
	
	/**
	 * Returns a String representing the named entity encoding in HTML for the
	 * specified character, if one exists. Otherwise, returns the
	 * NO_NAMED_ENTITY String constant, which is an empty String.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param argChar the character to be tested.
	 * @return the named entity encoding if it exists; {@code NO_NAMED_ENTITY} otherwise.
	 */
	public static String getNamedEntity(char argChar) {
		if (isSafeCharacter(argChar)) {
			return NO_NAMED_ENTITY;
		}
		
		switch (argChar) {
			case '<':
				return LESS_THAN_NAMED_ENTITY;
			case '>':
				return GREATER_THAN_NAMED_ENTITY;
			case '"':
				return QUOTE_NAMED_ENTITY;
			case '&':
				return AMPERSAND_NAMED_ENTITY;
			case 0x00A0:
				// non-breaking space
				return "&nbsp;";
			case 0x00A1:
				// inverted exclamation mark
				return "&iexcl;";
			case 0x00A2:
				// cent sign
				return "&cent;";
			case 0x00A3:
				// pound sign
				return "&pound;";
			case 0x00A4:
				// currency sign
				return "&curren;";
			case 0x00A5:
				// yen sign
				return "&yen;";
			case 0x00A6:
				// broken vertical bar
				return "&brvbar;";
			case 0x00A7:
				// section sign
				return "&sect;";
			case 0x00A8:
				// spacing diaeresis
				return "&uml;";
			case 0x00A9:
				// copyright sign
				return "&copy;";
			case 0x00AA:
				// feminine ordinal indicator
				return "&ordf;";
			case 0x00AB:
				// left-pointing double angle quotation mark
				return "&laquo;";
			case 0x00AC:
				// not sign
				return "&not;";
			case 0x00AD:
				// soft hyphen / discretionary hyphen
				return "&shy;";
			case 0x00AE:
				// registered trade mark sign
				return "&reg;";
			case 0x00AF:
				// spacing macron / overline / APL overbar
				return "&macr;";
			case 0x00B0:
				// degree sign
				return "&deg;";
			case 0x00B1:
				// plus-or-minus sign
				return "&plusmn;";
			case 0x00B2:
				// superscript two / squared
				return "&sup2;";
			case 0x00B3:
				// superscript three / cubed
				return "&sup3;";
			case 0x00B4:
				// acute accent
				return "&acute;";
			case 0x00B5:
				// micro sign
				return "&micro;";
			case 0x00B6:
				// paragraph sign
				return "&para;";
			case 0x00B7:
				// middle dot / Georgian comma
				// / Greek middle dot
				return "&middot;";
			case 0x00B8:
				// cedilla
				return "&cedil;";
			case 0x00B9:
				// superscript one
				return "&sup1;";
			case 0x00BA:
				// masculine ordinal indicator
				return "&ordm;";
			case 0x00BB:
				// right-pointing double angle quotation mark
				return "&raquo;";
			case 0x00BC:
				// fraction one quarter
				return "&frac14;";
			case 0x00BD:
				// fraction one half
				return "&frac12;";
			case 0x00BE:
				// fraction three quarters
				return "&frac34;";
			case 0x00BF:
				// inverted question mark
				return "&iquest;";
			case 0x00C0:
				// latin capital letter A with grave
				return "&Agrave;";
			case 0x00C1:
				// latin capital letter A with acute
				return "&Aacute;";
			case 0x00C2:
				// latin capital letter A with circumflex
				return "&Acirc;";
			case 0x00C3:
				// latin capital letter A with tilde
				return "&Atilde;";
			case 0x00C4:
				// latin capital letter A with diaeresis
				return "&Auml;";
			case 0x00C5:
				// latin capital letter A with ring above
				return "&Aring;";
			case 0x00C6:
				// latin capital letter AE
				return "&AElig;";
			case 0x00C7:
				// latin capital letter C with cedilla
				return "&Ccedil;";
			case 0x00C8:
				// latin capital letter E with grave
				return "&Egrave;";
			case 0x00C9:
				// latin capital letter E with acute
				return "&Eacute;";
			case 0x00CA:
				// latin capital letter E with circumflex
				return "&Ecirc;";
			case 0x00CB:
				// latin capital letter E with diaeresis
				return "&Euml;";
			case 0x00CC:
				// latin capital letter I with grave
				return "&Igrave;";
			case 0x00CD:
				// latin capital letter I with acute
				return "&Iacute;";
			case 0x00CE:
				// latin capital letter I with circumflex
				return "&Icirc;";
			case 0x00CF:
				// latin capital letter I with diaeresis
				return "&Iuml;";
			case 0x00D0:
				// latin capital letter ETH
				return "&ETH;";
			case 0x00D1:
				// latin capital letter N with tilde
				return "&Ntilde;";
			case 0x00D2:
				// latin capital letter O with grave
				return "&Ograve;";
			case 0x00D3:
				// latin capital letter O with acute
				return "&Oacute;";
			case 0x00D4:
				// latin capital letter O with circumflex
				return "&Ocirc;";
			case 0x00D5:
				// latin capital letter O with tilde
				return "&Otilde;";
			case 0x00D6:
				// latin capital letter O with diaeresis
				return "&Ouml;";
			case 0x00D7:
				// multiplication sign
				return "&times;";
			case 0x00D8:
				// latin capital letter O with stroke
				return "&Oslash;";
			case 0x00D9:
				// latin capital letter U with grave
				return "&Ugrave;";
			case 0x00DA:
				// latin capital letter U with acute
				return "&Uacute;";
			case 0x00DB:
				// latin capital letter U with circumflex
				return "&Ucirc;";
			case 0x00DC:
				// latin capital letter U with diaeresis
				return "&Uuml;";
			case 0x00DD:
				// latin capital letter Y with acute
				return "&Yacute;";
			case 0x00DE:
				// latin capital letter THORN
				return "&THORN;";
			case 0x00DF:
				// latin small letter sharp s / ess-zed
				return "&szlig ;";
			case 0x00E0:
				// latin small letter a with grave
				return "&agrave;";
			case 0x00E1:
				// latin small letter a with acute
				return "&aacute;";
			case 0x00E2:
				// latin small letter a with circumflex
				return "&acirc;";
			case 0x00E3:
				// latin small letter a with tilde
				return "&atilde;";
			case 0x00E4:
				// latin small letter a with diaeresis
				return "&auml;";
			case 0x00E5:
				// latin small letter a with ring above
				return "&aring;";
			case 0x00E6:
				// latin small letter ae
				return "&aelig;";
			case 0x00E7:
				// latin small letter c with cedilla
				return "&ccedil;";
			case 0x00E8:
				// latin small letter e with grave
				return "&egrave;";
			case 0x00E9:
				// latin small letter e with acute
				return "&eacute;";
			case 0x00EA:
				// latin small letter e with circumflex
				return "&ecirc;";
			case 0x00EB:
				// latin small letter e with diaeresis
				return "&euml;";
			case 0x00EC:
				// latin small letter i with grave
				return "&igrave;";
			case 0x00ED:
				// latin small letter i with acute
				return "&iacute;";
			case 0x00EE:
				// latin small letter i with circumflex
				return "&icirc;";
			case 0x00EF:
				// latin small letter i with diaeresis
				return "&iuml;";
			case 0x00F0:
				// latin small letter eth
				return "&eth;";
			case 0x00F1:
				// latin small letter n with tilde
				return "&ntilde;";
			case 0x00F2:
				// latin small letter o with grave
				return "&ograve;";
			case 0x00F3:
				// latin small letter o with acute
				return "&oacute;";
			case 0x00F4:
				// latin small letter o with circumflex
				return "&ocirc;";
			case 0x00F5:
				// latin small letter o with tilde
				return "&otilde;";
			case 0x00F6:
				// latin small letter o with diaeresis
				return "&ouml;";
			case 0x00F7:
				// division sign
				return "&divide;";
			case 0x00F8:
				// latin small letter o with stroke
				return "&oslash;";
			case 0x00F9:
				// latin small letter u with grave
				return "&ugrave;";
			case 0x00FA:
				// latin small letter u with acute
				return "&uacute;";
			case 0x00FB:
				// latin small letter u with circumflex
				return "&ucirc;";
			case 0x00FC:
				// latin small letter u with diaeresis
				return "&uuml;";
			case 0x00FD:
				// latin small letter y with acute
				return "&yacute;";
			case 0x00FE:
				// latin small letter thorn
				return "&thorn;";
			case 0x00FF:
				// latin small letter y with diaeresis
				return "&yuml;";
			case 0x0152:
				// latin capital ligature OE
				return "&OElig;";
			case 0x0153:
				// latin small ligature oe
				return "&oelig;";
			case 0x0160:
				// latin capital letter S with caron
				return "&Scaron;";
			case 0x0161:
				// latin small letter s with caron
				return "&scaron;";
			case 0x0178:
				// latin capital letter Y with diaeresis
				return "&Yuml;";
			case 0x0192:
				// latin small f with hook / function / florin
				return "&fnof;";
			case 0x02C6:
				// modifier letter circumflex accent
				return "&circ;";
			case 0x02DC:
				// small tilde
				return "&tilde;";
			case 0x0391:
				// greek capital letter alpha
				return "&Alpha;";
			case 0x0392:
				// greek capital letter beta
				return "&Beta;";
			case 0x0393:
				// greek capital letter gamma
				return "&Gamma;";
			case 0x0394:
				// greek capital letter delta
				return "&Delta;";
			case 0x0395:
				// greek capital letter epsilon
				return "&Epsilon;";
			case 0x0396:
				// greek capital letter zeta
				return "&Zeta;";
			case 0x0397:
				// greek capital letter eta
				return "&Eta;";
			case 0x0398:
				// greek capital letter theta
				return "&Theta;";
			case 0x0399:
				// greek capital letter iota
				return "&Iota;";
			case 0x039A:
				// greek capital letter kappa
				return "&Kappa;";
			case 0x039B:
				// greek capital letter lambda
				return "&Lambda;";
			case 0x039C:
				// greek capital letter mu
				return "&Mu;";
			case 0x039D:
				// greek capital letter nu
				return "&Nu;";
			case 0x039E:
				// greek capital letter xi
				return "&Xi;";
			case 0x039F:
				// greek capital letter omicron
				return "&Omicron;";
			case 0x03A0:
				// greek capital letter pi
				return "&Pi;";
			case 0x03A1:
				// greek capital letter rho
				return "&Rho;";
			// There is no Sigmaf, and no 0x03A2 character either
			case 0x03A3:
				// greek capital letter sigma
				return "&Sigma;";
			case 0x03A4:
				// greek capital letter tau
				return "&Tau;";
			case 0x03A5:
				// greek capital letter upsilon
				return "&Upsilon;";
			case 0x03A6:
				// greek capital letter phi
				return "&Phi;";
			case 0x03A7:
				// greek capital letter chi
				return "&Chi;";
			case 0x03A8:
				// greek capital letter psi
				return "&Psi;";
			case 0x03A9:
				// greek capital letter omega
				return "&Omega;";
			case 0x03B1:
				// greek small letter alpha
				return "&alpha;";
			case 0x03B2:
				// greek small letter beta
				return "&beta;";
			case 0x03B3:
				// greek small letter gamma
				return "&gamma;";
			case 0x03B4:
				// greek small letter delta
				return "&delta;";
			case 0x03B5:
				// greek small letter epsilon
				return "&epsilon;";
			case 0x03B6:
				// greek small letter zeta
				return "&zeta;";
			case 0x03B7:
				// greek small letter eta
				return "&eta;";
			case 0x03B8:
				// greek small letter theta
				return "&theta;";
			case 0x03B9:
				// greek small letter iota
				return "&iota;";
			case 0x03BA:
				// greek small letter kappa
				return "&kappa;";
			case 0x03BB:
				// greek small letter lambda
				return "&lambda;";
			case 0x03BC:
				// greek small letter mu
				return "&mu;";
			case 0x03BD:
				// greek small letter nu
				return "&nu;";
			case 0x03BE:
				// greek small letter xi
				return "&xi;";
			case 0x03BF:
				// greek small letter omicron
				return "&omicron;";
			case 0x03C0:
				// greek small letter pi
				return "&pi;";
			case 0x03C1:
				// greek small letter rho
				return "&rho;";
			case 0x03C2:
				// greek small letter final sigma
				return "&sigmaf;";
			case 0x03C3:
				// greek small letter sigma
				return "&sigma;";
			case 0x03C4:
				// greek small letter tau
				return "&tau;";
			case 0x03C5:
				// greek small letter upsilon
				return "&upsilon;";
			case 0x03C6:
				// greek small letter phi
				return "&phi;";
			case 0x03C7:
				// greek small letter chi
				return "&chi;";
			case 0x03C8:
				// greek small letter psi
				return "&psi;";
			case 0x03C9:
				// greek small letter omega
				return "&omega;";
			case 0x03D1:
				// greek small letter theta symbol
				return "&thetasym;";
			case 0x03D2:
				// greek upsilon with hook symbol
				return "&upsih;";
			case 0x03D6:
				// greek pi symbol
				return "&piv;";
			case 0x2002:
				// en space
				return "&ensp;";
			case 0x2003:
				// em space
				return "&emsp;";
			case 0x2009:
				// thin space
				return "&thinsp;";
			case 0x200C:
				// zero width non-joiner
				return "&zwnj;";
			case 0x200D:
				// zero width joiner
				return "&zwj;";
			case 0x200E:
				// left-to-right mark
				return "&lrm;";
			case 0x200F:
				// right-to-left mark
				return "&rlm;";
			case 0x2013:
				// en dash
				return "&ndash;";
			case 0x2014:
				// em dash
				return "&mdash;";
			case 0x2018:
				// left single quotation mark
				return "&lsquo;";
			case 0x2019:
				// right single quotation mark
				return "&rsquo;";
			case 0x201A:
				// single low-9 quotation mark
				return "&sbquo";
			case 0x201C:
				// left double quotation mark
				return "&ldquo;";
			case 0x201D:
				// right double quotation mark
				return "&rdquo;";
			case 0x201E:
				// double low-9 quotation mark
				return "&bdquo;";
			case 0x2020:
				// dagger
				return "&dagger;";
			case 0x2021:
				// double dagger
				return "&Dagger;";
			case 0x2022:
				// bullet
				// NOTE: bullet is NOT the same as bullet operator, 0x2219
				return "&bull;";
			case 0x2026:
				// horizontal ellipsis
				return "&hellip;";
			case 0x2030:
				// per mille sign
				return "&permil;";
			case 0x2032:
				// prime / minutes / feet
				return "&prime;";
			case 0x2033:
				// double prime / seconds / inches
				return "&Prime;";
			case 0x2039:
				// single left-pointing angle quotation mark
				// NOTE: lsaquo is proposed but not yet ISO standardized.
				return "&lsaquo;";
			case 0x203A:
				// single right-pointing angle quotation mark
				// NOTE: rsaquo is proposed but not yet ISO standardized.
				return "&rsaquo;";
			case 0x203E:
				// overline / spacing overscore
				return "&oline;";
			case 0x2044:
				// fraction slash
				return "&frasl;";
			case 0x20AC:
				// euro sign
				return "&euro;";
			case 0x2118:
				// script capital P / power set / Weierstrass p
				return "&weierp;";
			case 0x2111:
				// blackletter capital I / imaginary part
				return "&image;";
			case 0x211C:
				// blackletter capital R / real part symbol
				return "&real;";
			case 0x2122:
				// trade mark sign
				return "&trade;";
			case 0x2135:
				// alef symbol / first transfinite cardinal
				// NOTE: alef symbol is NOT the same as hebrew
				// letter alef (0x05D0) although the same glyph
				// could be used to depict both characters
				return "&alefsym;";
			case 0x2190:
				// leftwards arrow
				return "&larr;";
			case 0x2191:
				// upwards arrow
				return "&uarr;";
			case 0x2192:
				// rightwards arrow
				return "&rarr;";
			case 0x2193:
				// downwards arrow
				return "&darr;";
			case 0x2194:
				// left right arrow
				return "&harr;";
			case 0x21B5:
				// downwards arrow with corner leftwards
				// / carriage return
				return "&crarr;";
			case 0x21D0:
				// leftwards double arrow
				// NOTE: ISO 10646 does not say that lArr is the
				// same as the 'is implied by' arrow but also
				// does not have any other character for that
				// function. So ? lArr can be used for 'is
				// implied by' as ISOtech suggests.
				return "&lArr;";
			case 0x21D1:
				// upwards double arrow
				return "&uArr;";
			case 0x21D2:
				// rightwards double arrow
				// NOTE: ISO 10646 does not say that rArr is the
				// 'implies' character but does not have any
				// other character for that function. So ? rArr
				// can be used for 'implies' as ISOtech
				// suggests.
				return "&rArr;";
			case 0x21D3:
				// downwards double arrow
				return "&dArr;";
			case 0x21D4:
				// left right double arrow
				return "&hArr;";
			case 0x2200:
				// for all
				return "&forall;";
			case 0x2202:
				// partial differential
				return "&part;";
			case 0x2203:
				// there exists
				return "&exist;";
			case 0x2205:
				// empty set / null set / diameter
				return "&empty;";
			case 0x2207:
				// nabla = backward difference
				return "&nabla;";
			case 0x2208:
				// element of
				return "&isin;";
			case 0x2209:
				// not an element of
				return "&notin;";
			case 0x220B:
				// contains as member
				return "&ni;";
			case 0x220F:
				// n-ary product / product sign
				// NOTE: prod is NOT the same character as
				// 0x03A0 'greek capital letter pi' though the
				// same glyph might be used for both.
				return "&prod;";
			case 0x2211:
				// n-ary sumation
				// NOTE: sum is NOT the same character as 0x03A3
				// 'greek capital letter sigma' though the same
				// glyph might be used for both.
				return "&sum;";
			case 0x2212:
				// minus sign
				return "&minus;";
			case 0x2217:
				// asterisk operator
				return "&lowast;";
			case 0x221A:
				// square root / radical sign
				return "&radic;";
			case 0x221D:
				// proportional to
				return "&prop;";
			case 0x221E:
				// infinity
				return "&infin;";
			case 0x2220:
				// angle
				return "&ang;";
			case 0x2227:
				// logical and / wedge
				return "&and;";
			case 0x2228:
				// logical or / vee
				return "&or;";
			case 0x2229:
				// intersection / cap
				return "&cap;";
			case 0x222A:
				// union / cup
				return "&cup;";
			case 0x222B:
				// integral
				return "&int;";
			case 0x2234:
				// therefore
				return "&there4;";
			case 0x223C:
				// tilde operator / varies with / similar to
				// NOTE: tilde operator is NOT the same
				// character as the tilde (0x007E) although the
				// same glyph might be used to represent both.
				return "&sim;";
			case 0x2245:
				// approximately equal to
				return "&cong;";
			case 0x2248:
				// almost equal to / asymptotic to
				return "&asymp;";
			case 0x2260:
				// not equal to
				return "&ne;";
			case 0x2261:
				// identical to
				return "&equiv;";
			case 0x2264:
				// less-than or equal to
				return "&le;";
			case 0x2265:
				// greater-than or equal to
				return "&ge;";
			case 0x2282:
				// subset of
				return "&sub;";
			case 0x2283:
				// superset of
				return "&sup;";
			case 0x2284:
				// not a subset of
				return "&nsub;";
			case 0x2286:
				// subset of or equal to
				return "&sube;";
			case 0x2287:
				// superset of or equal to
				return "&supe;";
			case 0x2295:
				// circled plus / direct sum
				return "&oplus;";
			case 0x2297:
				// circled times / vector product
				return "&otimes;";
			case 0x22A5:
				// up tack / orthogonal to / perpendicular
				return "&perp;";
			case 0x22C5:
				// dot operator
				// NOTE: dot operator is NOT the same character
				// as 0x00B7 middle dot -->
				return "&sdot;";
			case 0x2308:
				// left ceiling / apl upstile
				return "&lceil;";
			case 0x2309:
				// right ceiling
				return "&rceil;";
			case 0x230A:
				// left floor / apl downstile
				return "&lfloor;";
			case 0x230B:
				// right floor
				return "&rfloor;";
			case 0x2329:
				// left-pointing angle bracket / bra
				// NOTE: lang is NOT the same character as
				// 0x003C 'less than' or 0x2039 'single
				// left-pointing angle quotation mark'.
				return "&lang;";
			case 0x232A:
				// right-pointing angle bracket / ket
				// NOTE: rang is NOT the same character as
				// 0x003E 'greater than' or 0x203A 'single
				// right-pointing angle quotation mark'.
				return "&rang;";
			case 0x25CA:
				// lozenge
				return "&loz;";
			case 0x2660:
				// black spade suit
				return "&spades;";
			case 0x2663:
				// black club suit / shamrock
				return "&clubs;";
			case 0x2665:
				// black heart suit / valentine
				return "&hearts;";
			case 0x2666:
				// black diamond suit
				return "&diams;";
			default:
				// Doesn't have a named entity
				return NO_NAMED_ENTITY;
		}
	}
	
	/**
	 * Returns a String representing the numbered entity encoding in HTML
	 * for the specified character, if one exists. Otherwise, returns the
	 * NO_NUMBERED_ENTITY String constant, which is an empty String.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param argChar the character to be tested.
	 * @return the numbered entity encoding if it exists; NO_NUMBERED_ENTITY otherwise.
	 */
	public static String getNumberedEntity(char argChar) {
		if (isUnusedCharacter(argChar)) {
			return NO_NUMBERED_ENTITY;
		} else {
			// Construct the numbered entity based on the decimal
			// value of the character
			return "&#" + ((long) argChar) + ";";
		}
	}
	
	/**
	 * Returns {@code true} if the character is a reserved HTML
	 * character ('&lt;', '&gt;', '&quot;', '&amp;').
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argChar the character to be tested.
	 * @return {@code true} if argChar is a reserved HTML
	 *         character; {@code false} otherwise.
	 */
	public static boolean isReservedHTMLCharacter(char argChar) {
		if (argChar == '<'	|| argChar == '>'
							|| argChar == '"'
							|| argChar == '&') {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns {@code true} if the character can be safely displayed
	 * without needing to be encoded by a numbered or named entity.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argChar the character to be tested.
	 * @return {@code true} if argChar can be safely displayed
	 *         without needing to be encoded by a numbered or named
	 *         entity; {@code false} otherwise.
	 */
	public static boolean isSafeCharacter(char argChar) {
		if (isUnusedCharacter(argChar)) {
			return true;
		} else if (isReservedHTMLCharacter(argChar)) {
			return false;
		} else if (argChar >= 0x0020 && argChar <= 0x007e) {
			// Digits, letters, punctuation
			return true;
		} else {
			// Everything else
			return false;
		}
	}
	
	/**
	 * Returns {@code true} if the character is an unused character in
	 * HTML. These include certain ASCII characters which are non-printing
	 * or are control characters.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argChar
	 *         The character to be tested.
	 * @return {@code true} if argChar is unused character
	 *         in HTML; {@code false} otherwise.
	 */
	public static boolean isUnusedCharacter(char argChar) {
		if (argChar <= 0x001f) {
			// In the first 32 characters of ASCII
//			return (argChar != 0x0009		// Horizontal Tab
//					&& argChar != 0x000a	// Line Break
//					&& argChar != 0x000d);	// Carriage Return
			return true;
		} else if (argChar >= 0x007f && argChar <= 0x009f) {
			// Control characters (ASCII 127-159)
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Scrubs the argument String for HTML.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argString the String to be scrubbed.
	 * @return The String with all characters replaced by any necessary
	 *         named entities.
	 */
	public static final String scrub(String argString) {
		if (argString == null) {
			return "";
		}
		
		int lclLength = argString.length();
		
		StringBuilder lclScrubbedString = new StringBuilder(lclLength);
		
		for (int i = 0; i < lclLength; i++) {
			char lclChar = argString.charAt(i);
			
			if (isSafeCharacter(lclChar)) {
				lclScrubbedString.append(lclChar);
			} else {
				String lclNamedEntity = getNamedEntity(lclChar);
				
				if (NO_NAMED_ENTITY.equals(lclNamedEntity)) {
					// No Named Entity -- append the character (RISKY!)
					lclScrubbedString.append(lclChar);
				} else {
					lclScrubbedString.append(lclNamedEntity);
				}
			}
		}
		return lclScrubbedString.toString();
	}
	
	/**
	 * Takes an argument Object and cleans it so that it can be safely displayed
	 * on a web page. If the argument String is <code>null</code>, returns an
	 * empty String.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argObject the object to escape
	 * @return an HTML-display-escaped version of the argument
	 */
	public static final String scrubForHTMLDisplay(Object argObject) {
		if (argObject == null) {
			return "";
		} else {
			return scrubForHTMLDisplay(argObject.toString());
		}
	}
	
	/**
	 * Takes an argument String and cleans it so that it can be safely
	 * displayed on a web page. If the argument String is <code>null</code>,
	 * <code>null</code> is returned.
	 * @author <a href="mailto:ben@silicon-age.com">Ben Jackson</a>
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 * @param  argString the string to escape
	 * @return an HTML-display-escaped version of the argument
	 */
	public static final String scrubForHTMLDisplay(String argString) {
		if (argString == null) {
			return null;
		}
		
		String lclDirty = scrubForStorage(argString);
		StringBuilder lclClean = new StringBuilder((int) (lclDirty.length() * 1.10));
		
		for (int i = 0; i<lclDirty.length(); i++) {
			char lclInChar = lclDirty.charAt(i);
			
			switch (lclInChar) {
			case '\n': lclClean.append("<br />\n"); break;
			case '\r': break;
			case 0x00a7: lclClean.append("&sect;"); break;
			case 0x00d2: case 0x00e2: lclClean.append("&reg;"); break;
			case 0x00d3: case 0x00e3: lclClean.append("&copy;"); break;
			case '"': lclClean.append("&quot;"); break;
			case '<': lclClean.append("&lt;"); break;
			case '>': lclClean.append("&gt;"); break;
//			case '&': lclClean.append("&amp;"); break;
			default: lclClean.append(lclInChar);
			}
		}
		return lclClean.toString();
	}
	
	/**
	 * Takes an argument Object and cleans it so that it can be safely displayed
	 * in a text box on a web page. If the argument String is <code>null</code>,
	 * returns an empty String.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argObject the Object to be scrubbed.
	 * @return an HTML-form-escaped version of the argument
	 */
	public static final String scrubForHTMLFormDisplay(Object argObject) {
		if (argObject == null) {
			return "";
		} else {
			return scrubForHTMLFormDisplay(argObject.toString());
		}
	}
	
	/**
	 * Takes an argument String and cleans it so that it can be safely
	 * displayed in a text box (input, textarea, or otherwise).
	 * If the argument String is
	 * <code>null</code>, <code>null</code> is returned.
	 * @author <a href="mailto:ben@silicon-age.com">Ben Jackson</a>
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 * @param  argString the string to escape
	 * @return an HTML-form-escaped version of the argument
	 */
	public static final String scrubForHTMLFormDisplay(String argString) {
		if (argString == null) {
			return ""; // used to return null, but I don't see why. -cgn
		}
		
		String lclDirty = scrubForStorage(argString);
		StringBuilder lclClean = new StringBuilder((int) (argString.length() * 1.1));
		
		for (int i = 0; i<lclDirty.length(); i++) {
			char lclInChar = lclDirty.charAt(i);
			
			switch (lclInChar) {
			case 0x00a7: lclClean.append("&sect;"); break;
			case 0x00d2: case 0x00e2: lclClean.append("&reg;"); break;
			case 0x00d3: case 0x00e3: lclClean.append("&copy;"); break;
			case '"': lclClean.append("&quot;"); break;
			case '<': lclClean.append("&lt;"); break;
			case '>': lclClean.append("&gt;"); break;
//			case '&': lclClean.append("&amp;"); break;
			default: lclClean.append(lclInChar);
			}
		}
			
		return lclClean.toString();
	}
	
	/**
	 * Takes an argument Object and cleans it so that it can be safely used in
	 * JavaScript. If the argument String is <code>null</code>, returns an empty
	 * String.
	 * @author <a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
	 * @param  argObject the Object to be scrubbed.
	 * @return String
	 */
	public static final String scrubForJavaScript(Object argObject) {
		if (argObject == null) {
			return "";
		} else {
			return scrubForJavaScript(argObject.toString());
		}
	}
	
	/**
	 * Takes an argument String and cleans it so that it can be safely 
	 * used in JavaScript. If the argument String is <code>null</code>, 
	 * <code>null</code> is returned.
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @param argString the string to escape
	 * @return the JavaScript-escaped version of the argument
	 */
	public static final String scrubForJavaScript(String argString) {
		if (argString == null) {
			return null;
		}
		
		String lclDirty = argString;
		StringBuilder lclClean = new StringBuilder((int) (lclDirty.length() * 1.1));
		
		//variables to hold (partial or final) results
		char lclInChar;
		char lclOutChar;
		
		//loop over string and add scrubbed version to buffer
		for (int i = 0; i < lclDirty.length(); i++) {
		
			lclInChar = lclDirty.charAt(i);
			lclOutChar = lclInChar;
		
			if (lclInChar == 0x0091) {
				// Open single quote --> backslash and single quote
				lclClean.append("\\\'");
			} else if (lclInChar == 0x0092) {
				// Close single quote --> backslash and single quote
				lclClean.append("\\\'");
			} else if (lclInChar == 0x0027) {
				// Single quote
				lclClean.append("\\\'");
			} else if (lclInChar == 0x005c) {
				//backslash --> backslash
				lclClean.append("\\\\");
			} else if (lclInChar == 0x0022) {
				//double quote --> double quote
				lclClean.append("\\\"");
			} else {
				lclClean.append(lclOutChar);
			}
			
		}
		return lclClean.toString();
	}
	
	/**
	 * Takes an argument String that was entered from a website form and
	 * cleans it so that it can be safely stored in a database. If the
	 * argument String is <code>null</code>, <code>null</code> is returned.
	 * @author		<a href="mailto:ben@silicon-age.com">
	 *			Ben Jackson</a>
	 * @author		<a href="mailto:kubiwan@silicon-age.com">
	 *			Chad Kubicek</a>
	 * @param	argString the string to escape
	 * @return the database-escaped version of the argument
	 */
	public static final String scrubForStorage(String argString) {
		if (argString == null) {
			return null;
		}
		
		String lclDirty = argString;
		StringBuilder lclClean = new StringBuilder((int) (lclDirty.length() * 1.1));
		
		for (int i = 0; i < lclDirty.length(); i++) {
			char lclInChar = lclDirty.charAt(i);
			switch (lclInChar) {
				case 0x0091: lclClean.append('\''); break; // Open single quote --> single quote
				case 0x0092: lclClean.append('\''); break; // Close single quote --> single quote
				case 0x0093: lclClean.append('"'); break; // Open double quote --> double quote
				case 0x0094: lclClean.append('"'); break; // Close double quote --> double quote
				case 0x0096: lclClean.append('-'); break; // endash --> dash
				case 0x0097: lclClean.append('-'); break; // emdash --> dash;
				case 0x0085: lclClean.append("..."); break; // ellipsis --> "..."
				default: lclClean.append(lclInChar);
			}
		}
		return lclClean.toString();
	}
	
	/**
	  * Escapes a string so that it can be safely used as an ID in CSS code (e.g. {@code div#myId}).
	  * @author Jonah Greenthal
	  * @param argString the string to escape
	  * @return a CSS-ID-safe version of the argument
	  */
	public static final String scrubForCssId(String argString) {
		if (argString == null) {
			return null;
		}
		
		StringBuilder lclSB = new StringBuilder(argString.length() + argString.length()/10);
		for (char lclC : argString.toCharArray()) {
			switch (lclC) {
				case ' ':
				case '_':
				case '!':
				case '"':
				case '#':
				case '$':
				case '%':
				case '&':
				case '\'':
				case '(':
				case ')':
				case '*':
				case '+':
				case ',':
				case '-':
				case '.':
				case '/':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '`':
				case '{':
				case '|':
				case '}':
				case '~':
					lclSB.append('\\');
					// Fall through!
				default:
					lclSB.append(lclC);
			}
		}
		
		return lclSB.toString();
	}
}
