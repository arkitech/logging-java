
package eu.arkitech.logback.common;


import java.util.HashSet;

import com.google.common.base.Preconditions;


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
			this.exceptionHandler = new ExceptionHandler ();
			super.setName (WorkerThread.generateName (name, this.identifier));
			super.setUncaughtExceptionHandler (this.exceptionHandler);
			super.setDaemon (true);
			super.setPriority ((priority != null) ? priority : WorkerThread.defaultPriority);
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
	
	public final boolean isCurrentThread ()
	{
		return (this == Thread.currentThread ());
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
			case Stopping :
			case Running :
				return (true);
			default:
				return (false);
		}
	}
	
	public final boolean requestStopHard ()
	{
		this.shouldStopHard = true;
		return (this.requestStopSoft ());
	}
	
	public final boolean requestStopSoft ()
	{
		synchronized (this) {
			switch (this.state) {
				case Created :
					throw (new IllegalStateException ("worker thread is not started"));
				case Stopped :
					return (false);
				case Starting :
				case Running :
					this.state = State.Stopping;
					return (true);
				case Stopping :
					return (true);
				default:
					throw (new IllegalStateException ("worker thread is in an unknown state"));
			}
		}
	}
	
	@Override
	public final void run ()
	{
		ShutdownHandler.handler.isAlive ();
		synchronized (WorkerThread.instances) {
			WorkerThread.instances.add (this);
		}
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
		synchronized (WorkerThread.instances) {
			WorkerThread.instances.remove (this);
		}
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
		return (this.shouldStopHard);
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
			} catch (final Error exception2) {}
		}
	}
	
	private final ExceptionHandler exceptionHandler;
	private final long identifier;
	private boolean shouldStopHard;
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
	public static final long stopHardEnforceTimeout = 5 * 1000;
	public static final long stopHardRequestTimeout = 30 * 1000;
	private static long identifierCounter = 0;
	private static final Object identifierCounterMonitor = new Object ();
	
	private static final HashSet<WorkerThread> instances = new HashSet<WorkerThread> ();
	
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
	
	@SuppressWarnings ("deprecation")
	private static final class ShutdownHandler
			extends Thread
	{
		private ShutdownHandler ()
		{
			super ();
			this.setName (ShutdownHandler.class.getName ());
			Runtime.getRuntime ().addShutdownHook (this);
		}
		
		@Override
		public final void run ()
		{
			Preconditions.checkState (ShutdownHandler.handler == this);
			final long shutdownTimestamp = System.currentTimeMillis ();
			final long stopHardRequestTimestamp = shutdownTimestamp + WorkerThread.stopHardRequestTimeout;
			final long stopHardEnforceTimestamp = stopHardRequestTimestamp + WorkerThread.stopHardEnforceTimeout;
			synchronized (WorkerThread.instances) {
				for (final WorkerThread thread : WorkerThread.instances)
					thread.requestStopSoft ();
			}
			while (true) {
				synchronized (WorkerThread.instances) {
					if (WorkerThread.instances.size () == 0)
						return;
				}
				try {
					Thread.sleep (ShutdownHandler.defaultWaitTimeout);
				} catch (final InterruptedException exception) {}
				final long currentTimestamp = System.currentTimeMillis ();
				if (currentTimestamp >= stopHardRequestTimestamp)
					break;
			}
			synchronized (WorkerThread.instances) {
				for (final WorkerThread thread : WorkerThread.instances)
					thread.requestStopHard ();
			}
			while (true) {
				synchronized (WorkerThread.instances) {
					if (WorkerThread.instances.size () == 0)
						return;
				}
				try {
					Thread.sleep (ShutdownHandler.defaultWaitTimeout);
				} catch (final InterruptedException exception) {}
				final long currentTimestamp = System.currentTimeMillis ();
				if (currentTimestamp >= stopHardEnforceTimestamp)
					break;
			}
			synchronized (WorkerThread.instances) {
				for (final WorkerThread thread : WorkerThread.instances)
					thread.stop ();
			}
		}
		
		static final ShutdownHandler handler = new ShutdownHandler ();
		private static final long defaultWaitTimeout = 100;
	}
}
