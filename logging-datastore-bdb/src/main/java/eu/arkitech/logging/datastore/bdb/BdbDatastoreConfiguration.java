
package eu.arkitech.logging.datastore.bdb;


import java.io.File;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.Configuration;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public class BdbDatastoreConfiguration
		implements
			Configuration
{
	public BdbDatastoreConfiguration ()
	{
		this (null);
	}
	
	public BdbDatastoreConfiguration (final BdbDatastoreConfiguration override, final BdbDatastoreConfiguration overriden)
	{
		super ();
		Preconditions.checkNotNull (override);
		Preconditions.checkNotNull (overriden);
		this.environmentPath = Objects.firstNonNull (override.environmentPath, overriden.environmentPath);
		this.readOnly = Objects.firstNonNull (override.readOnly, overriden.readOnly);
		this.serializer = Objects.firstNonNull (override.serializer, overriden.serializer);
		this.loadMutator = Objects.firstNonNull (override.loadMutator, overriden.loadMutator);
		this.storeMutator = Objects.firstNonNull (override.storeMutator, overriden.storeMutator);
		this.callbacks = Objects.firstNonNull (override.callbacks, overriden.callbacks);
		this.monitor = Objects.firstNonNull (override.monitor, overriden.monitor);
	}
	
	public BdbDatastoreConfiguration (final File environmentPath)
	{
		this (environmentPath, null);
	}
	
	public BdbDatastoreConfiguration (final File environmentPath, final Boolean readOnly)
	{
		this (environmentPath, readOnly, null);
	}
	
	public BdbDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Integer compressed)
	{
		this (environmentPath, readOnly, compressed, null);
	}
	
	public BdbDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Integer compressed, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, ((compressed != null) ? ((compressed < 0) ? new DefaultBinarySerializer () : new CompressedBinarySerializer (compressed)) : null), null, null, callbacks, null);
	}
	
	public BdbDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Serializer serializer, final LoggingEventMutator loadMutator, final LoggingEventMutator storeMutator, final Callbacks callbacks, final Object monitor)
	{
		super ();
		this.environmentPath = environmentPath;
		this.readOnly = readOnly;
		this.serializer = serializer;
		this.loadMutator = loadMutator;
		this.storeMutator = storeMutator;
		this.callbacks = callbacks;
		this.monitor = monitor;
	}
	
	public final Callbacks callbacks;
	public final File environmentPath;
	public final LoggingEventMutator loadMutator;
	public final Object monitor;
	public final Boolean readOnly;
	public final Serializer serializer;
	public final LoggingEventMutator storeMutator;
	
	public static final File defaultEnvironmentPath = new File ("/tmp/logging-bdb-datastore");
	public static final LoggingEventMutator defaultLoadMutator = null;
	public static final Serializer defaultSerializer = new DefaultBinarySerializer ();
	public static final LoggingEventMutator defaultStoreMutator = null;
}
