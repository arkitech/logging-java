
package eu.arkitech.logback.common;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;


public interface LoggingEventFilter
{
	public abstract FilterReply filter (final ILoggingEvent event)
			throws Throwable;
}
