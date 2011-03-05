
package eu.arkitech.logback.common;


public abstract class WorkerThread
		extends Thread
{
	public WorkerThread ()
	{
		this (null, null, null, null);
	}
	
	public WorkerThread (final String name)
	{
		this (name, null, null, null);
	}
	
	public WorkerThread (final String name, final Integer priority)
	{
		this (name, null, priority, null);
	}
	
	public WorkerThread (final String name, final ThreadGroup group, final Integer priority)
	{
		this (name, group, priority, null);
	}
	
	public WorkerThread (final String name, final ThreadGroup group, final Integer priority, final Long stackSize)
	{
		super (group, null, WorkerThread.generateName (name, 0), (stackSize != null) ? stackSize : WorkerThread.defaultStackSize);
		synchronized (this) {
			this.state = State.Created;
			this.identifier = WorkerThread.generateIdentifier ();
			this.shutdownHandler = new ShutdownHandler ();
			this.exceptionHandler = new ExceptionHandler ();
			super.setName (WorkerThread.generateName (name, this.identifier));
			super.setUncaughtExceptionHandler (this.exceptionHandler);
			super.setDaemon (true);
			super.setPriority ((priority != null) ? priority : WorkerThread.defaultPriority);
			this.shutdownHandler.setName (this.getName () + "@sh");
		}
	}
	
	@Override
	@SuppressWarnings ("deprecation")
	public final int countStackFrames ()
	{
		// throw (new UnsupportedOperationException ());
		return (super.countStackFrames ());
	}
	
	@Override
	@SuppressWarnings ("deprecation")
	public final void destroy ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final boolean equals (final Object object)
	{
		return (this == object);
	}
	
	@Override
	public final ClassLoader getContextClassLoader ()
	{
		return (super.getContextClassLoader ());
	}
	
	@Override
	public final long getId ()
	{
		return (super.getId ());
	}
	
	@Override
	public final StackTraceElement[] getStackTrace ()
	{
		// throw (new UnsupportedOperationException ());
		return (super.getStackTrace ());
	}
	
	@Override
	public final Thread.State getState ()
	{
		return (super.getState ());
	}
	
	@Override
	public final UncaughtExceptionHandler getUncaughtExceptionHandler ()
	{
		// throw (new UnsupportedOperationException ());
		return (super.getUncaughtExceptionHandler ());
	}
	
	public final State getWorkerState ()
	{
		return (this.state);
	}
	
	@Override
	public final int hashCode ()
	{
		return (super.hashCode ());
	}
	
	@Override
	public final void interrupt ()
	{
		// throw (new UnsupportedOperationException ());
		super.interrupt ();
	}
	
	@Override
	public final boolean isInterrupted ()
	{
		// throw (new UnsupportedOperationException ());
		return (super.isInterrupted ());
	}
	
	public final boolean isRunning ()
	{
		switch (this.state) {
			case Starting :
			case Running :
				return (true);
			default:
				return (false);
		}
	}
	
	public final void requestStop ()
	{
		synchronized (this) {
			switch (this.state) {
				case Created :
					throw (new IllegalStateException ("worker thread is not started"));
				case Starting :
				case Running :
					this.state = State.Stopping;
					break;
				case Stopping :
				case Stopped :
					break;
			}
		}
	}
	
	@Override
	public final void run ()
	{
		if (Thread.currentThread () != this)
			throw (new IllegalStateException ());
		synchronized (this) {
			if (this.state != State.Starting)
				throw (new IllegalStateException ());
			this.state = State.Running;
		}
		boolean initializeSucceeded = false;
		try {
			this.initializeLoop ();
			initializeSucceeded = true;
		} catch (final Throwable exception) {
			this.delegateHandleException (exception);
		}
		boolean loopSucceeded = false;
		if (initializeSucceeded) {
			try {
				this.executeLoop ();
				loopSucceeded = true;
			} catch (final Throwable exception) {
				this.delegateHandleException (exception);
			}
		}
		boolean finalizeSucceeded = false;
		try {
			this.finalizeLoop ();
			finalizeSucceeded = true;
		} catch (final Throwable exception) {
			this.delegateHandleException (exception);
		}
		@SuppressWarnings ("unused") final boolean succeeded = initializeSucceeded && loopSucceeded && finalizeSucceeded;
		synchronized (this) {
			if ((this.state != State.Running) && (this.state != State.Stopping))
				throw (new IllegalStateException ());
			this.state = State.Stopped;
		}
		try {
			Runtime.getRuntime ().removeShutdownHook (this.shutdownHandler);
		} catch (final IllegalStateException exception) {}
	}
	
	@Override
	public final void setContextClassLoader (final ClassLoader loader)
	{
		// throw (new UnsupportedOperationException ());
		super.setContextClassLoader (loader);
	}
	
	@Override
	public final void setUncaughtExceptionHandler (final UncaughtExceptionHandler handler)
	{
		// throw (new UnsupportedOperationException ());
		super.setUncaughtExceptionHandler (handler);
	}
	
	public final boolean shouldStopHard ()
	{
		return (this.shutdownHandler.isStarted ());
	}
	
	public boolean shouldStopSoft ()
	{
		return (this.shouldStopHard () || (this.state == State.Stopping));
	}
	
	@Override
	public final synchronized void start ()
	{
		synchronized (this) {
			if (this.state == State.Stopped)
				throw (new IllegalStateException ("worker thread is stopped"));
			if (this.state != State.Created)
				throw (new IllegalStateException ("worker thread is already started"));
			this.state = State.Starting;
			try {
				Runtime.getRuntime ().addShutdownHook (this.shutdownHandler);
			} catch (final IllegalStateException exception) {
				this.delegateHandleException (exception);
			}
			this.shutdownHandler.setName (this.getName () + "@sh");
			super.start ();
		}
	}
	
	@Override
	public final String toString ()
	{
		return (super.toString ());
	}
	
	@Override
	protected final Object clone ()
			throws CloneNotSupportedException
	{
		throw (new CloneNotSupportedException ());
	}
	
	protected abstract void executeLoop ()
			throws Throwable;
	
	@Override
	protected final void finalize ()
			throws Throwable
	{
		super.finalize ();
	}
	
	protected abstract void finalizeLoop ()
			throws Throwable;
	
	protected abstract void handleException (final Throwable exception)
			throws Throwable;
	
	protected abstract void initializeLoop ()
			throws Throwable;
	
	private final void delegateHandleException (final Throwable exception)
	{
		try {
			this.handleException (exception);
		} catch (final Throwable exception1) {
			try {
				this.getThreadGroup ().uncaughtException (this, exception1);
			} catch (final Throwable exception2) {}
		}
	}
	
	private final void handleShutdown ()
	{
		try {
			this.requestStop ();
		} catch (final Throwable exception) {
			this.delegateHandleException (exception);
		}
		while (true) {
			try {
				this.join ();
			} catch (final InterruptedException exception) {
				this.delegateHandleException (exception);
			}
			if (!this.isAlive ())
				break;
		}
	}
	
	private final ExceptionHandler exceptionHandler;
	private final long identifier;
	private final ShutdownHandler shutdownHandler;
	private State state;
	
	private static final long generateIdentifier ()
	{
		final long identifier;
		synchronized (WorkerThread.identifierCounterMonitor) {
			identifier = WorkerThread.identifierCounter++;
		}
		return (identifier);
	}
	
	private static final String generateName (final String name, final long identifier)
	{
		return (String.format ("%s@%s", (name != null) ? name : WorkerThread.class.getName (), identifier));
	}
	
	public static final int defaultPriority = Thread.MIN_PRIORITY;
	public static final long defaultStackSize = 0;
	private static long identifierCounter = 0;
	private static final Object identifierCounterMonitor = new Object ();
	
	public static enum State
	{
		Created,
		Running,
		Starting,
		Stopped,
		Stopping;
	}
	
	private final class ExceptionHandler
			implements
				UncaughtExceptionHandler
	{
		@Override
		public final void uncaughtException (final Thread thread, final Throwable exception)
		{
			if (thread != WorkerThread.this)
				throw (new IllegalStateException ());
			WorkerThread.this.delegateHandleException (exception);
		}
	}
	
	private final class ShutdownHandler
			extends Thread
	{
		public final boolean isStarted ()
		{
			return (this.started);
		}
		
		@Override
		public final void run ()
		{
			this.started = true;
			WorkerThread.this.handleShutdown ();
		}
		
		private boolean started;
	}
}
