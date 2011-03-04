
package eu.arkitech.logback.common;


import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;


public interface LoggingEventSink
{
	public abstract boolean isDrained ()
			throws Throwable;
	
	public abstract boolean push (final ILoggingEvent event)
			throws InterruptedException,
				Throwable;
	
	public abstract boolean push (final ILoggingEvent event, final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException,
				Throwable;
}
