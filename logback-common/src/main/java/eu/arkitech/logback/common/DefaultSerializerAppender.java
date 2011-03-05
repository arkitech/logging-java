
package eu.arkitech.logback.common;


public abstract class DefaultSerializerAppender
		extends DefaultAppender
{
	public LoggingEventMutator getMutator ()
	{
		return (this.mutator);
	}
	
	public Serializer getSerializer ()
	{
		return (this.serializer);
	}
	
	public void setMutator (final LoggingEventMutator mutator)
	{
		this.mutator = mutator;
	}
	
	public void setSerializer (final Serializer serializer)
	{
		this.serializer = serializer;
	}
	
	protected LoggingEventMutator mutator;
	protected Serializer serializer;
}
