package com.opal.types;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
// import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

/* THINK: Can this be done with an internal LocalDateTime? */

public class UTCDateTime implements Serializable, Comparable<UTCDateTime>, Temporal, /* TemporalAccessor, */ TemporalAdjuster {
	private static final long serialVersionUID = 1L;
	
	// THINK: Can this be done with a LocalDateTime to save some memory and method invocations?
	private final OffsetDateTime myODT;
	
	public final static UTCDateTime MIN = UTCDateTime.of(LocalDateTime.MIN);
	public final static UTCDateTime MAX = UTCDateTime.of(LocalDateTime.MAX);
	
	private UTCDateTime(OffsetDateTime argODT) {
		super();
		if (argODT == null) {
			throw new IllegalArgumentException();
		}
		if (argODT.getOffset() != ZoneOffset.UTC) {
			throw new IllegalArgumentException("OffsetDateTime is not in UTC.");
		}
		myODT = argODT;
	}

	private OffsetDateTime getInternal() {
		return myODT;
	}

	public static UTCDateTime of(LocalDateTime argLDT) {
		if (argLDT == null) {
			return null;
		}
		return new UTCDateTime(OffsetDateTime.of(argLDT, ZoneOffset.UTC));
	}
	
	public static UTCDateTime of(OffsetDateTime argODT) {
		if (argODT == null) {
			return null;
		}
		if (argODT.getOffset() != ZoneOffset.UTC) {
			throw new IllegalArgumentException("OffsetDateTime is not in UTC."); // FIXME: We should probably be willing to adjust
		}
		return new UTCDateTime(argODT);
	}

	public static UTCDateTime ofInstant(Instant argI) {
		if (argI == null) {
			return null;
		}
		return new UTCDateTime(OffsetDateTime.ofInstant(argI, ZoneOffset.UTC));
	}
	
	public OffsetDateTime asOffsetDateTime() {
		return getInternal();
	}
	
	/* Should this be toLocalDateTime?  asLocalDateTime? extractLocalDateTime?  They key idea is tha it gives the zoneless
	 * LocalDateTime in UTC (as opposed to correcting it for whatever the default timezone of the server is).
	 */
	public LocalDateTime toLocalDateTime() {
		return getInternal().toLocalDateTime();
	}
	
	@Override
	public boolean equals(Object argO) {
		if (argO == null) {
			return false;
		}
		if (argO instanceof UTCDateTime) {
			return this.getInternal().equals(((UTCDateTime) argO).getInternal());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getInternal().hashCode();
	}
	
	@Override
	public Temporal adjustInto(Temporal argTemporal) {
		return getInternal().adjustInto(argTemporal);
	}

	@Override
	public boolean isSupported(TemporalField argField) {
		return getInternal().isSupported(argField);
	}

	@Override
	public long getLong(TemporalField argField) {
		return getInternal().getLong(argField);
	}

	@Override
	public boolean isSupported(TemporalUnit argUnit) {
		return getInternal().isSupported(argUnit);
	}

	@Override
	public Temporal with(TemporalField argField, long argNewValue) {
		return getInternal().with(argField, argNewValue);
	}

	@Override
	public UTCDateTime plus(long argAmountToAdd, TemporalUnit argUnit) {
		return of(getInternal().plus(argAmountToAdd, argUnit));
	}

	@Override
	public long until(Temporal argEndExclusive, TemporalUnit argUnit) {
		return getInternal().until(argEndExclusive, argUnit);
	}

	@Override
	public int compareTo(UTCDateTime that) {
		return getInternal().compareTo(that.getInternal());
	}
	
	// THINK: What do we do for a parse(...) method with no Formatter?
	public static UTCDateTime parse(String argS, DateTimeFormatter argDTF) {
		return of(OffsetDateTime.parse(argS, argDTF)); // THINK: What if the String doesn't represent UTC?
	}
	
	public boolean isBefore(UTCDateTime that) {
		if (that == null) {
			throw new IllegalArgumentException("Comparison UTCDateTime is null");
		}
		return getInternal().isBefore(that.getInternal());
	}

	public boolean isAfter(UTCDateTime that) {
		if (that == null) {
			throw new IllegalArgumentException("Comparison UTCDateTime is null");
		}
		return getInternal().isAfter(that.getInternal());
	}
	
	public static UTCDateTime now() {
		return of(OffsetDateTime.now(ZoneOffset.UTC));
	}

	/* I assumed that I would not have to write methods like the following; am I supposed to inherit them from somewhere? */
	public UTCDateTime plusDays(long argN) {
		return of(getInternal().plusDays(argN));
	}

	public UTCDateTime plusWeeks(long argN) {
		return of(getInternal().plusWeeks(argN));
	}

	public UTCDateTime plusMonths(long argN) {
		return of(getInternal().plusMonths(argN));
	}

	public UTCDateTime plusYears(long argN) {
		return of(getInternal().plusYears(argN));
	}

	public UTCDateTime minusDays(long argN) {
		return of(getInternal().minusDays(argN));
	}

	public UTCDateTime minusWeeks(long argN) {
		return of(getInternal().minusWeeks(argN));
	}

	public UTCDateTime minusMonths(long argN) {
		return of(getInternal().minusMonths(argN));
	}

	public UTCDateTime minusYears(long argN) {
		return of(getInternal().minusYears(argN));
	}
}
