
package eu.arkitech.logging.conapp;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.arkitech.logback.amqp.consumer.AmqpConsumerAppender;
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
		
		final List<AmqpConsumerAppender> collector = Collections.synchronizedList (new LinkedList<AmqpConsumerAppender> ());
		
		AmqpConsumerAppender.CreateAction.defaultCollector = collector;
		AmqpConsumerAppender.CreateAction.defaultAutoStart = false;
		
		LoggerFactory.getILoggerFactory ();
		
		if (collector.isEmpty ())
			throw (new IllegalArgumentException ("no amqp logging injector defined; aborting!"));
		
		for (final AmqpConsumerAppender agent : collector)
			agent.start ();
		
		while (true) {
			boolean stillRunning = false;
			for (final AmqpConsumerAppender agent : collector)
				stillRunning |= agent.isRunning ();
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
