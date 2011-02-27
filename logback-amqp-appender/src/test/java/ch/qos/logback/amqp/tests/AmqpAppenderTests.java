
package ch.qos.logback.amqp.tests;


import java.util.UUID;

import ch.qos.logback.amqp.tools.DefaultMutator;

import org.slf4j.MDC;

import ch.qos.logback.amqp.AmqpAppender;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Assert;
import org.junit.Test;


public final class AmqpAppenderTests
{
	@Test
	public final void testAppender ()
			throws Throwable
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.class.getName ());
		
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpAppenderTests.testLoggerName);
		
		realLogger.debug ("initializing amqp appender");
		final AmqpAppender appender = new AmqpAppender ();
		appender.setName (String.format ("%s@%x", appender.getClass ().getName (), System.identityHashCode (appender)));
		appender.setContext (testLogger.getLoggerContext ());
		appender.start ();
		
		testLogger.addAppender (appender);
		testLogger.setAdditive (false);
		
		realLogger.debug ("logging generated messages");
		for (int index = 0; index < AmqpAppenderTests.messageCount; index++) {
			MDC.put (DefaultMutator.applicationKey, String.format ("app-%d", index % 3 + 1));
			MDC.put (DefaultMutator.componentKey, String.format ("comp-%d", index % 2 + 1));
			testLogger.error (UUID.randomUUID ().toString ());
		}
		
		realLogger.debug ("waiting for message draining (i.e. until their all sent)");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (appender.isDrained ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		realLogger.debug ("stopping amqp appender");
		appender.stop ();
		
		realLogger.debug ("joining amqp appender");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (!appender.isRunning ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		Assert.assertTrue (appender.isDrained ());
		Assert.assertFalse (appender.isStarted ());
		Assert.assertFalse (appender.isRunning ());
		
		testLogger.detachAppender (appender);
	}
	
	private static final int messageCount = 20;
	private static final String testLoggerName = "__testing__.ch.qos.logback.amqp.logger";
	private static final int timeout = 100;
	private static final int timeoutTries = 100;
}
