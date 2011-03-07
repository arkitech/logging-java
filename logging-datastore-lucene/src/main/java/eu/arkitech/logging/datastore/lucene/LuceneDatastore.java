
package eu.arkitech.logging.datastore.lucene;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.LoggingEventFilter;
import eu.arkitech.logging.datastore.bdb.BdbDatastore;
import eu.arkitech.logging.datastore.bdb.BdbDatastoreConfiguration;
import eu.arkitech.logging.datastore.common.SyncableDatastoreBackgroundWorker;
import eu.arkitech.logging.datastore.common.SyncableDatastoreBackgroundWorkerConfiguration;
import eu.arkitech.logging.datastore.common.SyncableImmutableDatastore;
import eu.arkitech.logging.datastore.common.SyncableMutableDatastore;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;


public final class LuceneDatastore
		implements
			SyncableMutableDatastore,
			SyncableImmutableDatastore
{
	public LuceneDatastore ()
	{
		this (new LuceneDatastoreConfiguration ());
	}
	
	public LuceneDatastore (final LuceneDatastoreConfiguration configuration_)
	{
		super ();
		final LuceneDatastoreConfiguration configuration = (configuration_ != null) ? configuration_ : new LuceneDatastoreConfiguration ();
		this.monitor = (configuration.monitor != null) ? configuration.monitor : new Object ();
		this.callbacks = (configuration.callbacks != null) ? configuration.callbacks : new DefaultLoggerCallbacks (this);
		this.readOnly = (configuration.readOnly != null) ? configuration.readOnly.booleanValue () : true;
		this.syncWorkerEnabled = Preconditions.checkNotNull (Objects.firstNonNull (configuration.syncEnabled, true));
		this.syncTimeout = Preconditions.checkNotNull (Objects.firstNonNull (configuration.syncTimeout, SyncableDatastoreBackgroundWorkerConfiguration.defaultSyncWriteTimeout));
		Preconditions.checkArgument ((this.syncTimeout == -1) || (this.syncWorkerEnabled && (this.syncTimeout > 0)));
		this.bdb = new BdbDatastore (new BdbDatastoreConfiguration (configuration.environmentPath, this.readOnly, false, -1L, configuration.serializer, configuration.loadMutator, configuration.storeMutator, this.callbacks, this.monitor));
		this.index = new LuceneIndex (this.bdb, this.readOnly, this.callbacks, this.monitor);
		this.state = State.Closed;
	}
	
	@Override
	public final boolean close ()
	{
		boolean succeeded = false;
		final SyncableDatastoreBackgroundWorker syncWorker;
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			Preconditions.checkState (this.state == State.Opened, "lucene datastore is not opened");
			syncWorker = this.syncWorker;
			this.syncWorker = null;
			this.callbacks.handleLogEvent (Level.DEBUG, null, "lucene datastore closing");
			succeeded = true;
			succeeded |= this.index.close ();
			succeeded |= this.bdb.close ();
			this.callbacks.handleLogEvent (Level.INFO, null, "lucene datastore closed");
			this.state = State.Closed;
		}
		if (syncWorker != null)
			syncWorker.cancel ();
		return (succeeded);
	}
	
	@Override
	public final boolean open ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "lucene datastore opening");
			Preconditions.checkState (this.state == State.Closed, "lucene datastore is already opened");
			if (this.syncWorkerEnabled) {
				this.syncWorker = new SyncableDatastoreBackgroundWorker (new SyncableDatastoreBackgroundWorkerConfiguration (this, this.syncTimeout, !this.readOnly ? this.syncTimeout : -1L, this.callbacks, this.monitor));
				if (!this.syncWorker.start ()) {
					this.callbacks.handleLogEvent (Level.ERROR, null, "bdb datastore failed to start sync thread; aborting!");
					this.close ();
					return (false);
				}
			}
			boolean succeeded = this.bdb.open ();
			if (succeeded)
				succeeded = this.index.open ();
			if (!succeeded) {
				this.close ();
				return (false);
			}
			this.state = State.Opened;
			this.callbacks.handleLogEvent (Level.INFO, null, "lucene datastore opened");
			return (true);
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
	
	@Override
	public final Iterable<ILoggingEvent> select (final ILoggingEvent referenceEvent, final int beforeCount, final int afterCount, final LoggingEventFilter filter)
	{
		return (this.bdb.select (referenceEvent, beforeCount, afterCount, filter));
	}
	
	@Override
	public final Iterable<ILoggingEvent> select (final long afterTimestamp, final long maximumInterval, final int maximumCount, final LoggingEventFilter filter)
	{
		return (this.bdb.select (afterTimestamp, maximumInterval, maximumCount, filter));
	}
	
	@Override
	public final ILoggingEvent select (final String key)
	{
		return (this.bdb.select (key));
	}
	
	@Override
	public final String store (final ILoggingEvent event)
	{
		final String key = this.bdb.store (event);
		if (key != null)
			this.index.store (key, event);
		return (key);
	}
	
	@Override
	public final boolean syncRead ()
	{
		synchronized (this.monitor) {
			if (this.readOnly)
				return (this.index.close (true) && this.bdb.close (true) && this.bdb.open (true) && this.index.open (true));
			else
				return (this.bdb.syncRead () && this.index.syncRead ());
		}
	}
	
	@Override
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
	private final long syncTimeout;
	private SyncableDatastoreBackgroundWorker syncWorker;
	private final boolean syncWorkerEnabled;
	
	public static enum State
	{
		Closed,
		Opened;
	}
}
