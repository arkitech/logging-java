
package eu.arkitech.logging.datastore.lucene;


import java.io.File;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.LoggingEventFilter;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.bdb.BdbDatastore;
import eu.arkitech.logging.datastore.common.Datastore;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;


public final class LuceneDatastore
		implements
			Datastore
{
	public LuceneDatastore (final File environmentPath, final boolean readOnly)
	{
		this (environmentPath, readOnly, -1);
	}
	
	public LuceneDatastore (final File environmentPath, final boolean readOnly, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, -1, callbacks);
	}
	
	public LuceneDatastore (final File environmentPath, final boolean readOnly, final int compressed)
	{
		this (environmentPath, readOnly, compressed, null);
	}
	
	public LuceneDatastore (
			final File environmentPath, final boolean readOnly, final int compressed, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, compressed == -1 ? new DefaultBinarySerializer () : new CompressedBinarySerializer (
				compressed), null, callbacks);
	}
	
	public LuceneDatastore (
			final File environmentPath, final boolean readOnly, final Serializer serializer,
			final LoggingEventMutator mutator, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, serializer, mutator, callbacks, new Object ());
	}
	
	public LuceneDatastore (
			final File environmentPath, final boolean readOnly, final Serializer serializer,
			final LoggingEventMutator mutator, final Callbacks callbacks, final Object monitor)
	{
		super ();
		synchronized (monitor) {
			this.monitor = monitor;
			this.readOnly = readOnly;
			this.state = State.Closed;
			this.callbacks = (callbacks != null) ? callbacks : new DefaultLoggerCallbacks (this);
			this.bdb = new BdbDatastore (environmentPath, this.readOnly, serializer, mutator, this.callbacks, this.monitor);
			this.index = new LuceneIndex (this.bdb, this.readOnly, this.callbacks, this.monitor);
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
			final long afterTimestamp, final long intervalMs, final LoggingEventFilter filter)
	{
		return (this.bdb.select (afterTimestamp, intervalMs, filter));
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
