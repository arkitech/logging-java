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


import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.WorkerThread.State;


public abstract class Worker
{
	protected Worker (final WorkerConfiguration configuration)
	{
		super ();
		Preconditions.checkNotNull (configuration);
		final Object monitor = (configuration.monitor != null) ? configuration.monitor : new Object ();
		this.monitor = monitor;
		this.callbacks = (configuration.callbacks != null) ? configuration.callbacks : new DefaultLoggerCallbacks (this);
		this.waitTimeout = WorkerConfiguration.defaultWaitTimeout;
		this.thread = new InternalThread ();
	}
	
	public final boolean awaitStop ()
	{
		return (this.awaitStop (0));
	}
	
	public final boolean awaitStop (final long timeout)
	{
		Preconditions.checkState (!this.isCurrentThread ());
		try {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "worker awaiting stop");
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
	
	public final boolean requestStop ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "worker requesting stop");
			return (this.thread.requestStopSoft ());
		}
	}
	
	public final boolean start ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "worker starting");
			this.thread.start ();
		}
		return (this.isRunning ());
	}
	
	protected abstract void executeLoop ();
	
	protected abstract void finalizeLoop ();
	
	protected abstract void initializeLoop ();
	
	protected final boolean isCurrentThread ()
	{
		return (this.thread.isCurrentThread ());
	}
	
	protected final boolean shouldStopHard ()
	{
		return (this.thread.shouldStopHard ());
	}
	
	protected boolean shouldStopSoft ()
	{
		return (this.thread.shouldStopSoft ());
	}
	
	protected final Callbacks callbacks;
	protected final Object monitor;
	protected final int waitTimeout;
	private final InternalThread thread;
	
	private final class InternalThread
			extends WorkerThread
	{
		protected InternalThread ()
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
			Worker.this.callbacks.handleException (exception, "worker encountered an unknown error while running");
		}
		
		@Override
		protected final void initializeLoop ()
		{
			Worker.this.initializeLoop ();
		}
	}
}
