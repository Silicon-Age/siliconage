package com.siliconage.util;

import org.apache.commons.lang3.StringUtils;

public enum Trinary {
	FALSE,
	UNKNOWN,
	TRUE;
	
	public boolean asBooleanPrimitive(boolean argUnknownValue) {
		switch (this) {
			case FALSE: return false;
			case UNKNOWN: return argUnknownValue;
			case TRUE: return true;
			default: throw new IllegalStateException();
		}
	}

	public Boolean asBoolean(Boolean argUnknownValue) {
		switch (this) {
			case FALSE: return false;
			case UNKNOWN: return argUnknownValue; // Could be null
			case TRUE: return true;
			default: throw new IllegalStateException();
		}
	}

	public Trinary not() {
		switch (this) {
			case FALSE: return TRUE;
			case UNKNOWN: return UNKNOWN;
			case TRUE: return FALSE;
			default: throw new IllegalStateException();
		}
	}
	
	public Trinary and(Trinary argT) {
		if (argT == null) {
			return null;
		}
		if (this == FALSE || argT == FALSE) {
			return FALSE;
		} else if (this == TRUE && argT == TRUE) {
			return TRUE;
		} else {
			return UNKNOWN;
		}
	}
	
	public Trinary and(Boolean argB) {
		return and(valueOf(argB));
	}
	
	public Trinary and(boolean argB) {
		return and(valueOf(argB));
	}
	
	public Trinary or(Trinary argT) {
		if (argT == null) {
			return null;
		}
		if (this == TRUE || argT == TRUE) {
			return TRUE;
		} else if (this == FALSE && argT == FALSE) {
			return FALSE;
		} else {
			return UNKNOWN;
		}
	}
	
	public Trinary or(Boolean argB) {
		return or(valueOf(argB));
	}
	
	public Trinary or(boolean argB) {
		return or(valueOf(argB));
	}
	
	public static Trinary fromString(String argString) {
		if (argString == null) {
			return UNKNOWN;
		} else {
			return valueOf((Object) argString);
		}
	}
	
	public static Trinary fromStringCaseInsensitive(String argString) {
		return StringUtils.isBlank(argString) ? UNKNOWN : valueOf(argString.toUpperCase());
	}
	
	public static Trinary valueOf(Boolean argBoolean) {
		return argBoolean == null ? UNKNOWN : valueOf(argBoolean.booleanValue());
	}
	
	public static Trinary valueOf(boolean argBoolean) {
		return argBoolean ? TRUE : FALSE;
	}
	
	public static Trinary valueOf(Object argO) {
		if (argO == null) {
			return UNKNOWN;
		} else if (argO instanceof Trinary) {
			return (Trinary) argO;
		} else if (argO instanceof Boolean) {
			return valueOf((Boolean) argO);
		} else if (argO instanceof String) {
			return valueOf((String) argO);
		} else {
			return valueOf(argO.toString()); // which may not work
		}
	}
	
	/**
	 * This compares {@code Trinary}s in the order {@code TRUE}, {@code FALSE}, {@code UNKNOWN}.
	 */
	public static class StandardComparator extends NullSafeComparator<Trinary> {
		private static final StandardComparator ourInstance = new StandardComparator();
		
		public static StandardComparator getInstance() {
			return ourInstance;
		}
		
		private StandardComparator() {
			super();
		}
		
		@Override
		protected int compareInternal(Trinary argA, Trinary argB) {
			switch (argA) {
				case TRUE:
					switch (argB) {
						case TRUE: return 0;
						case FALSE: return -1;
						case UNKNOWN: return -1;
						default: throw new IllegalStateException();
					}
				case FALSE:
					switch (argB) {
						case TRUE: return 1;
						case FALSE: return 0;
						case UNKNOWN: return -1;
						default: throw new IllegalStateException();
					}
				case UNKNOWN:
					switch (argB) {
						case TRUE: return 1;
						case FALSE: return 1;
						case UNKNOWN: return 0;
						default: throw new IllegalStateException();
					}
				default: throw new IllegalStateException();
			}
		}
	}
}

