/*
 * #%L
 * arkitech-logging-datastore-common
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

package eu.arkitech.logging.datastore.common;


import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.Worker;


public final class SyncableDatastoreBackgroundWorker
		extends Worker
{
	public SyncableDatastoreBackgroundWorker (final Datastore datastore)
	{
		this (new SyncableDatastoreBackgroundWorkerConfiguration (datastore));
	}
	
	public SyncableDatastoreBackgroundWorker (final SyncableDatastoreBackgroundWorkerConfiguration configuration)
	{
		super (configuration);
		this.datastore = Preconditions.checkNotNull (configuration.datastore);
		this.syncReadTimeout = Preconditions.checkNotNull ((configuration.syncReadTimeout != null) ? configuration.syncReadTimeout.longValue () : SyncableDatastoreBackgroundWorkerConfiguration.defaultSyncReadTimeout);
		Preconditions.checkArgument ((this.syncReadTimeout == -1) || (this.syncReadTimeout > 0));
		this.syncWriteTimeout = Preconditions.checkNotNull ((configuration.syncWriteTimeout != null) ? configuration.syncWriteTimeout.longValue () : SyncableDatastoreBackgroundWorkerConfiguration.defaultSyncWriteTimeout);
		Preconditions.checkArgument ((this.syncWriteTimeout == -1) || (this.syncWriteTimeout > 0));
		if (this.syncReadTimeout != -1)
			this.syncReadDatastore = SyncableImmutableDatastore.class.cast (this.datastore);
		else
			this.syncReadDatastore = null;
		if (this.syncWriteTimeout != -1)
			this.syncWriteDatastore = SyncableMutableDatastore.class.cast (this.datastore);
		else
			this.syncWriteDatastore = null;
		this.cancel = false;
	}
	
	public final boolean cancel ()
	{
		this.cancel = true;
		if (this.isCurrentThread ())
			return (true);
		else {
			this.requestStop ();
			return (this.awaitStop ());
		}
	}
	
	@Override
	protected final void executeLoop ()
	{
		long lastSyncWriteTimestamp = System.currentTimeMillis ();
		long lastSyncReadTimestamp = lastSyncWriteTimestamp;
		while (true) {
			if (this.shouldStopHard ())
				break;
			if (this.shouldStopSoft ()) {
				if (this.cancel)
					break;
				else
					this.callbacks.handleLogEvent (Level.WARN, null, "database background thread delayed...");
			}
			synchronized (this.monitor) {
				final long currentTimestamp = System.currentTimeMillis ();
				if (this.syncWriteDatastore != null) {
					if ((currentTimestamp - lastSyncWriteTimestamp) > this.syncWriteTimeout) {
						this.callbacks.handleLogEvent (Level.DEBUG, null, "datastore background thread (write) sync-ing the datastore");
						try {
							if (!this.syncWriteDatastore.syncWrite ())
								this.callbacks.handleLogEvent (Level.ERROR, null, "datastore background thread failed to (write) sync the datastore; ignoring!");
						} catch (final Error exception) {
							this.callbacks.handleException (exception, "datastore background thread encountered an unknown error while (write) sync-ing the datastore; ignoring!");
						}
						lastSyncWriteTimestamp = currentTimestamp;
					}
				}
				if (this.syncReadDatastore != null) {
					if ((currentTimestamp - lastSyncReadTimestamp) > this.syncReadTimeout) {
						this.callbacks.handleLogEvent (Level.DEBUG, null, "datastore background thread (read) sync-ing the datastore");
						try {
							if (!this.syncReadDatastore.syncRead ())
								this.callbacks.handleLogEvent (Level.ERROR, null, "datastore background thread failed to (read) sync the datastore; ignoring!");
						} catch (final Error exception) {
							this.callbacks.handleException (exception, "datastore background thread encountered an unknown error while (read) sync-ing the datastore; ignoring!");
						}
						lastSyncReadTimestamp = currentTimestamp;
					}
				}
			}
			try {
				Thread.sleep (this.waitTimeout);
			} catch (final InterruptedException exception) {}
		}
	}
	
	@Override
	protected final void finalizeLoop ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "datastore bagkground thread stopping");
			if (!this.cancel) {
				this.callbacks.handleLogEvent (Level.WARN, null, "datastore bagkground thread detected that the datastore is not closed; closing!");
				if (!this.datastore.close ())
					this.callbacks.handleLogEvent (Level.ERROR, null, "datastore background thread failed to close the datastore; ignoring!");
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "datastore bagkground thread stopped");
		}
	}
	
	@Override
	protected final void initializeLoop ()
	{
		synchronized (this.monitor) {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "datastore background thread starting");
			this.callbacks.handleLogEvent (Level.INFO, null, "datastore background thread started");
		}
	}
	
	private boolean cancel;
	private final Datastore datastore;
	private final SyncableImmutableDatastore syncReadDatastore;
	private final long syncReadTimeout;
	private final SyncableMutableDatastore syncWriteDatastore;
	private final long syncWriteTimeout;
}
