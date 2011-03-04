
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.util.LinkedList;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.RandomGenerator;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class LuceneDatastoreMain
{
	private LuceneDatastoreMain ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		if (arguments.length != 0)
			throw (new IllegalArgumentException ());
		
		final int compressed = 0;
		final boolean indexed = true;
		final int storeCount = 10 * 1000;
		final int selectCount = 10;
		final int queryCount = 10;
		final String queryString = "level:INFO OR level:ERROR";
		
		final Logger logger = LoggerFactory.getLogger (LuceneDatastoreMain.class);
		
		logger.info ("opening");
		final File path = new File ("/tmp/arkitech-logging-datastore");
		final LuceneDatastore datastore = new LuceneDatastore (path, compressed);
		datastore.open ();
		
		final LinkedList<String> keys;
		if (storeCount > 0) {
			logger.info ("storing");
			keys = new LinkedList<String> ();
			final RandomGenerator generator = new RandomGenerator ();
			for (int i = 0; i < storeCount; i++) {
				final ILoggingEvent event = generator.generate ();
				final String key = datastore.store (event);
				if (key != null)
					keys.add (key);
				else
					logger.error ("store failed");
			}
		} else
			keys = null;
		
		if ((keys != null) && (selectCount > 0)) {
			logger.info ("selecting");
			int i = 0;
			for (final String key : keys) {
				final ILoggingEvent event = datastore.select (key);
				if (event == null)
					logger.error ("select failed for `{}`", key);
				i++;
				if (i >= selectCount)
					break;
			}
		}
		
		if ((queryCount > 0) && indexed) {
			logger.info ("querying `{}`", queryString);
			Query query = null;
			try {
				query = datastore.parseQuery (queryString);
			} catch (final ParseException exception) {
				logger.error (String.format ("query failed for `{}`", queryString), exception);
			}
			if (query != null) {
				final Iterable<LuceneQueryResult> results = datastore.query (query, 100);
				if (results != null)
					for (final LuceneQueryResult result : results) {
						final ILoggingEvent event = result.event;
						System.out.format (
								"%s :: [%s] [%s] [%s] %s | %s\n", result.score, event.getTimeStamp (), event.getLevel (),
								event.getLoggerName (), event.getFormattedMessage (), result.key);
					}
				else
					logger.error ("query failed for `{}`", queryString);
			}
		}
		
		logger.info ("closing");
		datastore.close ();
	}
}
