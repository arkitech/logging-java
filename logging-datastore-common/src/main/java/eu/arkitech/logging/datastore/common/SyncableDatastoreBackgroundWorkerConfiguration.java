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
