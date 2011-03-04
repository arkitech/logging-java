
package eu.arkitech.logback.common;


import ch.qos.logback.classic.spi.ILoggingEvent;


public interface LoggingEventMutator
{
	public abstract void mutate (final ILoggingEvent event)
			throws Throwable;
}
