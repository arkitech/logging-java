
package eu.arkitech.logback.amqp.accessors;


import com.google.common.base.Objects;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.WorkerConfiguration;


public abstract class AmqpRawAccessorConfiguration
		extends WorkerConfiguration
{
	protected AmqpRawAccessorConfiguration (final AmqpRawAccessorConfiguration override, final AmqpRawAccessorConfiguration overriden)
	{
		super (override, overriden);
		this.host = Objects.firstNonNull (override.host, overriden.host);
		this.port = Objects.firstNonNull (override.port, overriden.port);
		this.virtualHost = Objects.firstNonNull (override.virtualHost, overriden.virtualHost);
		this.username = Objects.firstNonNull (override.username, overriden.username);
		this.password = Objects.firstNonNull (override.password, overriden.password);
	}
	
	protected AmqpRawAccessorConfiguration (final String host, final Integer port, final String virtualHost, final String username, final String password, final Callbacks callbacks, final Object monitor)
	{
		super (callbacks, monitor);
		this.host = host;
		this.port = port;
		this.virtualHost = virtualHost;
		this.username = username;
		this.password = password;
	}
	
	public final String host;
	public final String password;
	public final Integer port;
	public final String username;
	public final String virtualHost;
	public static final String defaultHost = "127.0.0.1";
	public static final String defaultPassword = "guest";
	public static final Integer defaultPort = 5672;
	public static final String defaultUsername = "guest";
	public static final String defaultVirtualHost = "/";
}
