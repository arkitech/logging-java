/*
 * #%L
 * arkitech-logging-datastore-bdb
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

package eu.arkitech.logging.datastore.bdb;


import java.io.File;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.AppenderNewInstanceAction;
import eu.arkitech.logback.common.DefaultSerializerAppender;
import eu.arkitech.logging.datastore.common.Datastore;
import eu.arkitech.logging.datastore.common.DatastoreAppender;


public class BdbDatastoreAppender
		extends DefaultSerializerAppender
		implements
			DatastoreAppender
{
	public BdbDatastoreAppender ()
	{
		super ();
		this.readOnly = false;
	}
	
	@Override
	public Datastore getDatastore ()
	{
		return (this.datastore);
	}
	
	public String getEnvironmentPath ()
	{
		return (this.environmentPath);
	}
	
	public Boolean getReadOnly ()
	{
		return (this.readOnly);
	}
	
	public Long getSyncTimeout ()
	{
		return (this.syncTimeout);
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (true);
	}
	
	public void setEnvironmentPath (final String environmentPath)
	{
		this.environmentPath = environmentPath;
	}
	
	public void setReadOnly (final Boolean readOnly)
	{
		this.readOnly = readOnly;
	}
	
	public void setSyncTimeout (final Long syncTimeout)
	{
		this.syncTimeout = syncTimeout;
	}
	
	protected BdbDatastoreConfiguration buildConfiguration ()
	{
		return (new BdbDatastoreConfiguration ((this.environmentPath != null) ? new File (this.environmentPath) : null, this.readOnly, true, this.syncTimeout, this.serializer, this.mutator, this.mutator, this.callbacks, null));
	}
	
	@Override
	protected final void reallyAppend (final ILoggingEvent event)
	{
		this.datastore.store (event);
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			try {
				if (this.datastore != null)
					throw (new IllegalStateException ());
				this.datastore = new BdbDatastore (this.buildConfiguration ());
				final boolean succeeded = this.datastore.open ();
				if (!succeeded) {
					this.reallyStop ();
					return (false);
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore appender encountered an unknown error while starting; aborting!");
				this.reallyStop ();
				return (false);
			}
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			try {
				if (this.datastore != null) {
					this.datastore.close ();
					this.datastore = null;
				}
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore appender encountered an unknown error while stopping; ignoring");
				return (false);
			} finally {
				this.datastore = null;
			}
		}
	}
	
	protected String environmentPath;
	protected Boolean readOnly;
	protected Long syncTimeout;
	private BdbDatastore datastore;
	
	public static final class CreateAction
			extends AppenderNewInstanceAction<BdbDatastoreAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<? super BdbDatastoreAppender> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (BdbDatastoreAppender.class, collector, autoRegister, autoStart);
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<? super BdbDatastoreAppender> defaultCollector = null;
	}
}
