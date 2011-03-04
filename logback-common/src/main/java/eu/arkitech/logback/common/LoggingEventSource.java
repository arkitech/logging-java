
package eu.arkitech.logback.common;


import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;


public interface LoggingEventSource
{
	public abstract boolean isDrained ()
			throws Throwable;
	
	public abstract ILoggingEvent pull ()
			throws InterruptedException,
				Throwable;
	
	public abstract ILoggingEvent pull (final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException,
				Throwable;
}
