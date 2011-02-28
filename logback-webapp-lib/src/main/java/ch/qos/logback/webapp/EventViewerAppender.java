
package ch.qos.logback.webapp;


import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;


public class EventViewerAppender
		extends AppenderBase<ILoggingEvent>
{
	public EventViewerAppender ()
	{
		this (new LinkedList<ILoggingEvent> ());
	}
	
	public EventViewerAppender (final LinkedList<ILoggingEvent> events)
	{
		super ();
		this.events = events;
		this.buffer = new LinkedBlockingQueue<ILoggingEvent> ();
	}
	
	public void drainEvents ()
	{
		while (true) {
			final ILoggingEvent event = this.buffer.poll ();
			if (event == null)
				break;
			this.events.add (event);
		}
	}
	
	public final LinkedList<ILoggingEvent> getEvents ()
	{
		return (this.events);
	}
	
	public void start ()
	{
		if (this.isStarted ())
			return;
		super.start ();
		if (this.context.getObject (this.name) != null)
			throw (new IllegalArgumentException (String.format ("duplicate object name found `%s`", this.name)));
		this.context.putObject (this.name, this);
	}
	
	public void stop ()
	{
		if (!this.isStarted ())
			return;
		this.context.putObject (this.name, null);
		super.stop ();
	}
	
	protected void append (final ILoggingEvent event)
	{
		this.buffer.add (event);
	}
	
	public final Object monitor = new Object ();
	private final LinkedBlockingQueue<ILoggingEvent> buffer;
	private final LinkedList<ILoggingEvent> events;
}
