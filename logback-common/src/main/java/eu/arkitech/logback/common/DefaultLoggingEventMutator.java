
package eu.arkitech.logback.common;


import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Objects;
import com.google.common.base.Strings;


public class DefaultLoggingEventMutator
		implements
			LoggingEventMutator
{
	public DefaultLoggingEventMutator ()
	{
		this.sequence = 0;
		this.application = null;
		this.component = null;
		this.node = null;
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
	
	@Override
	public void mutate (final ILoggingEvent event_)
	{
		final SLoggingEvent1 event = (SLoggingEvent1) event_;
		final String application = Strings.emptyToNull (Objects.firstNonNull (this.application, DefaultLoggingEventMutator.defaultApplication));
		final String component = Strings.emptyToNull (Objects.firstNonNull (this.component, DefaultLoggingEventMutator.defaultComponent));
		final String node = Strings.emptyToNull (Objects.firstNonNull (this.node, DefaultLoggingEventMutator.defaultNode));
		long sequence;
		synchronized (this) {
			sequence = this.sequence;
			this.sequence++;
		}
		
		if (event.mdcPropertyMap == null)
			event.mdcPropertyMap = new HashMap<String, String> (3);
		else
			event.mdcPropertyMap = new HashMap<String, String> (event.mdcPropertyMap);
		if ((application != null) && !event.mdcPropertyMap.containsKey (DefaultLoggingEventMutator.defaultApplicationMdcName))
			event.mdcPropertyMap.put (DefaultLoggingEventMutator.defaultApplicationMdcName, application);
		if ((component != null) && !event.mdcPropertyMap.containsKey (DefaultLoggingEventMutator.defaultComponentMdcName))
			event.mdcPropertyMap.put (DefaultLoggingEventMutator.defaultComponentMdcName, component);
		if ((node != null) && !event.mdcPropertyMap.containsKey (DefaultLoggingEventMutator.defaultNodeMdcName))
			event.mdcPropertyMap.put (DefaultLoggingEventMutator.defaultNodeMdcName, node);
		event.mdcPropertyMap.put (DefaultLoggingEventMutator.defaultSequenceMdcKey, Long.toString (sequence));
		
		final LinkedList<String> mdcInvalidKeys = new LinkedList<String> ();
		for (final String mdcKey : event.mdcPropertyMap.keySet ()) {
			final Object mdcValue = event.mdcPropertyMap.get (mdcKey);
			if (!(mdcValue instanceof String))
				mdcInvalidKeys.add (mdcKey);
		}
		for (final String mdcInvalidKey : mdcInvalidKeys)
			event.mdcPropertyMap.remove (mdcInvalidKey);
		if (event.argumentArray != null)
			for (int index = 0; index < event.argumentArray.length; index++)
				if ((event.argumentArray[index] != null) && !(event.argumentArray[index] instanceof Serializable))
					event.argumentArray[index] = String.valueOf (event.argumentArray[index]);
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
	
	static {
		DefaultLoggingEventMutator.defaultApplication = Strings.emptyToNull (System.getProperty (DefaultLoggingEventMutator.defaultApplicationPropertyName, DefaultLoggingEventMutator.defaultApplication));
		DefaultLoggingEventMutator.defaultComponent = Strings.emptyToNull (System.getProperty (DefaultLoggingEventMutator.defaultComponentPropertyName, DefaultLoggingEventMutator.defaultComponent));
		DefaultLoggingEventMutator.defaultNode = Strings.emptyToNull (System.getProperty (DefaultLoggingEventMutator.defaultNodePropertyName, DefaultLoggingEventMutator.defaultNode));
		if (DefaultLoggingEventMutator.defaultNode == null)
			try {
				DefaultLoggingEventMutator.defaultNode = InetAddress.getLocalHost ().getHostName ();
			} catch (final UnknownHostException exception) {
				DefaultLoggingEventMutator.defaultNode = null;
			}
	}
	
	public static String defaultApplication = null;
	public static final String defaultApplicationMdcName = "application";
	public static final String defaultApplicationPropertyName = "arkitech.logging.application";
	public static String defaultComponent = null;
	public static final String defaultComponentMdcName = "component";
	public static final String defaultComponentPropertyName = "arkitech.logging.component";
	public static String defaultNode = null;
	public static final String defaultNodeMdcName = "node";
	public static final String defaultNodePropertyName = "arkitech.logging.node";
	public static final String defaultSequenceMdcKey = "sequence";
}
