
package eu.arkitech.logging.datastore.lucene;


import java.io.File;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.bdb.BdbDatastoreConfiguration;


public final class LuceneDatastoreConfiguration
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
	
	public LuceneDatastoreConfiguration (final File environmentPath, final Boolean readOnly, final Integer compressed)
	{
		this (environmentPath, readOnly, compressed, null);
	}
	
	public LuceneDatastoreConfiguration (
			final File environmentPath, final Boolean readOnly, final Integer compressed, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, ((compressed != null) ? ((compressed < 0) ? new DefaultBinarySerializer ()
				: new CompressedBinarySerializer (compressed)) : null), null, null, callbacks);
	}
	
	public LuceneDatastoreConfiguration (
			final File environmentPath, final Boolean readOnly, final Serializer serializer,
			final LoggingEventMutator loadMutator, final LoggingEventMutator storeMutator, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, serializer, loadMutator, storeMutator, callbacks, null);
	}
	
	public LuceneDatastoreConfiguration (
			final File environmentPath, final Boolean readOnly, final Serializer serializer,
			final LoggingEventMutator loadMutator, final LoggingEventMutator storeMutator, final Callbacks callbacks,
			final Object monitor)
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
	
	public LuceneDatastoreConfiguration (
			final LuceneDatastoreConfiguration override, final LuceneDatastoreConfiguration overriden)
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
	
	public final Callbacks callbacks;
	public final File environmentPath;
	public final LoggingEventMutator loadMutator;
	public final Object monitor;
	public final Boolean readOnly;
	public final Serializer serializer;
	public final LoggingEventMutator storeMutator;
	
	public static final File defaultEnvironmentPath = new File ("/tmp/logging-lucene-datastore");
	public static final LoggingEventMutator defaultLoadMutator = BdbDatastoreConfiguration.defaultLoadMutator;
	public static final Serializer defaultSerializer = BdbDatastoreConfiguration.defaultSerializer;
	public static final LoggingEventMutator defaultStoreMutator = BdbDatastoreConfiguration.defaultStoreMutator;
}
