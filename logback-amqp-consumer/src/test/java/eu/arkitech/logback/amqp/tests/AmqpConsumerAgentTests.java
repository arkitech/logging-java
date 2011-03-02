
package eu.arkitech.logback.amqp.tests;


import ch.qos.logback.classic.Logger;
import eu.arkitech.logback.amqp.consumer.AmqpConsumerAgent;
import eu.arkitech.logback.common.BufferedAppender;
import org.slf4j.LoggerFactory;

import org.junit.Assert;
import org.junit.Test;


public final class AmqpConsumerAgentTests
{
	@Test
	public final void testAgent ()
			throws Throwable
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpConsumerAgentTests.class.getName ());
		
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpConsumerAgentTests.testLoggerName);
		
		realLogger.debug ("initializing collector appender");
		final BufferedAppender collector = new BufferedAppender ();
		collector.setName (String.format ("%s@%x", collector.getClass ().getName (), System.identityHashCode (collector)));
		collector.setContext (testLogger.getLoggerContext ());
		collector.start ();
		
		testLogger.addAppender (collector);
		testLogger.setAdditive (false);
		
		realLogger.debug ("initializing amqp consumer agent");
		final AmqpConsumerAgent agent = new AmqpConsumerAgent ();
		agent.setQueue ("logback.tests");
		agent.setContext (testLogger.getLoggerContext ());
		agent.start ();
		
		realLogger.debug ("waiting for message draining (i.e. until we receive enough)");
		for (int tries = 0; tries < AmqpConsumerAgentTests.timeoutTries; tries++) {
			if (collector.size () >= AmqpConsumerAgentTests.messageCount)
				break;
			Thread.sleep (AmqpConsumerAgentTests.timeout);
		}
		
		realLogger.debug ("stopping amqp consumer agent");
		agent.stop ();
		
		realLogger.debug ("joining amqp consumer agent");
		for (int tries = 0; tries < AmqpConsumerAgentTests.timeoutTries; tries++) {
			if (!agent.isRunning ())
				break;
			Thread.sleep (AmqpConsumerAgentTests.timeout);
		}
		
		Assert.assertTrue (agent.isDrained ());
		Assert.assertFalse (agent.isStarted ());
		Assert.assertFalse (agent.isRunning ());
		
		testLogger.detachAppender (collector);
	}
	
	private static final int messageCount = 20;
	private static final String testLoggerName = "__testing__.eu.arkitech.logback.amqp.logger";
	private static final int timeout = 100;
	private static final int timeoutTries = 100;
}
