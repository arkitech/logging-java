
package eu.arkitech.logback.common;


import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;


public final class LoggingEventPump
		extends Worker
{
	public LoggingEventPump (final WorkerConfiguration configuration, final LoggingEventSource source, final LoggingEventSink sink)
	{
		super (configuration);
		this.source = source;
		this.sink = sink;
		this.waitTimeout = LoggingEventPump.defaultWaitTimeout;
	}
	
	@Override
	protected final void executeLoop ()
	{
		while (true) {
			if (this.shouldStopSoft ())
				break;
			final ILoggingEvent event;
			try {
				event = this.source.pull (this.waitTimeout, TimeUnit.MILLISECONDS);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "logging event pump encountered an error while pulling the event from the source; ignoring!");
				continue;
			}
			if (event == null)
				continue;
			try {
				this.sink.push (event);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "logging event pump encountered an error while pushing the event to the sink; ignoring");
			}
		}
	}
	
	@Override
	protected final void finalizeLoop ()
	{}
	
	@Override
	protected final void initializeLoop ()
	{}
	
	@Override
	protected final boolean shouldStopSoft ()
	{
		final boolean isDrained;
		try {
			isDrained = this.source.isDrained ();
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "loging event pump encountered an error while checking the source; ignoring!");
			return (true);
		}
		return (super.shouldStopSoft () && isDrained);
	}
	
	private final LoggingEventSink sink;
	private final LoggingEventSource source;
	private final long waitTimeout;
	
	public static final long defaultWaitTimeout = 1000;
}
