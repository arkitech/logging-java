
package eu.arkitech.logback.amqp.accessors;


import eu.arkitech.logback.common.DefaultSerializerAppender;


public abstract class AmqpAccessorAppender
		extends DefaultSerializerAppender
{
	public AmqpAccessorAppender ()
	{
		super ();
	}
	
	public String getHost ()
	{
		return (this.host);
	}
	
	public String getPassword ()
	{
		return (this.password);
	}
	
	public Integer getPort ()
	{
		return (this.port);
	}
	
	public String getUsername ()
	{
		return (this.username);
	}
	
	public String getVirtualHost ()
	{
		return (this.virtualHost);
	}
	
	public abstract boolean isRunning ();
	
	public void setHost (final String host)
	{
		this.host = host;
	}
	
	public void setPassword (final String password)
	{
		this.password = password;
	}
	
	public void setPort (final Integer port)
	{
		this.port = port;
	}
	
	public void setUsername (final String username)
	{
		this.username = username;
	}
	
	public void setVirtualHost (final String virtualHost)
	{
		this.virtualHost = virtualHost;
	}
	
	protected String host;
	protected String password;
	protected Integer port;
	protected String username;
	protected String virtualHost;
}
