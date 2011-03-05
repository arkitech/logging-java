
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
		this (new LuceneDatastoreConfiguration ());
	}
	
	public LuceneDatastore (final LuceneDatastoreConfiguration configuration_)
	{
		super ();
		final LuceneDatastoreConfiguration configuration =
				(configuration_ != null) ? configuration_ : new LuceneDatastoreConfiguration ();
		final Object monitor = (configuration.monitor != null) ? configuration.monitor : new Object ();
		synchronized (monitor) {
			this.monitor = monitor;
			this.callbacks = (configuration.callbacks != null) ? configuration.callbacks : new DefaultLoggerCallbacks (this);
			this.readOnly = (configuration.readOnly != null) ? configuration.readOnly.booleanValue () : true;
			this.bdb =
					new BdbDatastore (new BdbDatastoreConfiguration (
							configuration.environmentPath, this.readOnly, configuration.serializer,
							configuration.loadMutator, configuration.storeMutator, this.callbacks, this.monitor));
			this.index = new LuceneIndex (this.bdb, this.readOnly, this.callbacks, this.monitor);
			this.state = State.Closed;
		}
	}
	
	public final boolean close ()
	{
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			Preconditions.checkState (this.state == State.Opened, "lucene datastore is not opened");
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
			Preconditions.checkState (this.state == State.Closed, "lucene datastore is already opened");
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
	
	public final Iterable<LuceneQueryResult> query (final Query query, final int maxCount)
	{
		return (this.index.query (query, maxCount));
	}
	
	public final Iterable<ILoggingEvent> select (
			final ILoggingEvent referenceEvent, final int beforeCount, final int afterCount, final LoggingEventFilter filter)
	{
		return (this.bdb.select (referenceEvent, beforeCount, afterCount, filter));
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
	
	public final boolean syncRead ()
	{
		synchronized (this.monitor) {
			return (this.bdb.syncRead () && this.index.syncRead ());
		}
	}
	
	public final boolean syncWrite ()
	{
		synchronized (this.monitor) {
			return (this.bdb.syncWrite () && this.index.syncWrite ());
		}
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
