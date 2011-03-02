
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.util.LinkedList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.RandomEventGenerator;
import eu.arkitech.logback.common.SLoggingEvent1;
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
		
		final Logger logger = LoggerFactory.getLogger (LuceneDatastoreMain.class);
		
		logger.info ("opening");
		final File path = new File ("/tmp/logging");
		final LuceneDatastore datastore = new LuceneDatastore (path);
		datastore.open ();
		
		logger.info ("storing");
		final LinkedList<String> keys = new LinkedList<String> ();
		final RandomEventGenerator generator = new RandomEventGenerator ();
		for (int i = 0; i < 100; i++) {
			final ILoggingEvent event = generator.generate ();
			final SLoggingEvent1 event1 = SLoggingEvent1.build (event);
			final String key = datastore.store (event1);
			if (key != null)
				keys.add (key);
			else
				logger.error ("store failed");
		}
		
		logger.info ("selecting");
		for (final String key : keys) {
			final ILoggingEvent event = datastore.select (key);
			if (event == null)
				logger.error ("select failed for `{}`", key);
		}
		final String queryString = "(level:INFO OR level:ERROR) AND message:a";
		logger.info ("querying `{}`", queryString);
		Query query = null;
		try {
			query = datastore.parseQuery (queryString);
		} catch (final ParseException exception) {
			logger.error (String.format ("query failed for `{}`", queryString), exception);
		}
		if (query != null) {
			final List<LuceneQueryResult> results = datastore.query (query, 100);
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
		
		logger.info ("cloning");
		datastore.close ();
	}
}
