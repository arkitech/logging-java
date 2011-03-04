
package eu.arkitech.logback.common;


import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;


public abstract class DefaultAppenderSink
		extends UnsynchronizedAppenderBase<ILoggingEvent>
		implements
			LoggingEventSink
{
	public DefaultAppenderSink ()
	{
		super ();
		this.callbacks = new DefaultContextAwareCallbacks (this);
	}
	
	public boolean push (final ILoggingEvent event)
	{
		return (this.doPush (event));
	}
	
	public boolean push (final ILoggingEvent event, final long timeout, final TimeUnit timeoutUnit)
	{
		return (this.doPush (event));
	}
	
	protected boolean doPush (final ILoggingEvent event)
	{
		this.doAppend (event);
		return (true);
	}
	
	protected final Callbacks callbacks;
}
