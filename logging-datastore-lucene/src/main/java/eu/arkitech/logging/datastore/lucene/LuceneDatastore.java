
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.bdb.BdbDatastore;
import eu.arkitech.logging.datastore.common.Datastore;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;


public final class LuceneDatastore
		implements
			Datastore
{
	public LuceneDatastore (final File environmentPath)
	{
		this (environmentPath, -1);
	}
	
	public LuceneDatastore (final File environmentPath, final Callbacks callbacks)
	{
		this (environmentPath, -1, callbacks);
	}
	
	public LuceneDatastore (final File environmentPath, final int compressed)
	{
		this (environmentPath, compressed, null);
	}
	
	public LuceneDatastore (final File environmentPath, final int compressed, final Callbacks callbacks)
	{
		this (environmentPath, compressed == -1 ? new DefaultBinarySerializer ()
				: new CompressedBinarySerializer (compressed), callbacks);
	}
	
	public LuceneDatastore (final File environmentPath, final Serializer serializer, final Callbacks callbacks)
	{
		this (environmentPath, serializer, callbacks, new Object ());
	}
	
	public LuceneDatastore (
			final File environmentPath, final Serializer serializer, final Callbacks callbacks, final Object monitor)
	{
		super ();
		synchronized (monitor) {
			this.monitor = monitor;
			this.state = State.Closed;
			this.callbacks = (callbacks != null) ? callbacks : new DefaultLoggerCallbacks (this);
			this.bdb = new BdbDatastore (environmentPath, serializer, this.callbacks, this.monitor);
			this.index = new LuceneIndex (this.bdb, this.callbacks, this.monitor);
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
	
	public final List<LuceneQueryResult> query (final Query query, final int maxCount)
	{
		return (this.index.query (query, maxCount));
	}
	
	public final List<ILoggingEvent> select (
			final ILoggingEvent reference, final int beforeCount, final int afterCount, final Filter<ILoggingEvent> filter)
	{
		return (this.bdb.select (reference, beforeCount, afterCount, filter));
	}
	
	public final List<ILoggingEvent> select (
			final long afterTimestamp, final long intervalMs, final Filter<ILoggingEvent> filter)
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
	private State state;
	
	public static enum State
	{
		Closed,
		Opened;
	}
}
