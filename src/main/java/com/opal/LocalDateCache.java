package com.opal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import javax.naming.InitialContext;

public abstract class LocalDateCache {
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(LocalDateCache.class.getName());

	private static LocalDate[] ourCache;
	private static int ourStartNumber;
	private static int ourEndNumber;
	
	static {
		initCache();
	}
	
	private static void initCache() {
		try {
			InitialContext lclIC = new InitialContext();
			
			String lclStartString = (String) lclIC.lookup("java:comp/env/opal/localdatecache/start");
			if (lclStartString == null) {
				ourLogger.info("No LocalDateCache start date provided; cache disabled.");
			}
			String lclEndString = (String) lclIC.lookup("java:comp/env/opal/localdatecache/end");
			if (lclEndString == null) {
				ourLogger.info("No LocalDateCache end date provided; cache disabled.");
			}
			if (lclStartString == null || lclEndString == null) {
				return;
			}
			
			LocalDate lclStart = LocalDate.parse(lclStartString);
			LocalDate lclEnd = LocalDate.parse(lclEndString);
			int lclStartNumber = toInt(lclStart);
			int lclEndNumber = toInt(lclEnd);
			if (lclStartNumber == Integer.MIN_VALUE) {
				ourLogger.warn("LocalDateCache start date is out of range; cache disabled.");
			} else if (lclEndNumber == Integer.MIN_VALUE) {
				ourLogger.warn("LocalDateCache end date is out of range; cache disabled.");
			} else if (lclStartNumber >= lclEndNumber) {
				ourLogger.warn("LocalDateCache start date must be strictly before end date); cache disabled.");
			}
			if (lclStartNumber == Integer.MIN_VALUE || lclEndNumber == Integer.MIN_VALUE || lclStartNumber >= lclEndNumber) {
				return;
			}
			ourLogger.info("Caching from " + lclStartNumber + " through " + lclEndNumber);
			ourStartNumber = lclStartNumber;
			ourEndNumber = lclEndNumber;
			ourCache = new LocalDate[lclEndNumber-lclStartNumber];
		} catch (Exception lclE) {
			ourLogger.error("Exception while constructing LocalDateCache; cache disabled.", lclE);
		}
	}
	
	private static int toInt(LocalDate argLD) {
		if (argLD == null) {
			return Integer.MIN_VALUE;
		}
		if (argLD.getYear() < 1900) {
			return Integer.MIN_VALUE;
		}
		if (argLD.getYear() > 2100) {
			return Integer.MIN_VALUE;
		}
		return (argLD.getYear() * 12 + argLD.getMonthValue() - 1) * 31 + argLD.getDayOfMonth();
	}
	
	private LocalDateCache() {
		super();
	}
	
	private static final long SECONDS_PER_DAY = 60L * 60L * 24L;
	private static final long NANOS_PER_SECOND = 1000L * 1000L * 1000L;
	
	public static LocalDate today() {
		final Clock lclClock = Clock.systemDefaultZone();
		final Instant lclNow = lclClock.instant();
		final ZoneOffset lclOffset = lclClock.getZone().getRules().getOffset(lclNow);
		return getDate(lclNow, lclOffset);
	}

	private static LocalDate getDate(Instant argInstant, ZoneOffset argOffset) {
		long lclEpochSec = argInstant.getEpochSecond() + argOffset.getTotalSeconds();  // overflow caught later
		long lclEpochDay = Math.floorDiv(lclEpochSec, SECONDS_PER_DAY);
		LocalDate lclLD = LocalDate.ofEpochDay(lclEpochDay);
		return cache(lclLD);
	}
	
	public static LocalDate cache(java.util.Date argDate) {
		if (argDate == null) {
			return null;
		}
		Instant lclInstant = argDate.toInstant();
		ZoneOffset lclOffset = ZoneId.systemDefault().getRules().getOffset(lclInstant);
		return getDate(lclInstant, lclOffset);
	}
	
	public static LocalDate cache(java.sql.Date argDate) {
		if (argDate == null) {
			return null;
		}
		Instant lclInstant = Instant.ofEpochMilli(argDate.getTime());
		ZoneOffset lclOffset = ZoneId.systemDefault().getRules().getOffset(lclInstant);
		return getDate(lclInstant, lclOffset);
	}
	
	public static LocalDate cache(java.sql.Timestamp argTS) {
		if (argTS == null) {
			return null;
		}
		Instant lclInstant = argTS.toInstant();
		ZoneOffset lclOffset = ZoneId.systemDefault().getRules().getOffset(lclInstant);
		return getDate(lclInstant, lclOffset);
	}
	
	public static LocalDate cache(LocalDate argLD) {
//		System.out.println("Checking the cache for " + argLD);
		if (argLD == null) {
			return null;
		}
		if (ourCache == null) {
//			System.out.println("No cache");
			return argLD;
		}
		int lclNumber = toInt(argLD);
		if (lclNumber == Integer.MIN_VALUE) {
//			System.out.println("Out of absolute range");
			return argLD;
		}
		if (lclNumber < ourStartNumber || lclNumber >= ourEndNumber) {
//			System.out.println("Out of specified range");
			return argLD;
		}
		LocalDate lclLD = ourCache[lclNumber - ourStartNumber];
//		System.out.println("Cache for " + lclNumber + " is " + lclLD);
		if (lclLD == null) {
			return ourCache[lclNumber - ourStartNumber] = argLD;
		} else {
			return lclLD;
		}
	}
	
	public static LocalDateTime cacheDateTime(LocalDateTime argLDT) {
		if (argLDT == null) {
			return null;
		}
		LocalDate lclLD = argLDT.toLocalDate();
		LocalDate lclCLD = cache(lclLD);
		if (lclLD != lclCLD) {
			return LocalDateTime.of(lclCLD, argLDT.toLocalTime());
		} else {
			return argLDT;
		}
	}
	
	public static LocalDateTime cacheDateTime(java.sql.Timestamp argTS) {
		if (argTS == null) {
			return null;
		}
		Instant lclInstant = argTS.toInstant();
		ZoneOffset lclOffset = ZoneId.systemDefault().getRules().getOffset(lclInstant);
		return LocalDateTime.of(
			getDate(lclInstant, lclOffset),
			getTime(lclInstant, lclOffset)
		);
	}
	
	public static LocalDateTime now() {
		Clock lclClock = Clock.systemDefaultZone();
		final Instant lclNow = lclClock.instant();
		ZoneOffset lclOffset = lclClock.getZone().getRules().getOffset(lclNow);
		return LocalDateTime.of(
			getDate(lclNow, lclOffset),
			getTime(lclNow, lclOffset)
		);
	}
	
	public static LocalTime nowTime() {
		Clock lclClock = Clock.systemDefaultZone();
		final Instant lclNow = lclClock.instant();
		ZoneOffset lclOffset = lclClock.getZone().getRules().getOffset(lclNow);
		return getTime(lclNow, lclOffset);
	}
	
	private static LocalTime getTime(Instant argInstant, ZoneOffset argOffset) {
		long localSecond = argInstant.getEpochSecond() + argOffset.getTotalSeconds();  // overflow caught later
		int secsOfDay = (int) Math.floorMod(localSecond, SECONDS_PER_DAY);
		return LocalTime.ofNanoOfDay(secsOfDay * NANOS_PER_SECOND + argInstant.getNano());
	}
}
