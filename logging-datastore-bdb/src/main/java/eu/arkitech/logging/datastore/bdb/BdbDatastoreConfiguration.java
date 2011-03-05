
package eu.arkitech.logging.datastore.bdb;


import java.io.File;

import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.Serializer;


public final class BdbDatastoreConfiguration
{
	public BdbDatastoreConfiguration ()
	{
		this (null);
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
	
	public BdbDatastoreConfiguration (
			final File environmentPath, final Boolean readOnly, final Integer compressed, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, ((compressed != null) ? ((compressed < 0) ? new DefaultBinarySerializer ()
				: new CompressedBinarySerializer (compressed)) : null), null, null, callbacks);
	}
	
	public BdbDatastoreConfiguration (
			final File environmentPath, final Boolean readOnly, final Serializer serializer,
			final LoggingEventMutator loadMutator, final LoggingEventMutator storeMutator, final Callbacks callbacks)
	{
		this (environmentPath, readOnly, serializer, loadMutator, storeMutator, callbacks, null);
	}
	
	public BdbDatastoreConfiguration (
			final File environmentPath, final Boolean readOnly, final Serializer serializer,
			final LoggingEventMutator loadMutator, final LoggingEventMutator storeMutator, final Callbacks callbacks,
			final Object monitor)
	{
		super ();
		this.environmentPath = (environmentPath != null) ? environmentPath : BdbDatastoreConfiguration.defaultEnvironmentPath;
		this.readOnly = (readOnly != null) ? readOnly.booleanValue () : true;
		this.serializer = (serializer != null) ? serializer : new DefaultBinarySerializer ();
		this.loadMutator = loadMutator;
		this.storeMutator = storeMutator;
		this.callbacks = (callbacks != null) ? callbacks : new DefaultLoggerCallbacks (this);
		this.monitor = (monitor != null) ? monitor : new Object ();
	}
	
	public final Callbacks callbacks;
	public final File environmentPath;
	public final LoggingEventMutator loadMutator;
	public final Object monitor;
	public final boolean readOnly;
	public final Serializer serializer;
	public final LoggingEventMutator storeMutator;
	
	public static final File defaultEnvironmentPath = new File ("/tmp/logging-bdb-datastore");
}
