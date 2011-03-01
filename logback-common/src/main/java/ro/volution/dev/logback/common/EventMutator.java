
package ro.volution.dev.logback.common;


import ch.qos.logback.classic.spi.ILoggingEvent;


public interface EventMutator
{
	public abstract void mutate (final ILoggingEvent event)
			throws Throwable;
}
