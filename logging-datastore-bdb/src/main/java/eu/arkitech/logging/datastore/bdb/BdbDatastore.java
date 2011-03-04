
package eu.arkitech.logging.datastore.bdb;


import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.ExceptionEvent;
import com.sleepycat.je.ExceptionListener;
import com.sleepycat.je.OperationStatus;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.CompressedBinarySerializer;
import eu.arkitech.logback.common.DefaultBinarySerializer;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
import eu.arkitech.logback.common.LoggingEventFilter;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.SLoggingEvent1;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.common.Datastore;


public final class BdbDatastore
		implements
			Datastore
{
	public BdbDatastore (final File environmentPath)
	{
		this (environmentPath, -1);
	}
	
	public BdbDatastore (final File environmentPath, final Callbacks callbacks)
	{
		this (environmentPath, -1, callbacks);
	}
	
	public BdbDatastore (final File environmentPath, final int compressed)
	{
		this (environmentPath, compressed, null);
	}
	
	public BdbDatastore (final File environmentPath, final int compressed, final Callbacks callbacks)
	{
		this (environmentPath, compressed == -1 ? new DefaultBinarySerializer ()
				: new CompressedBinarySerializer (compressed), null, callbacks);
	}
	
	public BdbDatastore (
			final File environmentPath, final Serializer serializer, final LoggingEventMutator mutator,
			final Callbacks callbacks)
	{
		this (environmentPath, serializer, mutator, callbacks, new Object ());
	}
	
	public BdbDatastore (
			final File environmentPath, final Serializer serializer, final LoggingEventMutator mutator,
			final Callbacks callbacks, final Object monitor)
	{
		super ();
		synchronized (monitor) {
			this.monitor = monitor;
			this.state = State.Closed;
			this.environmentPath = environmentPath;
			this.serializer = (serializer != null) ? serializer : new DefaultBinarySerializer ();
			this.mutator = mutator;
			this.callbacks = (callbacks != null) ? callbacks : new DefaultLoggerCallbacks (this);
			this.environmentConfiguration = new EnvironmentConfig ();
			this.environmentConfiguration.setAllowCreate (true);
			this.environmentConfiguration.setReadOnly (false);
			this.environmentConfiguration.setTransactional (false);
			this.environmentConfiguration.setLocking (false);
			this.environmentConfiguration.setExceptionListener (new ExceptionHandler ());
			this.eventDatabaseConfiguration = new DatabaseConfig ();
			this.eventDatabaseConfiguration.setAllowCreate (true);
			this.eventDatabaseConfiguration.setReadOnly (false);
			this.eventDatabaseConfiguration.setSortedDuplicates (false);
			this.eventDatabaseConfiguration.setTransactional (false);
		}
	}
	
	public final boolean close ()
	{
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			if (this.state != State.Opened)
				throw (new IllegalStateException ("bdb datastore is not opened"));
			if (this.eventDatabase != null)
				try {
					this.eventDatabase.close ();
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (
							exception, "bdb datastore encountered an error while closing the event database; ignoring!");
				} finally {
					this.eventDatabase = null;
				}
			if (this.environment != null)
				try {
					this.environment.close ();
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (
							exception, "bdb datastore encountered an error while closing the environment; ignoring!");
				} finally {
					this.environment = null;
				}
			this.state = State.Closed;
			return (true);
		}
	}
	
	public final boolean open ()
	{
		synchronized (this.monitor) {
			if (this.state != State.Closed)
				throw (new IllegalStateException ("bdb datastore is already opened"));
			try {
				if (!this.environmentPath.exists ())
					this.environmentPath.mkdir ();
				if (!this.environmentPath.exists ())
					throw (new IllegalStateException ());
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "bdb datastore encountered an error while opening the environment; aborting!");
			}
			try {
				this.environment = new Environment (this.environmentPath, this.environmentConfiguration);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "bdb datastore encountered an error while opening the environment; aborting!");
				this.close ();
				return (false);
			}
			try {
				this.eventDatabase =
						this.environment.openDatabase (
								null, BdbDatastore.defaultEventDatabaseName, this.eventDatabaseConfiguration);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "bdb datastore encountered an error while opening the environment; aborting!", exception);
				this.close ();
				return (false);
			}
			this.state = State.Opened;
			return (true);
		}
	}
	
	public final Database openDatabase (final String name, final DatabaseConfig configuration)
			throws DatabaseException
	{
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("bdb datastore is not opened"));
			return (this.environment.openDatabase (null, name, configuration));
		}
	}
	
	public final Iterable<ILoggingEvent> select (
			final ILoggingEvent reference, final int beforeCount, final int afterCount, final LoggingEventFilter filter)
	{
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("bdb datastore is not opened"));
			throw (new UnsupportedOperationException ());
		}
	}
	
	public final Iterable<ILoggingEvent> select (
			final long afterTimestamp, final long intervalMs, final LoggingEventFilter filter)
	{
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("bdb datastore is not opened"));
			throw (new UnsupportedOperationException ());
		}
	}
	
	public final ILoggingEvent select (final String key)
	{
		final DatabaseEntry keyEntry = this.encodeKeyEntry (key);
		if (keyEntry == null)
			return (null);
		final DatabaseEntry eventEntry = new DatabaseEntry ();
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("bdb datastore is not opened"));
			try {
				final OperationStatus outcome = this.eventDatabase.get (null, keyEntry, eventEntry, null);
				if (outcome != OperationStatus.SUCCESS) {
					this.callbacks.handleException (
							new DatabaseException (),
							"bdb datastore encountered an error while getting the event `%s`; aborting!", key);
					return (null);
				}
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "bdb datastore encountered an error while getting the event `%s`; aborting!", key);
				return (null);
			}
		}
		final ILoggingEvent event = this.decodeEventEntry (eventEntry);
		if (event instanceof SLoggingEvent1)
			((SLoggingEvent1) event).key = key;
		return (event);
	}
	
	public final String store (final ILoggingEvent originalEvent)
	{
		final ILoggingEvent clonedEvent = this.prepareEvent (originalEvent);
		if (clonedEvent == null)
			return (null);
		final DatabaseEntry eventEntry = this.encodeEventEntry (clonedEvent);
		if (eventEntry == null)
			return (null);
		final String key = this.encodeKey (clonedEvent.getTimeStamp (), eventEntry);
		final DatabaseEntry keyEntry = this.encodeKeyEntry (key);
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("bdb datastore is not opened"));
			try {
				final OperationStatus outcome = this.eventDatabase.put (null, keyEntry, eventEntry);
				if (outcome != OperationStatus.SUCCESS) {
					this.callbacks.handleException (
							new DatabaseException (),
							"bdb datastore encountered an error while storing the event `%s`; aborting!", key);
					return (null);
				}
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						new DatabaseException (),
						"bdb datastore encountered an error while storing the event `%s`; aborting!", key);
				return (null);
			}
		}
		if (originalEvent instanceof SLoggingEvent1)
			((SLoggingEvent1) originalEvent).key = key;
		return (key);
	}
	
	private final ILoggingEvent decodeEventEntry (final DatabaseEntry entry)
	{
		final Object object;
		try {
			object = this.serializer.deserialize (entry.getData (), entry.getOffset (), entry.getSize ());
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered an error while deserializing the event; aborting!");
			return (null);
		}
		final ILoggingEvent event;
		try {
			event = ILoggingEvent.class.cast (object);
		} catch (final ClassCastException exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered ane error while deserializing the event; aborting!");
			return (null);
		}
		return (event);
	}
	
	private final DatabaseEntry encodeEventEntry (final ILoggingEvent event)
	{
		final byte[] data;
		try {
			data = this.serializer.serialize (event);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered an error while serealizing the event; aborting!");
			return (null);
		}
		return (new DatabaseEntry (data));
	}
	
	private final String encodeKey (final long timestamp, final DatabaseEntry eventEntry)
	{
		final byte[] data =
				this.encodeRawKeyFromData (timestamp, eventEntry.getData (), eventEntry.getOffset (), eventEntry.getSize ());
		if (data == null)
			return (null);
		return (this.encodeKeyFromHash (data));
	}
	
	private final DatabaseEntry encodeKeyEntry (final String key)
	{
		return (new DatabaseEntry (key.getBytes ()));
	}
	
	private final String encodeKeyFromHash (final byte[] hashBytes)
	{
		final StringBuilder builder = new StringBuilder ();
		for (final byte b : hashBytes) {
			final String s = Integer.toHexString (b & 0xff);
			if (s.length () == 1)
				builder.append ('0').append (s);
			else
				builder.append (s);
		}
		return (builder.toString ());
	}
	
	private final byte[] encodeRawKeyFromData (final long timestamp, final byte[] data, final int offset, final int size)
	{
		final MessageDigest hasher;
		try {
			hasher = MessageDigest.getInstance (BdbDatastore.defaultHashAlgorithm);
		} catch (final NoSuchAlgorithmException exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered an error while creating the hasher; aborting!");
			return (null);
		}
		hasher.update (data, offset, size);
		final byte[] hashBytes = hasher.digest ();
		return (this.encodeRawKeyFromHash (timestamp, hashBytes));
	}
	
	private final byte[] encodeRawKeyFromHash (final long timestamp, final byte[] hashBytes)
	{
		final byte[] timestampBytes =
				new byte[] {(byte) ((timestamp >> 56) & 0xff), (byte) ((timestamp >> 48) & 0xff),
						(byte) ((timestamp >> 40) & 0xff), (byte) ((timestamp >> 32) & 0xff),
						(byte) ((timestamp >> 24) & 0xff), (byte) ((timestamp >> 16) & 0xff),
						(byte) ((timestamp >> 8) & 0xff), (byte) ((timestamp >> 0) & 0xff)};
		final byte[] keyBytes = new byte[timestampBytes.length + hashBytes.length];
		System.arraycopy (timestampBytes, 0, keyBytes, 0, timestampBytes.length);
		System.arraycopy (hashBytes, 0, keyBytes, timestampBytes.length, hashBytes.length);
		return (keyBytes);
	}
	
	private final ILoggingEvent prepareEvent (final ILoggingEvent originalEvent)
	{
		final SLoggingEvent1 clonedEvent;
		if (!(originalEvent instanceof SLoggingEvent1))
			try {
				clonedEvent = SLoggingEvent1.build (originalEvent);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "amqp publisher sink encountered an error while cloning the event; aborting!");
				return (null);
			}
		else
			clonedEvent = (SLoggingEvent1) originalEvent;
		try {
			if (this.mutator != null)
				this.mutator.mutate (clonedEvent);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp publisher sink encountered an error while mutating the event; aborting!");
			return (null);
		}
		return (clonedEvent);
	}
	
	private final Callbacks callbacks;
	private Environment environment;
	private final EnvironmentConfig environmentConfiguration;
	private final File environmentPath;
	private Database eventDatabase;
	private final DatabaseConfig eventDatabaseConfiguration;
	private final Object monitor;
	private final LoggingEventMutator mutator;
	private final Serializer serializer;
	private State state;
	
	public static final String defaultEventDatabaseName = "events";
	public static final String defaultHashAlgorithm = "MD5";
	
	public static enum State
	{
		Closed,
		Opened;
	}
	
	private final class ExceptionHandler
			implements
				ExceptionListener
	{
		public void exceptionThrown (final ExceptionEvent event)
		{
			BdbDatastore.this.callbacks.handleException (
					event.getException (), "bdb encountered an unexpected error (in thread `%s`)", event.getThreadName ());
		}
	}
}
