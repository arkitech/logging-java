
package ch.qos.logback.amqp;


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


public final class AmqpConsumerAgentMain
{
	private AmqpConsumerAgentMain ()
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
		
		final List<AmqpConsumerAgent> agents = Collections.synchronizedList (new LinkedList<AmqpConsumerAgent> ());
		
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory ();
		context.reset ();
		
		final Configurator configurator = new Configurator (agents);
		configurator.setContext (context);
		configurator.doConfigure (configurationPath);
		
		if (agents.isEmpty ())
			throw (new IllegalArgumentException ("no amqp consumer agents defined; aborting!"));
		
		for (final AmqpConsumerAgent agent : agents)
			agent.start ();
		
		while (true) {
			for (final AmqpConsumerAgent agent : agents)
				if (!agent.isRunning ())
					agents.remove (agent);
			if (agents.isEmpty ())
				break;
			try {
				Thread.sleep (AmqpConsumerAgent.waitTimeout);
			} catch (final InterruptedException exception) {
				break;
			}
		}
		
		System.exit (1);
	}
	
	public static final class AgentAction
			extends Action
	{
		public AgentAction (final List<AmqpConsumerAgent> agents)
		{
			super ();
			this.agents = agents;
			this.agent = null;
		}
		
		public void begin (final InterpretationContext ic, final String name, final Attributes attributes)
		{
			if (this.agent != null)
				throw (new IllegalStateException ());
			this.agent = new AmqpConsumerAgent ();
			this.agent.setContext (this.getContext ());
			ic.pushObject (this.agent);
		}
		
		public void end (final InterpretationContext ic, final String name)
		{
			if (this.agent == null)
				throw (new IllegalStateException ());
			if (ic.popObject () != this.agent)
				throw (new IllegalStateException ());
			this.agents.add (this.agent);
			this.agent = null;
		}
		
		private AmqpConsumerAgent agent;
		private final List<AmqpConsumerAgent> agents;
	}
	
	public static final class Configurator
			extends JoranConfigurator
	{
		public Configurator (final List<AmqpConsumerAgent> agents)
		{
			super ();
			this.agentAction = new AgentAction (agents);
			this.agentAction.setContext (this.getContext ());
		}
		
		public final void addInstanceRules (final RuleStore rules)
		{
			super.addInstanceRules (rules);
			rules.addRule (new Pattern ("/configuration/amqpConsumerAgent"), this.agentAction);
		}
		
		public final void setContext (final Context context)
		{
			super.setContext (context);
			this.agentAction.setContext (context);
		}
		
		private final AgentAction agentAction;
	}
}
