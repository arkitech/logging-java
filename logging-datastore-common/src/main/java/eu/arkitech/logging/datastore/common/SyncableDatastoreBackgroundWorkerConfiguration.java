
package eu.arkitech.logging.datastore.common;


import com.google.common.base.Objects;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.WorkerConfiguration;


public class SyncableDatastoreBackgroundWorkerConfiguration
		extends WorkerConfiguration
{
	public SyncableDatastoreBackgroundWorkerConfiguration (final Datastore datastore)
	{
		this (datastore, null, null);
	}
	
	public SyncableDatastoreBackgroundWorkerConfiguration (final Datastore datastore, final Callbacks callbacks, final Object monitor)
	{
		this (datastore, null, null, callbacks, monitor);
	}
	
	public SyncableDatastoreBackgroundWorkerConfiguration (final Datastore datastore, final Long syncReadTimeout, final Long syncWriteTimeout, final Callbacks callbacks, final Object monitor)
	{
		super (callbacks, monitor);
		this.datastore = datastore;
		this.syncReadTimeout = syncReadTimeout;
		this.syncWriteTimeout = syncWriteTimeout;
	}
	
	public SyncableDatastoreBackgroundWorkerConfiguration (final SyncableDatastoreBackgroundWorkerConfiguration override, final SyncableDatastoreBackgroundWorkerConfiguration overriden)
	{
		super (override, overriden);
		this.datastore = Objects.firstNonNull (override.datastore, overriden.datastore);
		this.syncReadTimeout = Objects.firstNonNull (override.syncReadTimeout, overriden.syncReadTimeout);
		this.syncWriteTimeout = Objects.firstNonNull (override.syncWriteTimeout, overriden.syncWriteTimeout);
	}
	
	public final Datastore datastore;
	public final Long syncReadTimeout;
	public final Long syncWriteTimeout;
	public static final long defaultSyncReadTimeout = 10 * 1000;
	public static final long defaultSyncWriteTimeout = 10 * 1000;
}
