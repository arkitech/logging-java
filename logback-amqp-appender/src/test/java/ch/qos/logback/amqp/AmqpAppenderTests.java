
package ch.qos.logback.amqp;


import java.util.LinkedList;
import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import junit.framework.Assert;
import org.slf4j.LoggerFactory;

import org.junit.Test;


public final class AmqpAppenderTests
{
	@Test
	public final void testPublisherConsumer ()
			throws Throwable
	{
		this.generatedMessages = new LinkedList<String> ();
		for (int index = 0; index < AmqpAppenderTests.messageCount; index++)
			this.generatedMessages.add (UUID.randomUUID ().toString ());
		
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.testLoggerName);
		testLogger.setAdditive (false);
		
		this.testAppender ();
		this.testAgent ();
	}
	
	private final void testAgent ()
			throws Throwable
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.class.getName () + ".testAgent");
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.testLoggerName);
		
		realLogger.debug ("registering collector appender");
		final LinkedList<String> collectedMessages = new LinkedList<String> ();
		final Appender<ILoggingEvent> appender = new AppenderBase<ILoggingEvent> () {
			protected final void append (final ILoggingEvent event)
			{
				collectedMessages.add (event.getMessage ());
			}
		};
		appender.setContext (testLogger.getLoggerContext ());
		testLogger.addAppender (appender);
		appender.start ();
		
		realLogger.debug ("initializing amqp consumer agent");
		final AmqpConsumerAgent agent = new AmqpConsumerAgent ();
		agent.setContext (testLogger.getLoggerContext ());
		
		realLogger.debug ("starting amqp consumer agent");
		agent.start ();
		
		realLogger.debug ("waiting for draining collected messages");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (collectedMessages.size () >= this.generatedMessages.size ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		realLogger.debug ("stopping amqp consumer agent");
		agent.stop ();
		
		realLogger.debug ("joining amqp consumer agent");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (!agent.isStarted ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		Assert.assertTrue (agent.isDrained ());
		Assert.assertFalse (agent.isStarted ());
		Assert.assertTrue (collectedMessages.equals (this.generatedMessages));
		
		testLogger.detachAppender (appender);
	}
	
	private final void testAppender ()
			throws Throwable
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.class.getName () + ".testAppender");
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.testLoggerName);
		
		realLogger.debug ("initializing amqp appender");
		final AmqpAppender appender = new AmqpAppender ();
		appender.setContext (testLogger.getLoggerContext ());
		testLogger.addAppender (appender);
		
		realLogger.debug ("starting amqp appender");
		appender.start ();
		
		realLogger.debug ("logging generated messages");
		for (final String message : this.generatedMessages)
			testLogger.error (message);
		
		realLogger.debug ("waiting for draining logged messages");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (appender.isDrained ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		realLogger.debug ("stopping amqp appender");
		appender.stop ();
		
		realLogger.debug ("joining amqp appender");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (!appender.isStarted ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		Assert.assertTrue (appender.isDrained ());
		Assert.assertFalse (appender.isStarted ());
		
		testLogger.detachAppender (appender);
	}
	
	private LinkedList<String> generatedMessages;
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		AmqpConsumerAgent.main (arguments);
	}
	
	private static final int messageCount = 1000;
	private static final String testLoggerName = UUID.randomUUID ().toString ();
	private static final int timeout = 100;
	private static final int timeoutTries = 100;
}
