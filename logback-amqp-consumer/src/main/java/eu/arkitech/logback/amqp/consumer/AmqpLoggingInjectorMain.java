
package eu.arkitech.logback.amqp.consumer;


import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.Pattern;
import ch.qos.logback.core.joran.spi.RuleStore;
import eu.arkitech.logback.common.RandomGenerator;
import org.slf4j.LoggerFactory;


public final class AmqpLoggingInjectorMain
{
	private AmqpLoggingInjectorMain ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		if ((arguments.length != 0) && (arguments.length != 1))
			throw (new IllegalArgumentException (
					"amqp consumer console application may take one argument (the logback configuration); aborting!"));
		
		final File configurationPath = (arguments.length > 1) ? new File (arguments[0]) : null;
		
		final List<AmqpLoggingInjector> collector = Collections.synchronizedList (new LinkedList<AmqpLoggingInjector> ());
		
		if (configurationPath != null) {
			
			if (!configurationPath.isFile ())
				throw (new IllegalArgumentException (String.format (
						"specified logback configuration `%s` does not exist (or is not a file); aborting!",
						configurationPath.getPath ())));
			
			final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory ();
			context.reset ();
			
			final Configurator configurator = new Configurator (collector);
			configurator.setContext (context);
			
			configurator.doConfigure (configurationPath);
			
		} else {
			
			AmqpLoggingInjector.CreateAction.defaultCollector = collector;
			AmqpLoggingInjector.CreateAction.defaultAutoStart = false;
			
			LoggerFactory.getILoggerFactory ();
		}
		
		if (collector.isEmpty ())
			throw (new IllegalArgumentException ("no amqp logging injector defined; aborting!"));
		
		for (final AmqpLoggingInjector agent : collector)
			agent.start ();
		
		while (true) {
			boolean stillRunning = false;
			for (final AmqpLoggingInjector agent : collector)
				stillRunning |= agent.isRunning ();
			if (!stillRunning)
				break;
			try {
				Thread.sleep (AmqpLoggingInjectorMain.defaultWaitTimeout);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		
		System.exit (1);
	}
	
	public static final long defaultWaitTimeout = 1000;
	
	private static final class Configurator
			extends JoranConfigurator
	{
		public Configurator (final List<AmqpLoggingInjector> agents)
		{
			super ();
			this.injectorAction = new AmqpLoggingInjector.CreateAction (agents, false);
			this.injectorAction.setContext (this.getContext ());
			this.generatorAction = new RandomGenerator.CreateAction ();
		}
		
		public final void addInstanceRules (final RuleStore rules)
		{
			super.addInstanceRules (rules);
			rules.addRule (new Pattern ("/configuration/amqpLoggingInjector"), this.injectorAction);
			rules.addRule (new Pattern ("/configuration/randomGenerator"), this.generatorAction);
		}
		
		public final void setContext (final Context context)
		{
			super.setContext (context);
			this.injectorAction.setContext (context);
		}
		
		private final Action generatorAction;
		private final Action injectorAction;
	}
}
