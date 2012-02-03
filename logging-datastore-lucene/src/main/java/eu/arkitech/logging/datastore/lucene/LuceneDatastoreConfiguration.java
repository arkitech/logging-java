/*
 * #%L
 * arkitech-logging-datastore-lucene
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

package eu.arkitech.logging.datastore.lucene;


import java.io.File;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.Configuration;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.bdb.BdbDatastoreConfiguration;


public class LuceneDatastoreConfiguration
		implements
			Configuration
{
	public LuceneDatastoreConfiguration ()
	{
		this (null);
	}
	
	public LuceneDatastoreConfiguration (final File environmentPath)
	{
		this (environmentPath, null);
	}
	
	public LuceneDatastoreConfiguration (final File environmentPath, final Boolean readOnly)
	{
		this (environmentPath, readOnly, null);
	}
	
	public LuceneDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Boolean syncEnabled, final Long syncTimeout, final Serializer serializer, final LoggingEventMutator loadMutator, final LoggingEventMutator storeMutator, final Callbacks callbacks, final Object monitor)
	{
		super ();
		this.environmentPath = environmentPath;
		this.readOnly = readOnly;
		this.syncEnabled = syncEnabled;
		this.syncTimeout = syncTimeout;
		this.serializer = serializer;
		this.loadMutator = loadMutator;
		this.storeMutator = storeMutator;
		this.callbacks = callbacks;
		this.monitor = monitor;
	}
	
	public LuceneDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Integer compressed)
	{
		this (environmentPath, readOnly, compressed, null);
	}
	
	public LuceneDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Integer compressed, final Long syncTimeout)
	{
		this (environmentPath, readOnly, compressed, syncTimeout, null);
	}
	
	public LuceneDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Integer compressed, final Long syncTimeout, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, true, syncTimeout, ((compressed != null) ? ((compressed < 0) ? new DefaultBinarySerializer () : new CompressedBinarySerializer (compressed)) : null), null, null, callbacks, null);
	}
	
	public LuceneDatastoreConfiguration (final LuceneDatastoreConfiguration override, final LuceneDatastoreConfiguration overriden)
	{
		super ();
		Preconditions.checkNotNull (override);
		Preconditions.checkNotNull (overriden);
		this.environmentPath = Objects.firstNonNull (override.environmentPath, overriden.environmentPath);
		this.readOnly = Objects.firstNonNull (override.readOnly, overriden.readOnly);
		this.syncEnabled = Objects.firstNonNull (override.syncEnabled, overriden.syncEnabled);
		this.syncTimeout = Objects.firstNonNull (override.syncTimeout, overriden.syncTimeout);
		this.serializer = Objects.firstNonNull (override.serializer, overriden.serializer);
		this.loadMutator = Objects.firstNonNull (override.loadMutator, overriden.loadMutator);
		this.storeMutator = Objects.firstNonNull (override.storeMutator, overriden.storeMutator);
		this.callbacks = Objects.firstNonNull (override.callbacks, overriden.callbacks);
		this.monitor = Objects.firstNonNull (override.monitor, overriden.monitor);
	}
	
	public final Callbacks callbacks;
	public final File environmentPath;
	public final LoggingEventMutator loadMutator;
	public final Object monitor;
	public final Boolean readOnly;
	public final Serializer serializer;
	public final LoggingEventMutator storeMutator;
	public final Boolean syncEnabled;
	public final Long syncTimeout;
	public static final File defaultEnvironmentPath = new File ("/tmp/logging-lucene-datastore");
	public static final LoggingEventMutator defaultLoadMutator = BdbDatastoreConfiguration.defaultLoadMutator;
	public static final Serializer defaultSerializer = BdbDatastoreConfiguration.defaultSerializer;
	public static final LoggingEventMutator defaultStoreMutator = BdbDatastoreConfiguration.defaultStoreMutator;
}
