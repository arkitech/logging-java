
package eu.arkitech.logging.datastore.common;


import ch.qos.logback.classic.spi.ILoggingEvent;


public interface MutableDatastore
		extends
			ImmutableDatastore
{
	public abstract String store (final ILoggingEvent event);
}
