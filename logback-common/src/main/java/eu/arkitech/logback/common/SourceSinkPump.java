
package eu.arkitech.logback.common;


import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Preconditions;


public final class SourceSinkPump
		extends Worker
{
	public SourceSinkPump (final WorkerConfiguration configuration, final LoggingEventSource source, final LoggingEventSink sink)
	{
		super (configuration);
		this.source = source;
		this.sink = sink;
	}
	
	public final void start (final long delay)
	{
		Preconditions.checkArgument (delay >= 0);
		if (delay == 0)
			this.start ();
		else {
			final Thread thread = new Thread () {
				@Override
				public final void run ()
				{
					try {
						Thread.sleep (delay);
					} catch (final InterruptedException exception) {
						return;
					}
					SourceSinkPump.this.start ();
				}
			};
			thread.start ();
		}
	}
	
	@Override
	protected final void executeLoop ()
	{
		while (true) {
			if (this.shouldStopSoft () || this.shouldStopHard ())
				break;
			final ILoggingEvent event;
			try {
				event = this.source.pull (this.waitTimeout, TimeUnit.MILLISECONDS);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "event pump encountered an unknown error while pulling the event from the source; ignoring!");
				continue;
			}
			if (event == null)
				continue;
			try {
				this.sink.push (event);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "event pump encountered an unknown error while pushing the event to the sink; ignoring!");
			}
		}
	}
	
	@Override
	protected final void finalizeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "event pump stopped");
	}
	
	@Override
	protected final void initializeLoop ()
	{
		this.callbacks.handleLogEvent (Level.DEBUG, null, "event pump started");
	}
	
	@Override
	protected final boolean shouldStopSoft ()
	{
		try {
			return ((this.source.isDrained () && super.shouldStopSoft ()) || this.shouldStopHard ());
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "event pump encountered an unknown error while checking the source; ignoring!");
			return (true);
		}
	}
	
	private final LoggingEventSink sink;
	private final LoggingEventSource source;
}
