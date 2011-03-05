
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.util.LinkedList;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.RandomGenerator;
import eu.arkitech.logback.common.SLoggingEvent1;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Main
{
	private Main ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@SuppressWarnings ({"unused", "null"})
	public static final void main (final String[] arguments)
			throws Throwable
	{
		if (arguments.length != 0)
			throw (new IllegalArgumentException ());
		
		final boolean readOnly = false;
		final int compressed = -1;
		final int storeCount = 100;
		final int selectKeysCount = 5;
		final int selectReferenceBeforeCount = 2;
		final int selectReferenceAfterCount = 2;
		final long selectAfterTimestamp = System.currentTimeMillis () - 10 * 1000;
		final long selectAfterInterval = Long.MAX_VALUE;
		final int selectAfterCount = 10;
		final int queryCount = 10;
		final String queryString = "level:INFO OR level:ERROR";
		
		final Logger logger = LoggerFactory.getLogger (Main.class);
		
		logger.info ("opening");
		final File path = new File ("/tmp/arkitech-logging-datastore");
		final LuceneDatastore datastore = new LuceneDatastore (new LuceneDatastoreConfiguration (path, readOnly, compressed));
		if (!datastore.open ()) {
			logger.error ("open failed");
			return;
		}
		
		final LinkedList<String> keys;
		if ((storeCount > 0) && !readOnly) {
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
			if (!datastore.syncWrite ())
				logger.error ("store sync write failed");
			if (!datastore.syncRead ())
				logger.error ("store sync read failed");
		} else
			keys = null;
		
		if ((keys != null) && (selectKeysCount > 0)) {
			logger.info ("selecting keys");
			int i = 0;
			for (final String key : keys) {
				final ILoggingEvent event = datastore.select (key);
				if (event == null)
					logger.error ("select failed for `{}`", key);
				i++;
				if (i >= selectKeysCount)
					break;
			}
		}
		
		if ((keys != null) && ((selectReferenceAfterCount > 0) || (selectReferenceBeforeCount > 0))) {
			logger.info ("selecting reference event");
			final ILoggingEvent referenceEvent = datastore.select (keys.get (keys.size () / 2));
			if (referenceEvent == null)
				logger.error ("select failed for reference event");
			else {
				logger.info ("selecting around timestamp `{}`", referenceEvent.getTimeStamp ());
				final Iterable<ILoggingEvent> events = datastore.select (referenceEvent, 10, 10, null);
				if (events != null)
					for (final ILoggingEvent event : events) {
						System.out.format ("[%s] [%s] [%s] %s | %s\n", event.getTimeStamp (), event.getLevel (), event.getLoggerName (), event.getFormattedMessage (), ((SLoggingEvent1) event).key);
					}
				else
					logger.error ("select around timestamp failed");
			}
		}
		
		if (selectAfterCount > 0) {
			logger.info ("selecting after timestamp `{}`", selectAfterTimestamp);
			final Iterable<ILoggingEvent> events = datastore.select (selectAfterTimestamp, selectAfterInterval, selectAfterCount, null);
			if (events != null)
				for (final ILoggingEvent event : events) {
					System.out.format ("[%s] [%s] [%s] %s | %s\n", event.getTimeStamp (), event.getLevel (), event.getLoggerName (), event.getFormattedMessage (), ((SLoggingEvent1) event).key);
				}
			else
				logger.error ("select after timestamp failed");
		}
		
		if (queryCount > 0) {
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
						System.out.format ("%s :: [%s] [%s] [%s] %s | %s\n", result.score, event.getTimeStamp (), event.getLevel (), event.getLoggerName (), event.getFormattedMessage (), result.key);
					}
				else
					logger.error ("query failed for `{}`", queryString);
			}
		}
		
		logger.info ("closing");
		datastore.close ();
	}
}
