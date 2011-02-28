
package ch.qos.logback.amqp.tests;


import java.util.LinkedList;

import ch.qos.logback.amqp.AmqpConsumerAgent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
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
		final LinkedList<String> collectedMessages = new LinkedList<String> ();
		final Appender<ILoggingEvent> collectorAppender = new AppenderBase<ILoggingEvent> () {
			protected final void append (final ILoggingEvent event)
			{
				collectedMessages.add (event.getMessage ());
			}
		};
		collectorAppender.setName (String.format (
				"%s@%x", collectorAppender.getClass ().getName (), System.identityHashCode (collectorAppender)));
		collectorAppender.setContext (testLogger.getLoggerContext ());
		collectorAppender.start ();
		
		testLogger.addAppender (collectorAppender);
		testLogger.setAdditive (false);
		
		realLogger.debug ("initializing amqp consumer agent");
		final AmqpConsumerAgent agent = new AmqpConsumerAgent ();
		agent.setContext (testLogger.getLoggerContext ());
		agent.start ();
		
		realLogger.debug ("waiting for message draining (i.e. until we receive enough)");
		for (int tries = 0; tries < AmqpConsumerAgentTests.timeoutTries; tries++) {
			if (collectedMessages.size () >= AmqpConsumerAgentTests.messageCount)
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
		
		testLogger.detachAppender (collectorAppender);
	}
	
	private static final int messageCount = 20;
	private static final String testLoggerName = "__testing__.ch.qos.logback.amqp.logger";
	private static final int timeout = 100;
	private static final int timeoutTries = 100;
}
