
package eu.arkitech.logging.datastore.bdb;


import java.io.File;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.DefaultSerializerAppenderSink;


public class BdbAppender
		extends DefaultSerializerAppenderSink
{
	public BdbAppender ()
	{
		super ();
	}
	
	public String getEnvironmentPath ()
	{
		return (this.environmentPath);
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (true);
	}
	
	public void setEnvironmentPath (final String environmentPath)
	{
		this.environmentPath = environmentPath;
	}
	
	protected BdbDatastoreConfiguration buildConfiguration ()
	{
		return (new BdbDatastoreConfiguration ((this.environmentPath != null) ? new File (this.environmentPath) : null, false, this.serializer, this.mutator, this.mutator, this.callbacks));
	}
	
	@Override
	protected final void reallyAppend (final ILoggingEvent event)
	{
		this.datastore.store (event);
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean datastoreOpenSucceeded;
			try {
				if (this.datastore != null)
					throw (new IllegalStateException ());
				this.datastore = new BdbDatastore (this.buildConfiguration ());
				datastoreOpenSucceeded = this.datastore.open ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb appender encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			return (datastoreOpenSucceeded);
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			final boolean datastoreCloseSucceeded = false;
			try {
				if (this.datastore != null)
					this.datastore.close ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb appender encountered an error while closing the datastore; ignoring");
				this.datastore = null;
			}
			return (datastoreCloseSucceeded);
		}
	}
	
	protected String environmentPath;
	private BdbDatastore datastore;
}
