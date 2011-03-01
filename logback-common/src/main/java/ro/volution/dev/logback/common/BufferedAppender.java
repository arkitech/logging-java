
package ro.volution.dev.logback.common;


import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;


public class BufferedAppender
		extends UnsynchronizedAppenderBase<ILoggingEvent>
		implements
			BlockingQueue<ILoggingEvent>
{
	public BufferedAppender ()
	{
		super ();
		this.events = new LinkedBlockingDeque<ILoggingEvent> ();
	}
	
	public boolean add (final ILoggingEvent e)
	{
		return (this.add (e));
	}
	
	public boolean addAll (final Collection<? extends ILoggingEvent> c)
	{
		return (this.events.addAll (c));
	}
	
	public void clear ()
	{
		this.events.clear ();
	}
	
	public boolean contains (final Object o)
	{
		return (this.events.contains (o));
	}
	
	public boolean containsAll (final Collection<?> c)
	{
		return (this.events.containsAll (c));
	}
	
	public int drainTo (final Collection<? super ILoggingEvent> c)
	{
		return (this.events.drainTo (c));
	}
	
	public int drainTo (final Collection<? super ILoggingEvent> c, final int maxElements)
	{
		return (this.events.drainTo (c, maxElements));
	}
	
	public ILoggingEvent element ()
	{
		return (this.events.element ());
	}
	
	public boolean isEmpty ()
	{
		return (this.events.isEmpty ());
	}
	
	public Iterator<ILoggingEvent> iterator ()
	{
		return (this.events.iterator ());
	}
	
	public boolean offer (final ILoggingEvent e)
	{
		return (this.events.offer (e));
	}
	
	public boolean offer (final ILoggingEvent e, final long timeout, final TimeUnit unit)
			throws InterruptedException
	{
		return (this.events.offer (e, timeout, unit));
	}
	
	public ILoggingEvent peek ()
	{
		return (this.events.peek ());
	}
	
	public ILoggingEvent poll ()
	{
		return (this.events.poll ());
	}
	
	public ILoggingEvent poll (final long timeout, final TimeUnit unit)
			throws InterruptedException
	{
		return (this.events.poll (timeout, unit));
	}
	
	public void put (final ILoggingEvent e)
			throws InterruptedException
	{
		this.events.put (e);
	}
	
	public int remainingCapacity ()
	{
		return (this.events.remainingCapacity ());
	}
	
	public ILoggingEvent remove ()
	{
		return (this.events.remove ());
	}
	
	public boolean remove (final Object o)
	{
		return (this.events.remove (o));
	}
	
	public boolean removeAll (final Collection<?> c)
	{
		return (this.events.removeAll (c));
	}
	
	public boolean retainAll (final Collection<?> c)
	{
		return (this.events.retainAll (c));
	}
	
	public int size ()
	{
		return (this.events.size ());
	}
	
	public ILoggingEvent take ()
			throws InterruptedException
	{
		return (this.events.take ());
	}
	
	public Object[] toArray ()
	{
		return (this.events.toArray ());
	}
	
	public <T> T[] toArray (final T[] a)
	{
		return (this.events.toArray (a));
	}
	
	protected void append (final ILoggingEvent event)
	{
		this.events.add (event);
	}
	
	private final LinkedBlockingDeque<ILoggingEvent> events;
}
