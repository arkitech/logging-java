
package eu.arkitech.logging.datastore.lucene;


import java.io.File;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.DefaultSerializerAppender;


public class LuceneAppender
		extends DefaultSerializerAppender
{
	public LuceneAppender ()
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
	
	protected LuceneDatastoreConfiguration buildConfiguration ()
	{
		return (new LuceneDatastoreConfiguration ((this.environmentPath != null) ? new File (this.environmentPath) : null, false, this.serializer, this.mutator, this.mutator, this.callbacks, null));
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
			try {
				if (this.datastore != null)
					throw (new IllegalStateException ());
				this.datastore = new LuceneDatastore (this.buildConfiguration ());
				final boolean succeeded = this.datastore.open ();
				if (!succeeded) {
					this.reallyStop ();
					return (false);
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore appender encountered an unknown error while starting; aborting!");
				this.reallyStop ();
				return (false);
			}
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			try {
				if (this.datastore != null) {
					this.datastore.close ();
					this.datastore = null;
				}
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore appender encountered an unknown error while stopping; ignoring");
				return (false);
			} finally {
				this.datastore = null;
			}
		}
	}
	
	protected String environmentPath;
	private LuceneDatastore datastore;
}
