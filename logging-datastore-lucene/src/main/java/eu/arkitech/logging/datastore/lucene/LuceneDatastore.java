
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.util.List;

import eu.arkitech.logback.common.DefaultBinarySerializer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.common.Datastore;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;


public final class LuceneDatastore
		implements
			Datastore
{
	public LuceneDatastore (final File path, final int compressed, final boolean indexed)
	{
		super ();
		this.path = path;
		this.compressed = compressed;
		this.indexed = indexed;
		if (this.compressed != -1)
			this.serializer = new CompressedBinarySerializer (this.compressed);
		else
			this.serializer = new DefaultBinarySerializer ();
		this.bdb = new BdbDatastore (this.path, true, this.serializer);
		if (this.indexed)
			this.index = new LuceneIndex (this.bdb);
		else
			this.index = null;
	}
	
	public final boolean close ()
	{
		boolean succeeded = true;
		if (this.index != null)
			succeeded |= this.index.close ();
		succeeded |= this.bdb.close ();
		return (succeeded);
	}
	
	public final boolean open ()
	{
		boolean succeeded = this.bdb.open ();
		if (succeeded) {
			if (this.index != null) {
				succeeded = this.index.open ();
				if (!succeeded) {
					this.index.close ();
					this.bdb.close ();
				}
			}
		}
		return (succeeded);
	}
	
	public final Query parseQuery (final String query)
			throws ParseException
	{
		if (this.index == null)
			throw (new IllegalStateException ());
		return (this.index.parseQuery (query));
	}
	
	public final List<LuceneQueryResult> query (final Query query, final int maxCount)
	{
		if (this.index == null)
			throw (new IllegalStateException ());
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
		if (key != null && this.index != null)
			this.index.store (key, event);
		return (key);
	}
	
	private final boolean indexed;
	private final int compressed;
	private final BdbDatastore bdb;
	private final LuceneIndex index;
	private final File path;
	private final Serializer serializer;
}
