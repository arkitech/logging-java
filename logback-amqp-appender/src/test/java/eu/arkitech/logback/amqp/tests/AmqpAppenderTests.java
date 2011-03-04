
package eu.arkitech.logback.amqp.tests;


import ch.qos.logback.classic.Logger;
import eu.arkitech.logback.amqp.appender.AmqpAppender;
import eu.arkitech.logback.common.DefaultLoggingEventMutator;
import eu.arkitech.logback.common.RandomGenerator;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
		
		final RandomGenerator generator = new RandomGenerator (AmqpAppenderTests.class.getName (), testLogger);
		realLogger.debug ("logging generated messages");
		MDC.clear ();
		for (int index = 0; index < AmqpAppenderTests.messageCount; index++) {
			MDC.put (DefaultLoggingEventMutator.applicationKey, String.format ("app-%d", index % 3 + 1));
			MDC.put (DefaultLoggingEventMutator.componentKey, String.format ("comp-%d", index % 2 + 1));
			testLogger.callAppenders (generator.generate ());
			MDC.clear ();
		}
		
		realLogger.debug ("waiting for message draining (i.e. until their all sent)");
		for (int tries = 0; tries < AmqpAppenderTests.timeoutTries; tries++) {
			if (appender.isDrained ())
				break;
			Thread.sleep (AmqpAppenderTests.timeout);
		}
		
		realLogger.debug ("stopping and joining amqp appender");
		appender.stop ();
		
		Assert.assertTrue (appender.isDrained ());
		Assert.assertFalse (appender.isStarted ());
		Assert.assertFalse (appender.isRunning ());
		
		testLogger.detachAppender (appender);
	}
	
	private static final int messageCount = 20;
	private static final String testLoggerName = "__testing__.eu.ackitech.logback.amqp.logger";
	private static final int timeout = 100;
	private static final int timeoutTries = 100 * 1000;
}
