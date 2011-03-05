
package eu.arkitech.logging.datastore.lucene;


import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.LoggingEventFilter;
import eu.arkitech.logging.datastore.bdb.BdbDatastore;
import eu.arkitech.logging.datastore.bdb.BdbDatastoreConfiguration;
import eu.arkitech.logging.datastore.common.Datastore;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;


public final class LuceneDatastore
		implements
			Datastore
{
	public LuceneDatastore ()
	{
		this (new BdbDatastoreConfiguration ());
	}
	
	public LuceneDatastore (final BdbDatastoreConfiguration configuration)
	{
		this (configuration, null);
	}
	
	public LuceneDatastore (final BdbDatastoreConfiguration configuration_, final Callbacks callbacks)
	{
		super ();
		final BdbDatastoreConfiguration configuration =
				(configuration_ != null) ? configuration_ : new BdbDatastoreConfiguration ();
		synchronized (configuration.monitor) {
			this.monitor = Preconditions.checkNotNull (configuration.monitor);
			this.callbacks =
					((callbacks != null) ? callbacks : ((configuration.callbacks != null) ? configuration.callbacks
							: new DefaultLoggerCallbacks (this)));
			this.readOnly = configuration.readOnly;
			this.bdb = new BdbDatastore (configuration, this.callbacks);
			this.index = new LuceneIndex (this.bdb, this.readOnly, this.callbacks, this.monitor);
			this.state = State.Closed;
		}
	}
	
	public final boolean close ()
	{
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			if (this.state != State.Opened)
				throw (new IllegalStateException ("lucene datastore is not opened"));
			boolean succeeded = true;
			succeeded |= this.index.close ();
			succeeded |= this.bdb.close ();
			this.state = State.Closed;
			return (succeeded);
		}
	}
	
	public final boolean open ()
	{
		synchronized (this.monitor) {
			if (this.state != State.Closed)
				throw (new IllegalStateException ("lucene datastore is already opened"));
			Runtime.getRuntime ().addShutdownHook (new Thread () {
				public final void run ()
				{
					LuceneDatastore.this.close ();
				}
			});
			boolean succeeded = this.bdb.open ();
			if (succeeded)
				succeeded = this.index.open ();
			if (!succeeded) {
				this.close ();
				return (false);
			}
			this.state = State.Opened;
			return (succeeded);
		}
	}
	
	public final Query parseQuery (final String query)
			throws ParseException
	{
		return (this.index.parseQuery (query));
	}
	
	public final Iterable<LuceneQueryResult> query (final Query query, final int maxCount, final boolean flush)
	{
		return (this.index.query (query, maxCount, flush));
	}
	
	public final Iterable<ILoggingEvent> select (
			final ILoggingEvent reference, final int beforeCount, final int afterCount, final LoggingEventFilter filter)
	{
		return (this.bdb.select (reference, beforeCount, afterCount, filter));
	}
	
	public final Iterable<ILoggingEvent> select (
			final long afterTimestamp, final long maximumInterval, final int maximumCount, final LoggingEventFilter filter)
	{
		return (this.bdb.select (afterTimestamp, maximumInterval, maximumCount, filter));
	}
	
	public final ILoggingEvent select (final String key)
	{
		return (this.bdb.select (key));
	}
	
	public final String store (final ILoggingEvent event)
	{
		final String key = this.bdb.store (event);
		if (key != null)
			this.index.store (key, event);
		return (key);
	}
	
	private final BdbDatastore bdb;
	private final Callbacks callbacks;
	private final LuceneIndex index;
	private final Object monitor;
	private final boolean readOnly;
	private State state;
	
	public static enum State
	{
		Closed,
		Opened;
	}
}
