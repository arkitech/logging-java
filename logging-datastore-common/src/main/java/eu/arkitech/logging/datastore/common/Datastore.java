
package eu.arkitech.logging.datastore.common;


import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;


public interface Datastore
{
	public abstract boolean close ();
	
	public abstract boolean open ();
	
	public abstract List<ILoggingEvent> select (
			final ILoggingEvent reference, final int beforeCount, final int afterCount, final Filter<ILoggingEvent> filter);
	
	public abstract List<ILoggingEvent> select (
			final long afterTimestamp, final long intervalMs, final Filter<ILoggingEvent> filter);
	
	public abstract ILoggingEvent select (final String key);
	
	public abstract String store (final ILoggingEvent event);
}
