
package eu.arkitech.logging.datastore.common;


import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.LoggingEventFilter;


public interface ImmutableDatastore
		extends
			Datastore
{
	public abstract Iterable<ILoggingEvent> select (final ILoggingEvent referenceEvent, final int beforeCount, final int afterCount, final LoggingEventFilter filter);
	
	public abstract Iterable<ILoggingEvent> select (final long afterTimestamp, final long maximumInterval, final int maximumCount, final LoggingEventFilter filter);
	
	public abstract ILoggingEvent select (final String key);
}
