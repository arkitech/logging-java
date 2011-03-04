
package eu.arkitech.logging.datastore.common;


import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.LoggingEventFilter;


public interface Datastore
{
	public abstract boolean close ();
	
	public abstract boolean open ();
	
	public abstract Iterable<ILoggingEvent> select (
			final ILoggingEvent reference, final int beforeCount, final int afterCount, final LoggingEventFilter filter);
	
	public abstract Iterable<ILoggingEvent> select (
			final long afterTimestamp, final long intervalMs, final LoggingEventFilter filter);
	
	public abstract ILoggingEvent select (final String key);
	
	public abstract String store (final ILoggingEvent event);
}
