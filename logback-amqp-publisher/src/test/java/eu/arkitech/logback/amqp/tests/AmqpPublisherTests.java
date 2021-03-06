/*
 * #%L
 * arkitech-logback-amqp-publisher
 * %%
 * Copyright (C) 2011 - 2012 Arkitech
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.arkitech.logback.amqp.tests;


import ch.qos.logback.classic.Logger;
import eu.arkitech.logback.amqp.publisher.AmqpPublisherAppender;
import eu.arkitech.logback.common.DefaultLoggingEventMutator;
import eu.arkitech.logback.common.RandomGenerator;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.junit.Assert;
import org.junit.Test;


public final class AmqpPublisherTests
{
	@Test
	public final void testPublisherAppender ()
			throws Exception
	{
		final Logger realLogger = (Logger) LoggerFactory.getLogger (AmqpPublisherTests.class.getName ());
		final Logger testLogger = (Logger) LoggerFactory.getLogger (AmqpPublisherTests.testLoggerName);
		realLogger.debug ("initializing amqp appender");
		final AmqpPublisherAppender appender = new AmqpPublisherAppender ();
		appender.setName (String.format ("%s@%x", appender.getClass ().getName (), System.identityHashCode (appender)));
		appender.setContext (testLogger.getLoggerContext ());
		appender.start ();
		testLogger.addAppender (appender);
		testLogger.setAdditive (false);
		final RandomGenerator generator = new RandomGenerator (testLogger);
		realLogger.debug ("logging generated messages");
		MDC.clear ();
		for (int index = 0; index < AmqpPublisherTests.messageCount; index++) {
			MDC.put (DefaultLoggingEventMutator.defaultApplicationMdcName, String.format ("app-%d", (index % 3) + 1));
			MDC.put (DefaultLoggingEventMutator.defaultComponentMdcName, String.format ("comp-%d", (index % 2) + 1));
			testLogger.callAppenders (generator.generate ());
			MDC.clear ();
		}
		realLogger.debug ("waiting for message draining (i.e. until their all sent)");
		for (int tries = 0; tries < AmqpPublisherTests.timeoutTries; tries++) {
			if (appender.isDrained ())
				break;
			Thread.sleep (AmqpPublisherTests.timeout);
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
	private static final int timeoutTries = 100;
}
