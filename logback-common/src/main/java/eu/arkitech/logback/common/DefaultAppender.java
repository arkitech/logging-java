/*
 * #%L
 * arkitech-logback-common
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

package eu.arkitech.logback.common;


import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;


public abstract class DefaultAppender
		extends UnsynchronizedAppenderBase<ILoggingEvent>
		implements
			LoggingEventSink
{
	public DefaultAppender ()
	{
		super ();
		this.callbacks = new DefaultContextAwareCallbacks (this);
	}
	
	@Override
	public boolean push (final ILoggingEvent event)
	{
		return (this.doPush (event));
	}
	
	@Override
	public boolean push (final ILoggingEvent event, final long timeout, final TimeUnit timeoutUnit)
	{
		return (this.doPush (event));
	}
	
	@Override
	public void start ()
	{
		this.reallyStart ();
		super.start ();
	}
	
	@Override
	public void stop ()
	{
		this.reallyStop ();
		super.stop ();
	}
	
	@Override
	protected void append (final ILoggingEvent event)
	{
		try {
			this.reallyAppend (event);
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "appender encountered an error while appending the event; ignoring!");
		}
	}
	
	protected abstract void reallyAppend (final ILoggingEvent event);
	
	protected abstract boolean reallyStart ();
	
	protected abstract boolean reallyStop ();
	
	private final boolean doPush (final ILoggingEvent event)
	{
		this.doAppend (event);
		return (true);
	}
	
	protected final Callbacks callbacks;
}
