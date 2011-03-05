
package eu.arkitech.logging.datastore.lucene;


import java.io.IOException;
import java.util.LinkedList;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logging.datastore.bdb.BdbDatastore;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.je.JEDirectory;
import org.apache.lucene.util.Version;


public final class LuceneIndex
{
	public LuceneIndex (final BdbDatastore bdb, final boolean readOnly, final Callbacks callbacks, final Object monitor)
	{
		super ();
		synchronized (monitor) {
			this.monitor = Preconditions.checkNotNull (monitor);
			this.callbacks = Preconditions.checkNotNull (callbacks);
			this.readOnly = readOnly;
			this.bdb = Preconditions.checkNotNull (bdb);
			this.analyzer = new StandardAnalyzer (LuceneIndex.version);
			this.parser = new QueryParser (LuceneIndex.version, LuceneIndex.messageFieldName, this.analyzer);
			this.state = State.Closed;
		}
	}
	
	public final boolean close ()
	{
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			Preconditions.checkState (this.state == State.Opened, "lucene index is not opened");
			try {
				this.callbacks.handleLogEvent (Level.INFO, null, "lucene index closing");
				if (this.searcher != null)
					try {
						this.searcher.close ();
					} catch (final IOException exception) {
						this.callbacks.handleException (
								exception, "lucene index encountered a database error while closing the searcher; ignoring!");
					} finally {
						this.searcher = null;
					}
				if (this.writer != null)
					try {
						this.writer.close ();
					} catch (final IOException exception) {
						this.callbacks.handleException (
								exception, "lucene index encountered a database error while closing the writer; ignoring!");
					} finally {
						this.writer = null;
					}
				if (this.directory != null)
					try {
						this.directory.close ();
					} catch (final IOException exception) {
						this.callbacks
								.handleException (
										exception,
										"lucene index encountered a database error while closing the directory; ignoring!");
					} finally {
						this.directory = null;
					}
				if (this.fileDatabase != null)
					try {
						this.fileDatabase.close ();
					} catch (final DatabaseException exception) {
						this.callbacks.handleException (
								exception,
								"lucene index encountered a database error while closing the file database; ignoring!");
					} finally {
						this.fileDatabase = null;
					}
				if (this.blockDatabase != null)
					try {
						this.blockDatabase.close ();
					} catch (final DatabaseException exception) {
						this.callbacks.handleException (
								exception,
								"lucene index encountered a database error while closing the block database; ignoring!");
					} finally {
						this.blockDatabase = null;
					}
				this.state = State.Closed;
				this.callbacks.handleLogEvent (Level.INFO, null, "lucene index closed");
				return (true);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered an unknown error while closing; aborting!");
				return (false);
			} finally {
				this.searcher = null;
				this.writer = null;
				this.directory = null;
				this.fileDatabase = null;
				this.blockDatabase = null;
			}
		}
	}
	
	public final boolean open ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Closed, "lucene indexe is already opened");
			try {
				this.callbacks.handleLogEvent (Level.INFO, null, "lucene index opening");
				try {
					this.fileDatabase = this.bdb.openDatabase (LuceneIndex.fileDatabaseName);
				} catch (final DatabaseException exception) {
					this.callbacks
							.handleException (
									exception,
									"lucene index encountered a database error while opening the file database; aborting!");
					this.close ();
					return (false);
				}
				try {
					this.blockDatabase = this.bdb.openDatabase (LuceneIndex.blockDatabaseName);
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (
							exception,
							"lucene index encountered a database error while opening the block database; aborting!");
					this.close ();
					return (false);
				}
				this.directory = new JEDirectory (null, this.fileDatabase, this.blockDatabase);
				if (!this.readOnly)
					try {
						this.writer = new IndexWriter (this.directory, this.analyzer, MaxFieldLength.UNLIMITED);
					} catch (final IOException exception) {
						this.callbacks.handleException (
								exception, "lucene indexer encountered a database error while opening the writer; aborting!");
						this.close ();
						return (false);
					}
				this.callbacks.handleLogEvent (Level.INFO, null, "lucene index opened");
				this.state = State.Opened;
				return (true);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered an unknown error while opening; aborting");
				this.close ();
				return (false);
			}
		}
	}
	
	public final Query parseQuery (final String query)
			throws ParseException
	{
		Preconditions.checkNotNull (query);
		return (this.parser.parse (query));
	}
	
	public final Iterable<LuceneQueryResult> query (final Query query, final int maxCount)
	{
		Preconditions.checkNotNull (query);
		Preconditions.checkArgument (maxCount > 0);
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "lucene index is not opened");
			try {
				if (this.searcher == null)
					try {
						this.searcher = new IndexSearcher (this.directory, true);
					} catch (final IOException exception) {
						this.callbacks.handleException (
								exception, "lucene index encountered a database error while opening the searcher; aborting!");
						return (null);
					}
				final int count;
				final String[] keys;
				final float[] scores;
				try {
					final TopDocs outcome = this.searcher.search (query, maxCount);
					final ScoreDoc[] results = outcome.scoreDocs;
					count = results.length;
					keys = new String[count];
					scores = new float[count];
					for (int i = 0; i < count; i++) {
						final ScoreDoc result = results[i];
						final Document document = this.searcher.doc (result.doc, LuceneIndex.keyFieldSelector);
						final String key = document.get (LuceneIndex.keyFieldName);
						if (key == null) {
							this.callbacks.handleLogEvent (
									Level.ERROR, null, "lucene index can not retrieve the document key; ignoring!");
							continue;
						}
						keys[i] = key;
						scores[i] = result.score;
					}
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "lucene index encountered a database error while querying the searcher; aborting!");
					return (null);
				}
				final LinkedList<LuceneQueryResult> results = new LinkedList<LuceneQueryResult> ();
				for (int i = 0; i < count; i++) {
					final String key = keys[i];
					final ILoggingEvent event = this.bdb.select (key);
					if (event == null) {
						this.callbacks.handleLogEvent (
								Level.ERROR, null, "lucene index can not retrieve the event; ignoring!");
						continue;
					}
					results.add (new LuceneQueryResult (key, event, scores[i]));
				}
				return (results);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered an unknown error while querying; aborting!");
				return (null);
			}
		}
	}
	
	public final boolean store (final String key, final ILoggingEvent event)
	{
		Preconditions.checkNotNull (key);
		Preconditions.checkNotNull (event);
		synchronized (this) {
			Preconditions.checkState (this.state == State.Opened, "lucene index is not opened");
			Preconditions.checkState (!this.readOnly, "lucene index is read-only");
			try {
				final Document document = this.buildDocument (key, event);
				this.writer.addDocument (document);
				return (true);
			} catch (final IOException exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered a database error while storing the document; aborting!");
				return (false);
			} catch (final InternalException exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered an internal error while storing the document; aborting!");
				return (false);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered an unknown error while storing the document; aborting!");
				return (false);
			}
		}
	}
	
	public final boolean syncRead ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "lucene index is not opened");
			if (this.searcher != null)
				try {
					this.searcher.close ();
					this.searcher = null;
					return (true);
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "lucene index encountered a database error while closing the searcher; aborting");
					return (false);
				} catch (final Throwable exception) {
					this.callbacks.handleException (
							exception, "lucene index encountered an unknown error while closing the searcher; aborting");
					return (false);
				}
			else
				return (true);
		}
	}
	
	public final boolean syncWrite ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.state == State.Opened, "lucene index is not opened");
			Preconditions.checkState (!this.readOnly, "lucene index is read-only");
			try {
				this.writer.commit ();
				return (true);
			} catch (final IOException exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered a database error while commiting the writer; aborting");
				return (false);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "lucene index encountered an unknown error while commiting the writer; aborting");
				return (false);
			}
		}
	}
	
	private final Document buildDocument (final String key, final ILoggingEvent event)
			throws InternalException
	{
		try {
			final Document document = new Document ();
			document.add (new Field (LuceneIndex.keyFieldName, key, Store.YES, Index.ANALYZED));
			document.add (new Field (LuceneIndex.levelFieldName, event.getLevel ().levelStr, Store.NO, Index.ANALYZED));
			document.add (new Field (LuceneIndex.loggerFieldName, event.getLoggerName (), Store.NO, Index.ANALYZED));
			document.add (new Field (LuceneIndex.messageFieldName, event.getFormattedMessage (), Store.NO, Index.ANALYZED));
			final IThrowableProxy exception = event.getThrowableProxy ();
			if (exception != null) {
				document.add (new Field (
						LuceneIndex.exceptionClassFieldName, exception.getClassName (), Store.NO, Index.ANALYZED));
				document.add (new Field (
						LuceneIndex.exceptionMessageFieldName, exception.getMessage (), Store.NO, Index.ANALYZED));
			}
			return (document);
		} catch (final Throwable exception) {
			throw (new InternalException ("lucene index encountered an error while building the document", exception));
		}
	}
	
	private final Analyzer analyzer;
	private final BdbDatastore bdb;
	private Database blockDatabase;
	private final Callbacks callbacks;
	private JEDirectory directory;
	private Database fileDatabase;
	private final Object monitor;
	private final QueryParser parser;
	private final boolean readOnly;
	private IndexSearcher searcher;
	private State state;
	private IndexWriter writer;
	
	public static final String blockDatabaseName = "lucene-blocks";
	public static final String exceptionClassFieldName = "exception-class";
	public static final String exceptionMessageFieldName = "exception-message";
	public static final String fileDatabaseName = "lucene-files";
	public static final String keyFieldName = "key";
	public static final KeyFieldSelector keyFieldSelector = new KeyFieldSelector ();
	public static final String levelFieldName = "level";
	public static final String loggerFieldName = "logger";
	public static final String messageFieldName = "message";
	@SuppressWarnings ("deprecation")
	public static final Version version = Version.LUCENE_CURRENT;
	
	public static enum State
	{
		Closed,
		Opened;
	}
	
	private final class InternalException
			extends Exception
	{
		public InternalException (final String message, final Throwable cause)
		{
			super (message, cause);
		}
		
		private static final long serialVersionUID = 1L;
	}
	
	private static final class KeyFieldSelector
			implements
				FieldSelector
	{
		public final FieldSelectorResult accept (final String name)
		{
			return (LuceneIndex.keyFieldName.equals (name) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD);
		}
		
		private static final long serialVersionUID = 1L;
	}
}
