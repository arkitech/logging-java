/*
 * #%L
 * arkitech-logging-conapp
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

package eu.arkitech.logging.conapp;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.arkitech.logback.amqp.accessors.AmqpAccessorAppender;
import eu.arkitech.logback.amqp.consumer.AmqpConsumerAppender;
import eu.arkitech.logback.amqp.publisher.AmqpPublisherAppender;
import eu.arkitech.logging.datastore.bdb.BdbDatastoreAppender;
import eu.arkitech.logging.datastore.common.DatastoreAppender;
import eu.arkitech.logging.datastore.lucene.LuceneDatastoreAppender;
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
		final List<AmqpAccessorAppender> amqpAppenders = Collections.synchronizedList (new LinkedList<AmqpAccessorAppender> ());
		AmqpConsumerAppender.CreateAction.defaultCollector = amqpAppenders;
		AmqpPublisherAppender.CreateAction.defaultCollector = amqpAppenders;
		final List<DatastoreAppender> datastoreAppenders = Collections.synchronizedList (new LinkedList<DatastoreAppender> ());
		BdbDatastoreAppender.CreateAction.defaultCollector = datastoreAppenders;
		LuceneDatastoreAppender.CreateAction.defaultCollector = datastoreAppenders;
		LoggerFactory.getILoggerFactory ();
		if (amqpAppenders.isEmpty ())
			throw (new IllegalArgumentException ("no amqp accessors defined; aborting!"));
		while (true) {
			boolean stillRunning = false;
			for (final AmqpAccessorAppender accessor : amqpAppenders)
				stillRunning |= accessor.isRunning ();
			if (!stillRunning)
				break;
			try {
				Thread.sleep (ConappMain.defaultWaitTimeout);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		for (final DatastoreAppender appender : datastoreAppenders)
			appender.stop ();
		System.exit (1);
	}
	
	public static final long defaultWaitTimeout = 1000;
}
