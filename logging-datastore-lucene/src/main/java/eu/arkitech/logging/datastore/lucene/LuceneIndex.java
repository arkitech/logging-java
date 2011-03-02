
package eu.arkitech.logging.datastore.lucene;


import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.TopDocs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.sleepycat.je.Database;
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
import org.apache.lucene.store.je.JEDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class LuceneIndex
{
	public LuceneIndex (final BdbDatastore bdb)
	{
		super ();
		this.logger = LoggerFactory.getLogger (LuceneIndex.class);
		this.callbacks = new Callbacks (this.logger);
		this.bdb = bdb;
		this.analyzer = new StandardAnalyzer (LuceneIndex.version);
		this.parser = new QueryParser (LuceneIndex.version, LuceneIndex.messageFieldName, this.analyzer);
	}
	
	public final boolean close ()
	{
		try {
			this.searcher.close ();
		} catch (final IOException exception) {
			this.callbacks.handleException (
					exception, "lucene index encountered an error while closing the index searcher; ignoring!");
		}
		try {
			this.writer.close ();
		} catch (final IOException exception) {
			this.callbacks.handleException (
					exception, "lucene index encountered an error while closing the index writer; ignoring!");
		}
		try {
			this.directory.close ();
		} catch (final IOException exception) {
			this.callbacks.handleException (
					exception, "lucene index encountered an error while closing the directory; ignoring!");
		}
		return (true);
	}
	
	public final boolean open ()
	{
		this.fileDatabase = this.bdb.getLuceneFileDatabase ();
		this.blockDatabase = this.bdb.getLuceneBlockDatabase ();
		this.directory = new JEDirectory (null, this.fileDatabase, this.blockDatabase);
		try {
			this.writer = new IndexWriter (this.directory, this.analyzer, MaxFieldLength.UNLIMITED);
		} catch (final IOException exception) {
			this.callbacks.handleException (
					exception, "lucene indexer encountered an error while opening the index writer; aborting!");
			this.close ();
			return (false);
		}
		try {
			this.searcher = new IndexSearcher (this.directory, true);
		} catch (final IOException exception) {
			this.callbacks.handleException (
					exception, "lucene indexer encountered an error while opening the index; aborting!");
			this.close ();
			return (false);
		}
		return (true);
	}
	
	public final Query parseQuery (final String query)
			throws ParseException
	{
		return (this.parser.parse (query));
	}
	
	public final List<LuceneQueryResult> query (final Query query, final int maxCount)
	{
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
					this.callbacks.handleException (
							new Throwable (), "lucene indexer couldn't retrieve the document `%s`; ignoring!", result.doc);
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
				this.callbacks.handleException (new Throwable (), "lucene indexer couldn't retrieve the document `%s`; ignoring!", key);
		}
		return (results);
	}
	
	public final boolean store (final String key, final ILoggingEvent event)
	{
		final Document document = this.buildDocument (key, event);
		try {
			this.writer.addDocument (document);
		} catch (final IOException exception) {
			this.callbacks.handleException (
					exception, "lucene indexer encountered an error while opening the index writer; aborting!");
			return (false);
		}
		return (true);
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
	private JEDirectory directory;
	private Database fileDatabase;
	private final Logger logger;
	private final QueryParser parser;
	private IndexSearcher searcher;
	private IndexWriter writer;
	
	public static final String exceptionClassFieldName = "exception-class";
	public static final String exceptionMessageFieldName = "exception-message";
	public static final String keyFieldName = "key";
	public static final KeyFieldSelector keyFieldSelector = new KeyFieldSelector ();
	public static final String levelFieldName = "level";
	public static final String loggerFieldName = "logger";
	public static final String messageFieldName = "message";
	@SuppressWarnings ("deprecation")
	public static final Version version = Version.LUCENE_CURRENT;
	
	private final class Callbacks
	{
		Callbacks (final Logger logger)
		{
			super ();
			this.logger = logger;
		}
		
		public final void handleException (
				final Throwable exception, final String messageFormat, final Object ... messageArguments)
		{
			this.logger.error (String.format (messageFormat, messageArguments), exception);
		}
		
		private final Logger logger;
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
