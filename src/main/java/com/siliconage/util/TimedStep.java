package com.siliconage.util;

import java.io.IOException;
import java.util.OptionalLong;

import org.apache.commons.lang3.Validate;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public abstract class TimedStep implements AutoCloseable {
	private final String myEventName;
	private final long myStartTime;
	private OptionalLong myEndTime = OptionalLong.empty();
	
	protected TimedStep(String argEventName) {
		super();
		
		myEventName = Validate.notNull(argEventName);
		myStartTime = System.currentTimeMillis();
	}
	
	@Override
	public void close() {
		complete(System.currentTimeMillis());
		emit(generateMessage());
	}
	
	public long getTimeElapsed() {
		Validate.isTrue(hasCompleted(), getEventName() + " is not yet complete");
		return getOptionalEndTime().getAsLong() - getStartTime();
	}
	
	public long complete(long argEndTime) {
		myEndTime = OptionalLong.of(argEndTime);
		return getTimeElapsed();
	}
	
	public boolean hasCompleted() {
		return getOptionalEndTime().isPresent();
	}
	
	public String getEventName() {
		return myEventName;
	}
	
	public long getStartTime() {
		return myStartTime;
	}
	
	public OptionalLong getOptionalEndTime() {
		return myEndTime;
	}
	
	public String generateMessage() {
		Validate.isTrue(hasCompleted(), getEventName() + " is not yet complete");
		return getEventName() + " took " + getTimeElapsed() + " ms";
	}
	
	public abstract void emit(String argMessage);
	
	public static final TimedStep logging(String argEventName, Logger argLogger, Level argLevel) {
		return new LoggedTimedStep(argEventName, argLogger, argLevel);
	}
	
	public static final TimedStep logging(String argEventName, Logger argLogger) {
		return new LoggedTimedStep(argEventName, argLogger);
	}
	
	public static final TimedStep appending(String argEventName, Appendable argAppendable) {
		return new AppendableTimedStep(argEventName, argAppendable);
	}
	
	public static final TimedStep timingOnly(String argEventName) {
		return new NonWritingTimedStep(argEventName);
	}
	
	public static final TimedStep toStandardOut(String argEventName) {
		return appending(argEventName, System.out);
	}
	
	public static final TimedStep toStandardError(String argEventName) {
		return appending(argEventName, System.err);
	}
	
	private static class LoggedTimedStep extends TimedStep {
		private static final Level DEFAULT_LEVEL = Level.DEBUG;
		
		private final Logger myLogger;
		private final Level myLevel;
		
		protected LoggedTimedStep(String argEventName, Logger argLogger, Level argLevel) {
			super(argEventName);
			
			myLogger = Validate.notNull(argLogger);
			myLevel = Validate.notNull(argLevel);
		}
		
		protected LoggedTimedStep(String argEventName, Logger argLogger) {
			this(argEventName, argLogger, DEFAULT_LEVEL);
		}
		
		protected Logger getLogger() {
			return myLogger;
		}
		
		protected Level getLevel() {
			return myLevel;
		}
		
		@Override
		public void emit(String argMessage) {
			getLogger().log(getLevel(), argMessage);
		}
	}
	
	private static class AppendableTimedStep extends TimedStep {
		private final Appendable myAppendable;
		
		protected AppendableTimedStep(String argEventName, Appendable argAppendable) {
			super(argEventName);
			
			myAppendable = Validate.notNull(argAppendable);
		}
		
		protected Appendable getAppendable() {
			return myAppendable;
		}
		
		@Override
		public void emit(String argMessage) {
			try {
				getAppendable().append(argMessage + '\n');
			} catch (IOException lclE) {
				throw new IllegalStateException("Could not append timing result '" + argMessage + '"', lclE);
			}
		}
	}
	
	private static class NonWritingTimedStep extends TimedStep {
		protected NonWritingTimedStep(String argEventName) {
			super(argEventName);
		}
		
		@Override
		public void emit(String argMessage) {
			return;
		}
	}
}
