
package eu.arkitech.logging.datastore.lucene;


import ch.qos.logback.classic.spi.ILoggingEvent;


public final class LuceneQueryResult
{
	public LuceneQueryResult (final String key, final ILoggingEvent event, final float score)
	{
		super ();
		this.key = key;
		this.event = event;
		this.score = score;
	}
	
	public final ILoggingEvent event;
	public final String key;
	public final float score;
}
