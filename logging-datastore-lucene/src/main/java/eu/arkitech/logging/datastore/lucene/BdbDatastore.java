
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.ExceptionEvent;
import com.sleepycat.je.ExceptionListener;
import com.sleepycat.je.OperationStatus;
import eu.arkitech.logback.common.Serializer;
import eu.arkitech.logging.datastore.common.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class BdbDatastore
		implements
			Datastore
{
	public BdbDatastore (final File environmentPath, final boolean luceneEnabled, final Serializer serializer)
	{
		super ();
		this.environmentPath = environmentPath;
		this.luceneEnabled = luceneEnabled;
		this.serializer = serializer;
		this.logger = LoggerFactory.getLogger (BdbDatastore.class);
		this.callbacks = new Callbacks (this.logger);
		this.environmentConfiguration = new EnvironmentConfig ();
		this.environmentConfiguration.setAllowCreate (true);
		this.environmentConfiguration.setReadOnly (false);
		this.environmentConfiguration.setTransactional (false);
		this.environmentConfiguration.setLocking (false);
		this.environmentConfiguration.setExceptionListener (this.callbacks);
		this.eventDatabaseConfig = new DatabaseConfig ();
		this.eventDatabaseConfig.setAllowCreate (true);
		this.eventDatabaseConfig.setReadOnly (false);
		this.eventDatabaseConfig.setSortedDuplicates (false);
		this.eventDatabaseConfig.setTransactional (false);
		if (this.luceneEnabled) {
			this.luceneFileDatabaseConfig = this.eventDatabaseConfig;
			this.luceneBlockDatabaseConfig = this.eventDatabaseConfig;
		} else {
			this.luceneFileDatabaseConfig = null;
			this.luceneBlockDatabaseConfig = null;
		}
	}
	
	public BdbDatastore (final File environmentPath, final Serializer serializer)
	{
		this (environmentPath, false, serializer);
	}
	
	public final boolean close ()
	{
		try {
			this.eventDatabase.close ();
		} catch (final DatabaseException exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered an error while closing the databases; ignoring!");
		} finally {
			this.eventDatabase = null;
		}
		if (this.luceneFileDatabase != null)
			try {
				this.luceneFileDatabase.close ();
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "bdb datastore encountered an error while closing the databases; ignoring!");
			} finally {
				this.luceneFileDatabase = null;
			}
		if (this.luceneBlockDatabase != null)
			try {
				this.luceneBlockDatabase.close ();
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "bdb datastore encountered an error while closing the databases; ignoring!");
			} finally {
				this.luceneBlockDatabase = null;
			}
		try {
			this.environment.close ();
		} catch (final DatabaseException exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered an error while closing the databases; ignoring!");
		} finally {
			this.environment = null;
		}
		return (true);
	}
	
	public final boolean open ()
	{
		try {
			this.environment = new Environment (this.environmentPath, this.environmentConfiguration);
			this.eventDatabase =
					this.environment.openDatabase (null, BdbDatastore.defaultEventDatabaseName, this.eventDatabaseConfig);
			if (this.luceneEnabled) {
				this.luceneFileDatabase =
						this.environment.openDatabase (
								null, BdbDatastore.defaultLuceneFileDatabaseName, this.luceneFileDatabaseConfig);
				this.luceneBlockDatabase =
						this.environment.openDatabase (
								null, BdbDatastore.defaultLuceneBlockDatabaseName, this.luceneBlockDatabaseConfig);
			}
		} catch (final DatabaseException exception) {
			this.callbacks.handleException (
					exception, "bdb datastore encountered an error while opening the databases; aborting!", exception);
			this.close ();
			return (false);
		}
		return (true);
	}
	
	public final List<ILoggingEvent> select (
			final ILoggingEvent reference, final int beforeCount, final int afterCount, final Filter<ILoggingEvent> filter)
	{
		throw (new UnsupportedOperationException ());
	}
	
	public final List<ILoggingEvent> select (
			final long afterTimestamp, final long intervalMs, final Filter<ILoggingEvent> filter)
	{
		throw (new UnsupportedOperationException ());
	}
	
	public final ILoggingEvent select (final String key)
	{
		final DatabaseEntry keyEntry = new DatabaseEntry (key.getBytes ());
		final DatabaseEntry eventEntry = new DatabaseEntry ();
		try {
			final OperationStatus outcome = this.eventDatabase.get (null, keyEntry, eventEntry, null);
			if (outcome != OperationStatus.SUCCESS) {
				this.callbacks.handleException (new DatabaseException (), "bdb datastore encountered an error while getting the event `%s`; aborting!", key);
				return (null);
			}
		} catch (final DatabaseException exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered an error while getting the event `%s`; aborting!", key);
			return (null);
		}
		final Object object;
		try {
			object = this.serializer.deserialize (eventEntry.getData (), eventEntry.getOffset (), eventEntry.getSize ());
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered an error while deserializing the event `%s`; aborting!", key);
			return (null);
		}
		final ILoggingEvent event;
		try {
			event = ILoggingEvent.class.cast (object);
		} catch (final ClassCastException exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered ane error while deserializing the event `%s`; aborting!", key);
			return (null);
		}
		return (event);
	}
	
	public final String store (final ILoggingEvent event)
	{
		final byte[] eventData;
		try {
			eventData = this.serializer.serialize (event);
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered an error while serealizing the event; aborting!");
			return (null);
		}
		final byte[] keyBytes = this.buildKey (eventData, event.getTimeStamp ());
		final String key = this.formatKey (keyBytes);
		final DatabaseEntry keyEntry = new DatabaseEntry (key.getBytes ());
		final DatabaseEntry eventEntry = new DatabaseEntry (eventData);
		try {
			final OperationStatus outcome = this.eventDatabase.put (null, keyEntry, eventEntry);
			if (outcome != OperationStatus.SUCCESS) {
				this.callbacks.handleException (new DatabaseException (), "bdb datastore encountered an error while storing the event `%s`; aborting!", key);
				return (null);
			}
		} catch (final DatabaseException exception) {
			this.callbacks.handleException (new DatabaseException (), "bdb datastore encountered an error while storing the event `%s`; aborting!", key);
			return (null);
		}
		return (key);
	}
	
	private final byte[] buildKey (final byte[] data, final long timestamp)
	{
		return (this.buildKey (data, 0, data.length, timestamp));
	}
	
	private final byte[] buildKey (final byte[] data, final int offset, final int size, final long timestamp)
	{
		final byte[] timestampBytes = new byte[] {
				(byte)((timestamp >> 56) & 0xff),
				(byte)((timestamp >> 48) & 0xff),
				(byte)((timestamp >> 40) & 0xff),
				(byte)((timestamp >> 32) & 0xff),
				(byte)((timestamp >> 24) & 0xff),
				(byte)((timestamp >> 16) & 0xff),
				(byte)((timestamp >> 8) & 0xff),
				(byte)((timestamp >> 0) & 0xff),
		};
		final MessageDigest hasher;
		try {
			hasher = MessageDigest.getInstance (defaultHashAlgorithm);
		} catch (final NoSuchAlgorithmException exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered an error while creating the key for the event; aborting!");
			return (null);
		}
		hasher.update (data, offset, size);
		final byte[] hashBytes = hasher.digest ();
		final byte[] keyBytes = new byte[timestampBytes.length + hashBytes.length];
		System.arraycopy (timestampBytes, 0, keyBytes, 0, timestampBytes.length);
		System.arraycopy (hashBytes, 0, keyBytes, timestampBytes.length, hashBytes.length);
		return (keyBytes);
	}
	
	private final String formatKey (final byte[] bytes) {
		final StringBuilder builder = new StringBuilder ();
		for (final byte b : bytes) {
			final String s = Integer.toHexString (b & 0xff);
			if (s.length () == 1)
				builder.append ('0') .append (s);
			else
				builder.append (s);
		}
		return (builder.toString ());
	}
	
	final Database getLuceneBlockDatabase ()
	{
		return (this.luceneBlockDatabase);
	}
	
	final Database getLuceneFileDatabase ()
	{
		return (this.luceneFileDatabase);
	}
	
	private final Callbacks callbacks;
	private Environment environment;
	private final EnvironmentConfig environmentConfiguration;
	private final File environmentPath;
	private Database eventDatabase;
	private final DatabaseConfig eventDatabaseConfig;
	private final Logger logger;
	private Database luceneBlockDatabase;
	private final DatabaseConfig luceneBlockDatabaseConfig;
	private final boolean luceneEnabled;
	private Database luceneFileDatabase;
	private final DatabaseConfig luceneFileDatabaseConfig;
	private final Serializer serializer;
	
	public static final String defaultEventDatabaseName = "events";
	public static final String defaultLuceneBlockDatabaseName = "lucene-blocks";
	public static final String defaultLuceneFileDatabaseName = "lucene-files";
	public static final String defaultHashAlgorithm = "MD5";
	
	private final class Callbacks
			implements
				ExceptionListener
	{
		Callbacks (final Logger logger)
		{
			super ();
			this.logger = logger;
		}
		
		public final void exceptionThrown (final ExceptionEvent event)
		{
			this.logger.error ("bdb datastore encountered an error; ignoring!", event.getException ());
		}
		
		public final void handleException (
				final Throwable exception, final String messageFormat, final Object ... messageArguments)
		{
			this.logger.error (String.format (messageFormat, messageArguments), exception);
		}
		
		private final Logger logger;
	}
}
