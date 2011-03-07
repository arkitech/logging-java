
package eu.arkitech.logging.conapp;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.arkitech.logback.amqp.consumer.AmqpConsumerAppender;
import eu.arkitech.logback.amqp.publisher.AmqpPublisherAppender;
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
		
		final List<AmqpConsumerAppender> consumers = Collections.synchronizedList (new LinkedList<AmqpConsumerAppender> ());
		AmqpConsumerAppender.CreateAction.defaultCollector = consumers;
		AmqpConsumerAppender.CreateAction.defaultAutoStart = false;
		
		final List<AmqpPublisherAppender> publishers = Collections.synchronizedList (new LinkedList<AmqpPublisherAppender> ());
		AmqpPublisherAppender.CreateAction.defaultCollector = publishers;
		AmqpPublisherAppender.CreateAction.defaultAutoStart = false;
		
		LoggerFactory.getILoggerFactory ();
		
		if (consumers.isEmpty () && publishers.isEmpty ())
			throw (new IllegalArgumentException ("no amqp accessors defined; aborting!"));
		
		for (final AmqpConsumerAppender accessor : consumers)
			accessor.start ();
		for (final AmqpPublisherAppender accessor : publishers)
			accessor.start ();
		
		while (true) {
			boolean stillRunning = false;
			for (final AmqpConsumerAppender accessor : consumers)
				stillRunning = accessor.isRunning ();
			for (final AmqpPublisherAppender accessor : publishers)
				stillRunning = accessor.isRunning ();
			if (!stillRunning)
				break;
			try {
				Thread.sleep (ConappMain.defaultWaitTimeout);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		
		System.exit (1);
	}
	
	public static final long defaultWaitTimeout = 1000;
}
