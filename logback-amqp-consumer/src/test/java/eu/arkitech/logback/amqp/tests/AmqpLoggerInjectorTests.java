
package eu.arkitech.logback.amqp.tests;


import ch.qos.logback.classic.Logger;
import eu.arkitech.logback.amqp.consumer.AmqpLoggingInjector;
import eu.arkitech.logback.common.BlockingQueueAppender;
import org.slf4j.LoggerFactory;

import org.junit.Assert;
import org.junit.Test;


public final class AmqpLoggerInjectorTests
{
	@Test
	public final void testAgent ()
			throws Throwable
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpLoggerInjectorTests.class.getName ());
		
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpLoggerInjectorTests.testLoggerName);
		
		realLogger.debug ("initializing collector appender");
		final BlockingQueueAppender collector = new BlockingQueueAppender ();
		collector.setName (String.format ("%s@%x", collector.getClass ().getName (), System.identityHashCode (collector)));
		collector.setContext (testLogger.getLoggerContext ());
		collector.start ();
		
		testLogger.addAppender (collector);
		testLogger.setAdditive (false);
		
		realLogger.debug ("initializing amqp consumer agent");
		final AmqpLoggingInjector agent = new AmqpLoggingInjector ();
		agent.setQueue ("logging.events.tests");
		agent.setContext (testLogger.getLoggerContext ());
		agent.start ();
		
		realLogger.debug ("waiting for message draining (i.e. until we receive enough)");
		for (int tries = 0; tries < AmqpLoggerInjectorTests.timeoutTries; tries++) {
			if (collector.size () >= AmqpLoggerInjectorTests.messageCount)
				break;
			Thread.sleep (AmqpLoggerInjectorTests.timeout);
		}
		
		realLogger.debug ("stopping and joining amqp consumer agent");
		agent.stop ();
		
		Assert.assertTrue (agent.isDrained ());
		Assert.assertFalse (agent.isStarted ());
		Assert.assertFalse (agent.isRunning ());
		
		testLogger.detachAppender (collector);
	}
	
	private static final int messageCount = 20;
	private static final String testLoggerName = "__testing__.eu.ackitech.logback.amqp.logger";
	private static final int timeout = 100;
	private static final int timeoutTries = 100 * 1000;
}
