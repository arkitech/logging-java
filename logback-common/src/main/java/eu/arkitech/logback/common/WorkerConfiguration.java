
package eu.arkitech.logback.common;


import com.google.common.base.Objects;


public class WorkerConfiguration
		implements
			Configuration
{
	public WorkerConfiguration ()
	{
		this ((Callbacks) null, (Object) null);
	}
	
	public WorkerConfiguration (final Callbacks callbacks, final Object monitor)
	{
		super ();
		this.callbacks = callbacks;
		this.monitor = monitor;
	}
	
	public WorkerConfiguration (final WorkerConfiguration override, final WorkerConfiguration overriden)
	{
		super ();
		this.callbacks = Objects.firstNonNull (override.callbacks, overriden.callbacks);
		this.monitor = Objects.firstNonNull (override.callbacks, overriden.callbacks);
	}
	
	public final Callbacks callbacks;
	public final Object monitor;
	public static final int defaultWaitTimeout = 1000;
}
