
package eu.arkitech.logging.datastore.lucene;


import java.io.IOException;
import java.util.LinkedList;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.DefaultLoggerCallbacks;
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
			this.monitor = monitor;
			this.readOnly = readOnly;
			this.state = State.Closed;
			this.bdb = bdb;
			this.callbacks = (callbacks != null) ? callbacks : new DefaultLoggerCallbacks (this);
			this.analyzer = new StandardAnalyzer (LuceneIndex.version);
			this.parser = new QueryParser (LuceneIndex.version, LuceneIndex.messageFieldName, this.analyzer);
			this.databaseConfiguration = new DatabaseConfig ();
			this.databaseConfiguration.setAllowCreate (!this.readOnly);
			this.databaseConfiguration.setReadOnly (this.readOnly);
			this.databaseConfiguration.setSortedDuplicates (false);
			this.databaseConfiguration.setTransactional (false);
		}
	}
	
	public final boolean close ()
	{
		synchronized (this.monitor) {
			if (this.state == State.Closed)
				return (false);
			if (this.state != State.Opened)
				throw (new IllegalStateException ("lucene indexer is not opened"));
			if (this.searcher != null)
				try {
					this.searcher.close ();
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "lucene indexer encountered an error while closing the searcher; ignoring!");
				} finally {
					this.searcher = null;
				}
			if (this.writer != null)
				try {
					this.writer.close ();
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "lucene indexer encountered an error while closing the writer; ignoring!");
				} finally {
					this.writer = null;
				}
			if (this.directory != null)
				try {
					this.directory.close ();
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "lucene indexer encountered an error while closing the directory; ignoring!");
				} finally {
					this.directory = null;
				}
			if (this.fileDatabase != null)
				try {
					this.fileDatabase.close ();
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (
							exception, "lucene indexer encountered an error while closing the file database; ignoring!");
				} finally {
					this.fileDatabase = null;
				}
			if (this.blockDatabase != null)
				try {
					this.blockDatabase.close ();
				} catch (final DatabaseException exception) {
					this.callbacks.handleException (
							exception, "lucene indexer encountered an error while closing the block database; ignoring!");
				} finally {
					this.blockDatabase = null;
				}
			this.state = State.Closed;
			return (true);
		}
	}
	
	public final boolean open ()
	{
		synchronized (this.monitor) {
			if (this.state != State.Closed)
				throw (new IllegalStateException ("lucene indexer is already opened"));
			try {
				this.fileDatabase = this.bdb.openDatabase (LuceneIndex.fileDatabaseName, this.databaseConfiguration);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "lucene indexer encountered an error while opening the file database; aborting!");
				this.close ();
				return (false);
			}
			try {
				this.blockDatabase = this.bdb.openDatabase (LuceneIndex.blockDatabaseName, this.databaseConfiguration);
			} catch (final DatabaseException exception) {
				this.callbacks.handleException (
						exception, "lucene indexer encountered an error while opening the block database; aborting!");
				this.close ();
				return (false);
			}
			this.directory = new JEDirectory (null, this.fileDatabase, this.blockDatabase);
			if (!this.readOnly)
				try {
					this.writer = new IndexWriter (this.directory, this.analyzer, MaxFieldLength.UNLIMITED);
				} catch (final IOException exception) {
					this.callbacks.handleException (
							exception, "lucene indexer encountered an error while opening the writer; aborting!");
					this.close ();
					return (false);
				}
			this.state = State.Opened;
			return (true);
		}
	}
	
	public final Query parseQuery (final String query)
			throws ParseException
	{
		if (query == null)
			throw (new IllegalArgumentException ());
		return (this.parser.parse (query));
	}
	
	public final Iterable<LuceneQueryResult> query (final Query query, final int maxCount, final boolean flush)
	{
		if ((query == null) || (maxCount <= 0))
			throw (new IllegalArgumentException ());
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("lucene indexer is not opened"));
			final int count;
			final String[] keys;
			final float[] scores;
			try {
				if (flush && !this.readOnly) {
					this.writer.commit ();
					if (this.searcher != null)
						try {
							this.searcher.close ();
						} catch (final IOException exception) {
							this.callbacks.handleException (
									exception, "lucene indexer encountered an error while closing the searcher; ignoring!");
						} finally {
							this.searcher = null;
						}
				}
				if (this.searcher == null)
					try {
						this.searcher = new IndexSearcher (this.directory, true);
					} catch (final IOException exception) {
						this.callbacks.handleException (
								exception, "lucene indexer encountered an error while opening the searcher; aborting!");
						return (null);
					}
				final TopDocs outcome = this.searcher.search (query, maxCount);
				final ScoreDoc[] results = outcome.scoreDocs;
				count = results.length;
				keys = new String[count];
				scores = new float[count];
				for (int i = 0; i < count; i++) {
					final ScoreDoc result = results[i];
					final Document document = this.searcher.doc (result.doc, LuceneIndex.keyFieldSelector);
					if (document != null) {
						final String key = document.get (LuceneIndex.keyFieldName);
						if (key != null) {
							keys[i] = key;
							scores[i] = result.score;
						} else
							this.callbacks.handleException (
									new Throwable (), "lucene indexer couldn't retrieve the document `%s` key; ignoring!",
									result.doc);
					} else
						this.callbacks
								.handleException (
										new Throwable (), "lucene indexer couldn't retrieve the document `%s`; ignoring!",
										result.doc);
				}
			} catch (final IOException exception) {
				this.callbacks.handleException (
						exception, "lucene indexer encountered an error while accessing the index; aborting!");
				return (null);
			}
			final LinkedList<LuceneQueryResult> results = new LinkedList<LuceneQueryResult> ();
			for (int i = 0; i < count; i++) {
				final String key = keys[i];
				final ILoggingEvent event = this.bdb.select (key);
				if (event != null)
					results.add (new LuceneQueryResult (key, event, scores[i]));
				else
					this.callbacks.handleException (
							new Throwable (), "lucene indexer couldn't retrieve the document `%s`; ignoring!", key);
			}
			return (results);
		}
	}
	
	public final boolean store (final String key, final ILoggingEvent event)
	{
		if ((key == null) || (event == null))
			throw (new IllegalArgumentException ());
		if (this.readOnly)
			throw (new IllegalStateException ());
		final Document document = this.buildDocument (key, event);
		if (document == null)
			return (false);
		synchronized (this.monitor) {
			if (this.state != State.Opened)
				throw (new IllegalStateException ("lucene indexer is not opened"));
			try {
				this.writer.addDocument (document);
			} catch (final IOException exception) {
				this.callbacks.handleException (
						exception, "lucene indexer encountered an error while storing the document `%s`; aborting!", key);
				return (false);
			}
			return (true);
		}
	}
	
	private final Document buildDocument (final String key, final ILoggingEvent event)
	{
		final Document document = new Document ();
		document.add (new Field (LuceneIndex.keyFieldName, key, Store.YES, Index.ANALYZED));
		document.add (new Field (LuceneIndex.levelFieldName, event.getLevel ().levelStr, Store.NO, Index.ANALYZED));
		document.add (new Field (LuceneIndex.loggerFieldName, event.getLoggerName (), Store.NO, Index.ANALYZED));
		document.add (new Field (LuceneIndex.messageFieldName, event.getFormattedMessage (), Store.NO, Index.ANALYZED));
		final IThrowableProxy exception = event.getThrowableProxy ();
		if (exception != null) {
			document
					.add (new Field (LuceneIndex.exceptionClassFieldName, exception.getClassName (), Store.NO, Index.ANALYZED));
			document
					.add (new Field (LuceneIndex.exceptionMessageFieldName, exception.getMessage (), Store.NO, Index.ANALYZED));
		}
		return (document);
	}
	
	private final Analyzer analyzer;
	private final BdbDatastore bdb;
	private Database blockDatabase;
	private final Callbacks callbacks;
	private final DatabaseConfig databaseConfiguration;
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
