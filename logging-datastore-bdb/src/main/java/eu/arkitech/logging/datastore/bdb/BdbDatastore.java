
package eu.arkitech.logging.datastore.bdb;


import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.google.common.base.Preconditions;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
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
	public BdbDatastore ()
	{
		this (null);
	}
	
	public BdbDatastore (final BdbDatastoreConfiguration configuration_)
	{
		super ();
		final BdbDatastoreConfiguration configuration = (configuration_ != null) ? configuration_ : new BdbDatastoreConfiguration ();
		this.monitor = (configuration.monitor != null) ? configuration.monitor : new Object ();
		this.callbacks = (configuration.callbacks != null) ? configuration.callbacks : new DefaultLoggerCallbacks (this);
		this.environmentPath = Preconditions.checkNotNull ((configuration.environmentPath != null) ? configuration.environmentPath : BdbDatastoreConfiguration.defaultEnvironmentPath);
		this.readOnly = (configuration.readOnly != null) ? configuration.readOnly.booleanValue () : true;
		this.serializer = Preconditions.checkNotNull ((configuration.serializer != null) ? configuration.serializer : BdbDatastoreConfiguration.defaultSerializer);
		this.loadMutator = (configuration.loadMutator != null) ? configuration.loadMutator : BdbDatastoreConfiguration.defaultLoadMutator;
		this.storeMutator = (configuration.storeMutator != null) ? configuration.storeMutator : BdbDatastoreConfiguration.defaultStoreMutator;
		this.state = State.Closed;
	}
	
	@Override
	public final boolean close ()
	{
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			try {
				this.callbacks.handleLogEvent (Level.DEBUG, null, "bdb datastore closing");
				if (this.eventDatabase != null)
					try {
						this.eventDatabase.close ();
					} catch (final DatabaseException exception) {
						this.callbacks.handleException (exception, "bdb datastore encountered a database error while closing the event database; ignoring!");
					} finally {
						this.eventDatabase = null;
					}
				if (this.environment != null)
					try {
						this.environment.close ();
					} catch (final DatabaseException exception) {
						this.callbacks.handleException (exception, "bdb datastore encountered a database error while closing the environment; ignoring!");
					} finally {
						this.environment = null;
					}
				this.state = State.Closed;
				this.callbacks.handleLogEvent (Level.INFO, null, "bdb datastore closed");
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unknown error while closing; aborting!");
				return (false);
			} finally {
				this.eventDatabase = null;
				this.environment = null;
			}
		}
	}
	
	@Override
	public final boolean open ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Closed, "bdb datastore is already opened");
			try {
				this.callbacks.handleLogEvent (Level.DEBUG, null, "bdb datastore opening");
				if (!this.environmentPath.exists ()) {
					this.callbacks.handleLogEvent (Level.WARN, null, "bdb datastore environment path does not exist; creating!");
					if (!this.environmentPath.mkdir ()) {
						this.callbacks.handleLogEvent (Level.ERROR, null, "bdb datastore environment path does not exist, but it can not be created; aborting!");
						return (false);
					}
				}
				if (!this.environmentPath.isDirectory ()) {
					this.callbacks.handleLogEvent (Level.ERROR, null, "bdb datastore environment path exists, but it is not a directory; aborting!");
					return (false);
				}
				try {
					final EnvironmentConfig configuration = new EnvironmentConfig ();
					configuration.setAllowCreate (!this.readOnly);
					configuration.setReadOnly (this.readOnly);
					configuration.setTransactional (false);
					configuration.setLocking (false);
					configuration.setExceptionListener (new ExceptionHandler ());
					this.environment = new Environment (this.environmentPath, configuration);
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (exception, "bdb datastore encountered a database error while opening the environment; aborting!");
					this.close ();
					return (false);
				}
				try {
					final DatabaseConfig onfiguration = new DatabaseConfig ();
					onfiguration.setAllowCreate (!this.readOnly);
					onfiguration.setReadOnly (this.readOnly);
					onfiguration.setSortedDuplicates (false);
					onfiguration.setTransactional (false);
					this.eventDatabase = this.environment.openDatabase (null, BdbDatastore.eventDatabaseName, onfiguration);
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (exception, "bdb datastore encountered a database error while opening the event database; aborting!");
					this.close ();
					return (false);
				}
				this.state = State.Opened;
				this.callbacks.handleLogEvent (Level.INFO, null, "bdb datastore opened");
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unknown error while opening; aborting!");
				this.close ();
				return (false);
			}
		}
	}
	
	public final Database openDatabase (final String name)
			throws DatabaseException
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			final DatabaseConfig databaseConfiguration = new DatabaseConfig ();
			databaseConfiguration.setAllowCreate (!this.readOnly);
			databaseConfiguration.setReadOnly (this.readOnly);
			databaseConfiguration.setSortedDuplicates (false);
			databaseConfiguration.setTransactional (false);
			return (this.environment.openDatabase (null, name, databaseConfiguration));
		}
	}
	
	@Override
	public final Iterable<ILoggingEvent> select (final ILoggingEvent referenceEvent, final int beforeCount, final int afterCount, final LoggingEventFilter filter)
	{
		Preconditions.checkNotNull (referenceEvent);
		Preconditions.checkArgument (beforeCount >= 0);
		Preconditions.checkArgument (afterCount >= 0);
		final long referenceEventTimestamp = referenceEvent.getTimeStamp ();
		Preconditions.checkArgument (referenceEventTimestamp > 0);
		final String referenceKey;
		try {
			final String referenceEventKey = (referenceEvent instanceof SLoggingEvent1) ? ((SLoggingEvent1) referenceEvent).key : null;
			referenceKey = (referenceEventKey != null) ? referenceEventKey : this.encodeMinKey (referenceEventTimestamp);
		} catch (final InternalException exception) {
			throw (new IllegalArgumentException ("bdb datastore encountered an error while preparing the reference key", exception));
		}
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			try {
				final Cursor cursor = this.openCursor (this.eventDatabase, null);
				try {
					final LinkedList<ILoggingEvent> events;
					final String realReferenceKey;
					final ILoggingEvent realReferenceEvent;
					{
						final KeyEventEntryPair outcome = this.searchKeyAfter (cursor, referenceKey);
						if (outcome != null) {
							realReferenceKey = outcome.keyEventPair.key;
							realReferenceEvent = outcome.keyEventPair.event;
						} else {
							realReferenceKey = null;
							realReferenceEvent = null;
						}
					}
					if (realReferenceEvent != null) {
						events = new LinkedList<ILoggingEvent> ();
						if (this.filterEvent (filter, realReferenceEvent))
							events.add (realReferenceEvent);
						outer : for (int index = 0; index < beforeCount; index++) {
							while (true) {
								final KeyEventEntryPair outcome = this.searchBackward (cursor);
								if (outcome == null)
									break outer;
								final ILoggingEvent event = outcome.keyEventPair.event;
								if (this.filterEvent (filter, event)) {
									events.addFirst (event);
									break;
								}
							}
						}
						{
							final KeyEventEntryPair outcome = this.searchKeyAfter (cursor, realReferenceKey);
							if (outcome == null)
								throw (new DatabaseException ());
						}
						outer : for (int index = 0; index < afterCount; index++) {
							while (true) {
								final KeyEventEntryPair outcome = this.searchForward (cursor);
								if (outcome == null)
									break outer;
								final ILoggingEvent event = outcome.keyEventPair.event;
								if (this.filterEvent (filter, event)) {
									events.addLast (event);
									break;
								}
							}
						}
					} else
						events = null;
					return (events);
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (exception, "bdb datastore encountered a database error while selecting the events; aborting!");
					return (null);
				} finally {
					this.closeCursor (cursor);
				}
			} catch (final InternalException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an internal error while selecting the events; aborting!");
				return (null);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unexpected error while selecting the events; aborting!");
				return (null);
			}
		}
	}
	
	@Override
	public final Iterable<ILoggingEvent> select (final long afterTimestamp, final long maximumInterval, final int maximumCount, final LoggingEventFilter filter)
	{
		Preconditions.checkArgument (afterTimestamp >= 0);
		Preconditions.checkArgument (maximumInterval > 0);
		Preconditions.checkArgument (maximumCount > 0);
		final long beforeTimestamp = ((afterTimestamp + maximumInterval) >= 0) ? (afterTimestamp + maximumInterval) : Long.MAX_VALUE;
		final String referneceKey;
		try {
			referneceKey = this.encodeMinKey (afterTimestamp);
		} catch (final InternalException exception) {
			throw (new IllegalArgumentException ("bdb datastore encountered an error while preparing the reference key", exception));
		}
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			try {
				final Cursor cursor = this.openCursor (this.eventDatabase, null);
				try {
					final LinkedList<ILoggingEvent> events;
					final ILoggingEvent realReferenceEvent;
					{
						final KeyEventEntryPair outcome = this.searchKeyAfter (cursor, referneceKey);
						if (outcome != null)
							realReferenceEvent = outcome.keyEventPair.event;
						else
							realReferenceEvent = null;
					}
					if (realReferenceEvent != null) {
						events = new LinkedList<ILoggingEvent> ();
						if (realReferenceEvent.getTimeStamp () < beforeTimestamp) {
							if (this.filterEvent (filter, realReferenceEvent))
								events.add (realReferenceEvent);
							outer : for (int index = 0; index < maximumCount; index++) {
								while (true) {
									final KeyEventEntryPair outcome = this.searchForward (cursor);
									if (outcome == null)
										break outer;
									final ILoggingEvent event = outcome.keyEventPair.event;
									if (event.getTimeStamp () >= beforeTimestamp)
										break outer;
									if (this.filterEvent (filter, event)) {
										events.addLast (event);
										break;
									}
								}
							}
						}
					} else
						events = null;
					return (events);
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (exception, "bdb datastore encountered a database error while selecting the events; aborting!");
					return (null);
				} finally {
					this.closeCursor (cursor);
				}
			} catch (final InternalException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an error internal while selecting the events; aborting!");
				return (null);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unexpected error while selecting the events; aborting!");
				return (null);
			}
		}
	}
	
	@Override
	public final ILoggingEvent select (final String key)
	{
		Preconditions.checkNotNull (key);
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			try {
				final KeyEventEntryPair outcome = this.searchKeyExact (this.eventDatabase, key);
				return ((outcome != null) ? outcome.keyEventPair.event : null);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered a database error while selecting the event; aborting!");
				return (null);
			} catch (final InternalException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an internal error while selecting the event; aborting!");
				return (null);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unknown error while selecting the event; aborting!");
				return (null);
			}
		}
	}
	
	@Override
	public final String store (final ILoggingEvent originalEvent)
	{
		Preconditions.checkNotNull (originalEvent);
		Preconditions.checkState (!this.readOnly);
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			try {
				final KeyEventEntryPair eventEntryPair = this.prepareStoreEntryPair (originalEvent);
				final OperationStatus outcome = this.eventDatabase.put (null, eventEntryPair.entryPair.key, eventEntryPair.entryPair.value);
				if (outcome != OperationStatus.SUCCESS)
					throw (new DatabaseException ());
				this.environment.sync ();
				return (eventEntryPair.keyEventPair.key);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered a database error while storing the event; aborting!");
				return (null);
			} catch (final InternalException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an internal error while storing the event; aborting!");
				return (null);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unknown error while storing the event; aborting!");
				return (null);
			}
		}
	}
	
	public final boolean syncRead ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			return (true);
		}
	}
	
	public final boolean syncWrite ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "bdb datastore is not opened");
			Preconditions.checkState (!this.readOnly, "bdb datastore is read-only");
			try {
				this.environment.sync ();
				return (true);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered a database error while commiting the environment; aborting!");
				return (false);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore encountered an unknown error while commiting the environment; aborting!");
				return (false);
			}
		}
	}
	
	private final void closeCursor (final Cursor cursor)
	{
		try {
			cursor.close ();
		} catch (final DatabaseException exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered a database error while closing the cursor; ignoring!");
		}
	}
	
	private final ILoggingEvent decodeEventEntry (final DatabaseEntry entry)
			throws InternalException
	{
		try {
			return ((ILoggingEvent) this.serializer.deserialize (entry.getData (), entry.getOffset (), entry.getSize ()));
		} catch (final Throwable exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while deserializing the event", exception));
		}
	}
	
	private final String decodeKeyEntry (final DatabaseEntry entry)
			throws InternalException
	{
		try {
			return (new String (entry.getData (), entry.getOffset (), entry.getSize ()));
		} catch (final Error exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while deserializing the key", exception));
		}
	}
	
	private final KeyEventEntryPair decodeKeyEventEntryPair (final DatabaseEntry keyEntry, final DatabaseEntry eventEntry)
			throws InternalException
	{
		final String key = this.decodeKeyEntry (keyEntry);
		final ILoggingEvent event = this.decodeEventEntry (eventEntry);
		return (new KeyEventEntryPair (key, event, keyEntry, eventEntry));
	}
	
	private final KeyEventEntryPair decodeSearchOutcome (final OperationStatus outcome, final DatabaseEntry keyEntry, final DatabaseEntry eventEntry)
			throws DatabaseException,
				InternalException
	{
		final KeyEventEntryPair pair;
		if (outcome == OperationStatus.NOTFOUND)
			pair = null;
		else if (outcome != OperationStatus.SUCCESS)
			throw (new DatabaseException ());
		else {
			pair = this.decodeKeyEventEntryPair (keyEntry, eventEntry);
			this.prepareLoadEvent (pair.keyEventPair.key, pair.keyEventPair.event);
		}
		return (pair);
	}
	
	private final DatabaseEntry encodeEventEntry (final ILoggingEvent event)
			throws InternalException
	{
		try {
			return (new DatabaseEntry (this.serializer.serialize (event)));
		} catch (final Throwable exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while serializing the event", exception));
		}
	}
	
	private final String encodeKey (final byte[] hashBytes)
			throws InternalException
	{
		try {
			final StringBuilder builder = new StringBuilder ();
			for (final byte b : hashBytes) {
				final String s = Integer.toHexString (b & 0xff);
				if (s.length () == 1)
					builder.append ('0').append (s);
				else
					builder.append (s);
			}
			return (builder.toString ());
		} catch (final Error exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while encoding the key", exception));
		}
	}
	
	private final String encodeKey (final long timestamp, final DatabaseEntry eventEntry)
			throws InternalException
	{
		final byte[] data = this.encodeRawKeyFromData (timestamp, eventEntry.getData (), eventEntry.getOffset (), eventEntry.getSize ());
		if (data == null)
			return (null);
		return (this.encodeKey (data));
	}
	
	private final DatabaseEntry encodeKeyEntry (final String key)
	{
		return (new DatabaseEntry (key.getBytes ()));
	}
	
	private final String encodeMinKey (final long timestamp)
			throws InternalException
	{
		return (this.encodeKey (this.encodeRawKeyFromHash (timestamp, BdbDatastore.minHashBytes)));
	}
	
	private final byte[] encodeRawKeyFromData (final long timestamp, final byte[] data, final int offset, final int size)
			throws InternalException
	{
		final MessageDigest hasher;
		try {
			hasher = MessageDigest.getInstance (BdbDatastore.hashAlgorithm);
		} catch (final NoSuchAlgorithmException exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered an error while creating the hasher; aborting!");
			return (null);
		}
		final byte[] hashBytes;
		try {
			hasher.update (data, offset, size);
			hashBytes = hasher.digest ();
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "bdb datastore encountered an error while feedingt the hasher; aborting!");
			return (null);
		}
		if (hashBytes.length != BdbDatastore.hashSize)
			throw (new InternalException ("bdb datastore obtained an invalid hash size"));
		return (this.encodeRawKeyFromHash (timestamp, hashBytes));
	}
	
	private final byte[] encodeRawKeyFromHash (final long timestamp, final byte[] hashBytes)
			throws InternalException
	{
		try {
			final byte[] timestampBytes = new byte[] {(byte) ((timestamp >> 56) & 0xff), (byte) ((timestamp >> 48) & 0xff), (byte) ((timestamp >> 40) & 0xff), (byte) ((timestamp >> 32) & 0xff), (byte) ((timestamp >> 24) & 0xff), (byte) ((timestamp >> 16) & 0xff), (byte) ((timestamp >> 8) & 0xff), (byte) ((timestamp >> 0) & 0xff)};
			final byte[] keyBytes = new byte[timestampBytes.length + hashBytes.length];
			System.arraycopy (timestampBytes, 0, keyBytes, 0, timestampBytes.length);
			System.arraycopy (hashBytes, 0, keyBytes, timestampBytes.length, hashBytes.length);
			return (keyBytes);
		} catch (final Error exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while encoding the key", exception));
		}
	}
	
	private final boolean filterEvent (final LoggingEventFilter filter, final ILoggingEvent event)
			throws InternalException
	{
		try {
			return ((filter != null) ? (filter.filter (event) != FilterReply.DENY) : true);
		} catch (final Throwable exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while filtering the event", exception));
		}
	}
	
	private final void mutateEvent (final LoggingEventMutator mutator, final ILoggingEvent event)
			throws InternalException
	{
		try {
			if (mutator != null)
				mutator.mutate (event);
		} catch (final Throwable exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while mutating the event", exception));
		}
	}
	
	private final Cursor openCursor (final Database database, final CursorConfig cursorConfiguration)
			throws InternalException
	{
		try {
			return (database.openCursor (null, cursorConfiguration));
		} catch (final DatabaseException exception) {
			throw (new InternalException ("bdb datastore encountered a database error while opening the cursor", exception));
		}
	}
	
	private final ILoggingEvent prepareEvent (final String key, final LoggingEventMutator mutator, final ILoggingEvent originalEvent)
			throws InternalException
	{
		try {
			final SLoggingEvent1 clonedEvent;
			if (!(originalEvent instanceof SLoggingEvent1))
				clonedEvent = SLoggingEvent1.build (originalEvent);
			else
				clonedEvent = (SLoggingEvent1) originalEvent;
			this.mutateEvent (mutator, clonedEvent);
			if ((key != null) && (clonedEvent.key == null))
				clonedEvent.key = key;
			return (clonedEvent);
		} catch (final Error exception) {
			throw (new InternalException ("bdb datastore encountered an unknown error while preparing the event", exception));
		}
	}
	
	private final ILoggingEvent prepareLoadEvent (final String key, final ILoggingEvent event)
			throws InternalException
	{
		return (this.prepareEvent (key, this.loadMutator, event));
	}
	
	private final KeyEventEntryPair prepareStoreEntryPair (final ILoggingEvent originalEvent)
			throws InternalException
	{
		final ILoggingEvent clonedEvent = this.prepareStoreEvent (originalEvent);
		final DatabaseEntry eventEntry = this.encodeEventEntry (clonedEvent);
		final String key = this.encodeKey (clonedEvent.getTimeStamp (), eventEntry);
		final DatabaseEntry keyEntry = this.encodeKeyEntry (key);
		return (new KeyEventEntryPair (key, clonedEvent, keyEntry, eventEntry));
	}
	
	private final ILoggingEvent prepareStoreEvent (final ILoggingEvent event)
			throws InternalException
	{
		return (this.prepareEvent (null, this.storeMutator, event));
	}
	
	private final KeyEventEntryPair searchBackward (final Cursor cursor)
			throws DatabaseException,
				InternalException
	{
		final DatabaseEntry keyEntry = new DatabaseEntry ();
		final DatabaseEntry eventEntry = new DatabaseEntry ();
		final OperationStatus outcome = cursor.getPrev (keyEntry, eventEntry, null);
		return (this.decodeSearchOutcome (outcome, keyEntry, eventEntry));
	}
	
	private final KeyEventEntryPair searchForward (final Cursor cursor)
			throws DatabaseException,
				InternalException
	{
		final DatabaseEntry keyEntry = new DatabaseEntry ();
		final DatabaseEntry eventEntry = new DatabaseEntry ();
		final OperationStatus outcome = cursor.getNext (keyEntry, eventEntry, null);
		return (this.decodeSearchOutcome (outcome, keyEntry, eventEntry));
	}
	
	private final KeyEventEntryPair searchKeyAfter (final Cursor cursor, final String key)
			throws DatabaseException,
				InternalException
	{
		final DatabaseEntry keyEntry = this.encodeKeyEntry (key);
		final DatabaseEntry eventEntry = new DatabaseEntry ();
		final OperationStatus outcome = cursor.getSearchKeyRange (keyEntry, eventEntry, null);
		return (this.decodeSearchOutcome (outcome, keyEntry, eventEntry));
	}
	
	private final KeyEventEntryPair searchKeyExact (final Database database, final String key)
			throws DatabaseException,
				InternalException
	{
		final DatabaseEntry keyEntry = this.encodeKeyEntry (key);
		final DatabaseEntry eventEntry = new DatabaseEntry ();
		final OperationStatus outcome = database.get (null, keyEntry, eventEntry, null);
		return (this.decodeSearchOutcome (outcome, keyEntry, eventEntry));
	}
	
	private final Callbacks callbacks;
	private Environment environment;
	private final File environmentPath;
	private Database eventDatabase;
	private final LoggingEventMutator loadMutator;
	private final Object monitor;
	private final boolean readOnly;
	private final Serializer serializer;
	private State state;
	private final LoggingEventMutator storeMutator;
	
	static {
		hashAlgorithm = "MD5";
		hashSize = 16;
		minHashBytes = new byte[BdbDatastore.hashSize];
		Arrays.fill (BdbDatastore.minHashBytes, (byte) 0);
	}
	public static final String eventDatabaseName = "events";
	private static final String hashAlgorithm;
	private static final int hashSize;
	private static final byte[] minHashBytes;
	
	public static enum State
	{
		Closed,
		Opened;
	}
	
	private static final class EntryPair
	{
		public EntryPair (final DatabaseEntry key, final DatabaseEntry value)
		{
			super ();
			this.key = key;
			this.value = value;
		}
		
		public final DatabaseEntry key;
		public final DatabaseEntry value;
	}
	
	private final class ExceptionHandler
			implements
				ExceptionListener
	{
		@Override
		public void exceptionThrown (final ExceptionEvent event)
		{
			BdbDatastore.this.callbacks.handleException (event.getException (), "bdb encountered an unexpected error (in thread `%s`)", event.getThreadName ());
		}
	}
	
	private static final class InternalException
			extends Exception
	{
		public InternalException (final String message)
		{
			super (message);
		}
		
		public InternalException (final String message, final Throwable cause)
		{
			super (message, cause);
		}
		
		private static final long serialVersionUID = 1L;
	}
	
	private static final class KeyEventEntryPair
	{
		public KeyEventEntryPair (final KeyEventPair keyEventPair, final EntryPair databaseEntryPair)
		{
			super ();
			this.keyEventPair = keyEventPair;
			this.entryPair = databaseEntryPair;
		}
		
		public KeyEventEntryPair (final String key, final ILoggingEvent event, final DatabaseEntry keyEntry, final DatabaseEntry valueEntry)
		{
			this (new KeyEventPair (key, event), new EntryPair (keyEntry, valueEntry));
		}
		
		public final EntryPair entryPair;
		public final KeyEventPair keyEventPair;
	}
	
	private static final class KeyEventPair
	{
		public KeyEventPair (final String key, final ILoggingEvent event)
		{
			super ();
			this.key = key;
			this.event = event;
		}
		
		public final ILoggingEvent event;
		public final String key;
	}
}
