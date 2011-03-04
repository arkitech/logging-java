
package eu.arkitech.logback.amqp.consumer;


import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Pattern;
import ch.qos.logback.core.joran.spi.RuleStore;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;


public final class AmqpLoggingInjectorMain
{
	private AmqpLoggingInjectorMain ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		if (arguments.length != 1)
			throw (new IllegalArgumentException (
					"amqp consumer agent main takes exactly one argument (the logback configuration); aborting!"));
		
		final File configurationPath = new File (arguments[0]);
		if (!configurationPath.isFile ())
			throw (new IllegalArgumentException (String.format (
					"specified logback configuration `%s` does not exist (or is not a file); aborting!",
					configurationPath.getPath ())));
		
		final List<AmqpLoggingInjector> agents = Collections.synchronizedList (new LinkedList<AmqpLoggingInjector> ());
		
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory ();
		context.reset ();
		
		final Configurator configurator = new Configurator (agents);
		configurator.setContext (context);
		configurator.doConfigure (configurationPath);
		
		if (agents.isEmpty ())
			throw (new IllegalArgumentException ("no amqp consumer agents defined; aborting!"));
		
		for (final AmqpLoggingInjector agent : agents)
			agent.start ();
		
		while (true) {
			boolean stillRunning = false;
			for (final AmqpLoggingInjector agent : agents)
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
	
	public static final class Configurator
			extends JoranConfigurator
	{
		public Configurator (final List<AmqpLoggingInjector> agents)
		{
			super ();
			this.agentAction = new JoranAction (agents, false);
			this.agentAction.setContext (this.getContext ());
		}
		
		public final void addInstanceRules (final RuleStore rules)
		{
			super.addInstanceRules (rules);
			rules.addRule (new Pattern ("/configuration/amqpLoggingInjector"), this.agentAction);
		}
		
		public final void setContext (final Context context)
		{
			super.setContext (context);
			this.agentAction.setContext (context);
		}
		
		private final JoranAction agentAction;
	}
	
	public static final class JoranAction
			extends Action
	{
		public JoranAction ()
		{
			this (null, true);
		}
		
		public JoranAction (final List<AmqpLoggingInjector> agents, final boolean autoStart)
		{
			super ();
			this.agents = agents;
			this.autoStart = autoStart;
			this.agent = null;
		}
		
		public void begin (final InterpretationContext ic, final String name, final Attributes attributes)
		{
			if (this.agent != null)
				throw (new IllegalStateException ());
			this.agent = new AmqpLoggingInjector ();
			this.agent.setContext (this.getContext ());
			ic.pushObject (this.agent);
		}
		
		public void end (final InterpretationContext ic, final String name)
		{
			if (this.agent == null)
				throw (new IllegalStateException ());
			if (ic.popObject () != this.agent)
				throw (new IllegalStateException ());
			if (this.autoStart)
				this.agent.start ();
			if (this.agents != null)
				this.agents.add (this.agent);
			this.agent = null;
		}
		
		private AmqpLoggingInjector agent;
		private final List<AmqpLoggingInjector> agents;
		private final boolean autoStart;
	}
}
