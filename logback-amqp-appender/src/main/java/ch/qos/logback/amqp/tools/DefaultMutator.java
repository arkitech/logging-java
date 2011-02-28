
package ch.qos.logback.amqp.tools;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;


public class DefaultMutator
		implements
			Mutator
{
	public DefaultMutator ()
	{
		this.sequence = 0;
		this.application = System.getProperty ("application");
		this.component = System.getProperty ("component");
		try {
			this.node = InetAddress.getLocalHost ().getHostName ();
		} catch (final UnknownHostException exception) {
			this.node = null;
		}
	}
	
	public String getApplication ()
	{
		return (this.application);
	}
	
	public String getComponent ()
	{
		return (this.component);
	}
	
	public String getNode ()
	{
		return (this.node);
	}
	
	public long getSequence ()
	{
		return (this.sequence);
	}
	
	public void mutate (final PubLoggingEventVO event)
	{
		long sequence;
		synchronized (this) {
			sequence = this.sequence;
			this.sequence++;
		}
		if (event.mdcPropertyMap == null)
			event.mdcPropertyMap = new HashMap<String, String> (3);
		else
			event.mdcPropertyMap = new HashMap<String, String> (event.mdcPropertyMap);
		event.mdcPropertyMap.put (DefaultMutator.sequenceKey, Long.toString (sequence));
		if (!event.mdcPropertyMap.containsKey (DefaultMutator.applicationKey))
			event.mdcPropertyMap.put (DefaultMutator.applicationKey, this.application != null ? this.application : "unknown");
		if (!event.mdcPropertyMap.containsKey (DefaultMutator.componentKey))
			event.mdcPropertyMap.put (DefaultMutator.componentKey, this.component != null ? this.component : "unknown");
		if (!event.mdcPropertyMap.containsKey (DefaultMutator.nodeKey))
			event.mdcPropertyMap.put (DefaultMutator.nodeKey, this.node != null ? this.node : "unknown");
	}
	
	public void setApplication (final String application)
	{
		this.application = application;
	}
	
	public void setComponent (final String component)
	{
		this.component = component;
	}
	
	public void setNode (final String node)
	{
		this.node = node;
	}
	
	protected String application;
	protected String component;
	protected String node;
	protected long sequence;
	
	public static final String applicationKey = "application";
	public static final String componentKey = "component";
	public static final String nodeKey = "node";
	public static final String sequenceKey = "sequence";
}
