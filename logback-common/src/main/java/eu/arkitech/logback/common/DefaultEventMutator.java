
package eu.arkitech.logback.common;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import ch.qos.logback.classic.spi.ILoggingEvent;


public class DefaultEventMutator
		implements
			EventMutator
{
	public DefaultEventMutator ()
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
	
	public void mutate (final ILoggingEvent event_)
	{
		final SLoggingEvent1 event = (SLoggingEvent1) event_;
		long sequence;
		synchronized (this) {
			sequence = this.sequence;
			this.sequence++;
		}
		if (event.mdcPropertyMap == null)
			event.mdcPropertyMap = new HashMap<String, String> (3);
		else
			event.mdcPropertyMap = new HashMap<String, String> (event.mdcPropertyMap);
		event.mdcPropertyMap.put (DefaultEventMutator.sequenceKey, Long.toString (sequence));
		if (!event.mdcPropertyMap.containsKey (DefaultEventMutator.applicationKey))
			event.mdcPropertyMap.put (DefaultEventMutator.applicationKey, this.application != null ? this.application
					: "unknown");
		if (!event.mdcPropertyMap.containsKey (DefaultEventMutator.componentKey))
			event.mdcPropertyMap.put (DefaultEventMutator.componentKey, this.component != null ? this.component : "unknown");
		if (!event.mdcPropertyMap.containsKey (DefaultEventMutator.nodeKey))
			event.mdcPropertyMap.put (DefaultEventMutator.nodeKey, this.node != null ? this.node : "unknown");
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
