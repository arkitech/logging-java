
package eu.arkitech.logback.common;


import ch.qos.logback.classic.Level;
import eu.arkitech.logback.common.WorkerThread.State;


public abstract class Worker
{
	protected Worker (final Callbacks callbacks, final Object monitor)
	{
		super ();
		final Object monitor_ = (monitor != null) ? monitor : new Object ();
		synchronized (monitor_) {
			this.monitor = monitor_;
			this.callbacks = callbacks;
			this.waitTimeout = Worker.defaultWaitTimeout;
			this.thread = new ShovelThread ();
		}
	}
	
	public final boolean awaitStop ()
	{
		return (this.awaitStop (0));
	}
	
	public final boolean awaitStop (final long timeout)
	{
		try {
			this.thread.join (timeout);
		} catch (final InterruptedException exception) {}
		return (!this.isRunning ());
	}
	
	public final State getState ()
	{
		return (this.thread.getWorkerState ());
	}
	
	public final boolean isRunning ()
	{
		return (this.thread.isRunning ());
	}
	
	public final void requestStop ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.INFO, null, "worker stopping");
			this.thread.requestStop ();
		}
	}
	
	public final boolean start ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.INFO, null, "worker starting");
			this.thread.start ();
		}
		return (this.thread.isRunning ());
	}
	
	protected abstract void executeLoop ();
	
	protected abstract void finalizeLoop ();
	
	protected abstract void initializeLoop ();
	
	protected final boolean shouldStopHard ()
	{
		return (this.thread.shouldStopHard ());
	}
	
	protected boolean shouldStopSoft ()
	{
		return (this.thread.shouldStopSoft ());
	}
	
	public final Object monitor;
	protected final Callbacks callbacks;
	protected final int waitTimeout;
	private final ShovelThread thread;
	
	public static final int defaultWaitTimeout = 500;
	
	private final class ShovelThread
			extends WorkerThread
	{
		protected ShovelThread ()
		{
			super (String.format ("%s@%s", Worker.this.getClass ().getName (), System.identityHashCode (Worker.this)));
		}
		
		@Override
		protected final void executeLoop ()
		{
			Worker.this.executeLoop ();
		}
		
		@Override
		protected final void finalizeLoop ()
		{
			Worker.this.finalizeLoop ();
		}
		
		@Override
		protected final void handleException (final Throwable exception)
		{
			Worker.this.callbacks.handleException (exception, "worker encountered an unknown error while running; aborting");
		}
		
		@Override
		protected final void initializeLoop ()
		{
			Worker.this.initializeLoop ();
		}
	}
}
