
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
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "appender encountered an error while appending the event; ignoring!");
		}
	}
	
	protected abstract void reallyAppend (final ILoggingEvent event)
			throws Throwable;
	
	protected abstract boolean reallyStart ();
	
	protected abstract boolean reallyStop ();
	
	private final boolean doPush (final ILoggingEvent event)
	{
		this.doAppend (event);
		return (true);
	}
	
	protected final Callbacks callbacks;
}
