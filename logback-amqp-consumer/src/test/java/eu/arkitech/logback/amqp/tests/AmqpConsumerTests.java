
package eu.arkitech.logback.amqp.tests;


import ch.qos.logback.classic.Logger;
import eu.arkitech.logback.amqp.consumer.AmqpConsumerAppender;
import eu.arkitech.logback.common.BlockingQueueAppender;
import org.slf4j.LoggerFactory;

import org.junit.Assert;
import org.junit.Test;


public final class AmqpConsumerTests
{
	@Test
	public final void testConsumerAppender ()
			throws Exception
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpConsumerTests.class.getName ());
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpConsumerTests.testLoggerName);
		realLogger.debug ("initializing collector appender");
		final BlockingQueueAppender collector = new BlockingQueueAppender ();
		collector.setName (String.format ("%s@%x", collector.getClass ().getName (), System.identityHashCode (collector)));
		collector.setContext (testLogger.getLoggerContext ());
		collector.start ();
		testLogger.addAppender (collector);
		testLogger.setAdditive (false);
		realLogger.debug ("initializing amqp consumer object");
		final AmqpConsumerAppender agent = new AmqpConsumerAppender ();
		agent.setQueue ("logging.events.tests");
		agent.setContext (testLogger.getLoggerContext ());
		agent.start ();
		realLogger.debug ("waiting for message draining (i.e. until we receive enough)");
		for (int tries = 0; tries < AmqpConsumerTests.timeoutTries; tries++) {
			if (collector.size () >= AmqpConsumerTests.messageCount)
				break;
			Thread.sleep (AmqpConsumerTests.timeout);
		}
		realLogger.debug ("stopping and joining amqp consumer object");
		agent.stop ();
		Assert.assertTrue (agent.isDrained ());
		Assert.assertFalse (agent.isStarted ());
		Assert.assertFalse (agent.isRunning ());
		testLogger.detachAppender (collector);
	}
	
	private static final int messageCount = 20;
	private static final String testLoggerName = "__testing__.eu.ackitech.logback.amqp.logger";
	private static final int timeout = 100;
	private static final int timeoutTries = 100;
}
