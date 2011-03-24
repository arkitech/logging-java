
package eu.arkitech.logging.conapp;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.arkitech.logback.amqp.accessors.AmqpAccessorAppender;
import eu.arkitech.logback.amqp.consumer.AmqpConsumerAppender;
import eu.arkitech.logback.amqp.publisher.AmqpPublisherAppender;
import eu.arkitech.logging.datastore.bdb.BdbDatastoreAppender;
import eu.arkitech.logging.datastore.common.DatastoreAppender;
import eu.arkitech.logging.datastore.lucene.LuceneDatastoreAppender;
import org.slf4j.LoggerFactory;


public final class ConappMain
{
	private ConappMain ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		if (arguments.length != 0)
			throw (new IllegalArgumentException ("amqp consumer console application takes no arguments (use the logback system property `logback.configurationFile`); aborting!"));
		
		final List<AmqpAccessorAppender> amqpAppenders = Collections.synchronizedList (new LinkedList<AmqpAccessorAppender> ());
		AmqpConsumerAppender.CreateAction.defaultCollector = amqpAppenders;
		AmqpPublisherAppender.CreateAction.defaultCollector = amqpAppenders;
		
		final List<DatastoreAppender> datastoreAppenders = Collections.synchronizedList (new LinkedList<DatastoreAppender> ());
		BdbDatastoreAppender.CreateAction.defaultCollector = datastoreAppenders;
		LuceneDatastoreAppender.CreateAction.defaultCollector = datastoreAppenders;
		
		LoggerFactory.getILoggerFactory ();
		
		if (amqpAppenders.isEmpty ())
			throw (new IllegalArgumentException ("no amqp accessors defined; aborting!"));
		
		while (true) {
			boolean stillRunning = false;
			for (final AmqpAccessorAppender accessor : amqpAppenders)
				stillRunning |= accessor.isRunning ();
			if (!stillRunning)
				break;
			try {
				Thread.sleep (ConappMain.defaultWaitTimeout);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		
		for (final DatastoreAppender appender : datastoreAppenders)
			appender.stop ();
		
		System.exit (1);
	}
	
	public static final long defaultWaitTimeout = 1000;
}
