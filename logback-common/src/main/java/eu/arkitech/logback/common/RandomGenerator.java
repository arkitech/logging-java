
package eu.arkitech.logback.common;


import java.util.List;
import java.util.Random;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;


public class RandomGenerator
		extends ContextAwareBase
{
	public RandomGenerator ()
	{
		this (null, null);
	}
	
	public RandomGenerator (final Class<?> source)
	{
		this (null, source.getName ());
	}
	
	public RandomGenerator (final Logger logger)
	{
		this (logger, null);
	}
	
	public RandomGenerator (final Logger logger, final String loggerName)
	{
		super ();
		this.random = new Random ();
		this.logger = logger;
		this.loggerName = loggerName;
		this.loopCount = RandomGenerator.defaultLoopCount;
		this.initialDelay = RandomGenerator.defaultInitialDelay;
		this.loopDelay = RandomGenerator.defaultLoopDelay;
	}
	
	public RandomGenerator (final String loggerName)
	{
		this (null, loggerName);
	}
	
	public void append ()
	{
		final ILoggingEvent event = this.generate ();
		final String loggerName = event.getLoggerName ();
		final Logger logger = (Logger) LoggerFactory.getLogger (Objects.firstNonNull (loggerName, RandomGenerator.class.getName ()));
		logger.callAppenders (event);
	}
	
	public ILoggingEvent generate ()
	{
		final Logger logger = this.resolveLogger ();
		final float levelDice = this.random.nextFloat ();
		final float exceptionDice = this.random.nextFloat ();
		final float exception2Dice = this.random.nextFloat ();
		final String message = UUID.randomUUID ().toString ();
		final Level level;
		if (levelDice < 0.1)
			level = Level.TRACE;
		else if (levelDice < 0.3)
			level = Level.DEBUG;
		else if (levelDice < 0.7)
			level = Level.INFO;
		else if (levelDice < 0.85)
			level = Level.WARN;
		else if (levelDice < 1.0)
			level = Level.ERROR;
		else
			throw (new AssertionError ());
		final Throwable exception;
		if (exceptionDice < 0.1)
			if (exception2Dice < 0.2)
				exception = new Throwable (UUID.randomUUID ().toString (), new Throwable (UUID.randomUUID ().toString ()));
			else
				exception = new Throwable (UUID.randomUUID ().toString ());
		else
			exception = null;
		return (new LoggingEvent (null, logger, level, message, exception, null));
	}
	
	public long getInitialDelay ()
	{
		return (this.initialDelay);
	}
	
	public Logger getLogger ()
	{
		return (this.logger);
	}
	
	public String getLoggerName ()
	{
		return (this.loggerName);
	}
	
	public long getLoopCount ()
	{
		return (this.loopCount);
	}
	
	public long getLoopDelay ()
	{
		return (this.loopDelay);
	}
	
	public Logger resolveLogger ()
	{
		final Logger configuredLogger = this.logger;
		final String configuredLoggerName = Strings.emptyToNull (this.loggerName);
		final Logger logger;
		if (configuredLogger != null)
			logger = configuredLogger;
		else if (configuredLoggerName != null)
			logger = (Logger) LoggerFactory.getLogger (configuredLoggerName);
		else
			logger = (Logger) LoggerFactory.getLogger (RandomGenerator.class);
		if (logger == null)
			throw (new IllegalStateException ());
		return (logger);
	}
	
	public void setInitialDelay (final long delay)
	{
		this.initialDelay = delay;
	}
	
	public void setLogger (final Logger logger)
	{
		this.logger = logger;
	}
	
	public void setLoggerName (final String loggerName)
	{
		this.loggerName = loggerName;
	}
	
	public void setLoopCount (final long count)
	{
		this.loopCount = count;
	}
	
	public void setLoopDelay (final long delay)
	{
		this.loopDelay = delay;
	}
	
	public Thread start ()
	{
		return (this.start (this.loopCount, this.initialDelay, this.loopDelay));
	}
	
	public RandomGeneratorThread start (final long loopCount, final long initialDelay, final long loopDelay)
	{
		final RandomGeneratorThread thread = new RandomGeneratorThread (this, loopCount, initialDelay, loopDelay);
		thread.start ();
		return (thread);
	}
	
	protected long initialDelay;
	protected Logger logger;
	protected String loggerName;
	protected long loopCount;
	protected long loopDelay;
	protected final Random random;
	
	public static long defaultInitialDelay = RandomGenerator.defaultLoopDelay;
	public static long defaultLoopCount = 360;
	public static long defaultLoopDelay = 1000;
	
	public static final class CreateAction
			extends ObjectNewInstanceAction<RandomGenerator>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<? super RandomGenerator> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (RandomGenerator.class, collector, autoRegister, autoStart);
		}
		
		@Override
		protected void startObject ()
		{
			this.object.start ();
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<? super RandomGenerator> defaultCollector = null;
	}
	
	public static final class RandomGeneratorThread
			extends WorkerThread
	{
		public RandomGeneratorThread (final RandomGenerator generator, final long loopCount, final long initialDelay, final long loopDelay)
		{
			super (generator.getClass ().getSimpleName (), Thread.MIN_PRIORITY);
			Preconditions.checkNotNull (generator);
			Preconditions.checkArgument (loopCount >= 0);
			Preconditions.checkArgument (initialDelay >= 0);
			Preconditions.checkArgument (loopDelay >= 0);
			this.generator = generator;
			this.loopCount = loopCount;
			this.initialDelay = initialDelay;
			this.loopDelay = loopDelay;
		}
		
		@Override
		protected void executeLoop ()
		{
			try {
				Thread.sleep (this.initialDelay);
			} catch (final InterruptedException exception) {
				return;
			}
			long index = 0;
			while (true) {
				if (this.shouldStopSoft ())
					break;
				if (index == this.loopCount)
					break;
				this.generator.append ();
				index++;
				if (index == this.loopCount)
					break;
				try {
					Thread.sleep (this.loopDelay);
				} catch (final InterruptedException exception) {
					break;
				}
			}
		}
		
		@Override
		protected void finalizeLoop ()
		{}
		
		@Override
		protected void handleException (final Throwable exception)
		{
			exception.printStackTrace ();
		}
		
		@Override
		protected void initializeLoop ()
		{}
		
		protected final RandomGenerator generator;
		protected final long initialDelay;
		protected final long loopCount;
		protected final long loopDelay;
	}
}
