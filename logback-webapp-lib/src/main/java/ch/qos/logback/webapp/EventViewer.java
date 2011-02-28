
package ch.qos.logback.webapp;


import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;


public class EventViewer
		extends HttpServlet
{
	public EventViewer ()
	{
		super ();
	}
	
	public void destroy ()
	{
		super.destroy ();
		this.appender = null;
		this.layout.stop ();
		this.layout = null;
	}
	
	public void init (final ServletConfig configuration)
			throws ServletException
	{
		final String appenderName = configuration.getInitParameter (EventViewer.appenderParameterName);
		if ((appenderName == null) || appenderName.isEmpty ())
			throw (new ServletException (String.format (
					"logback event viewer `%s` parameter is not set; aborting!", EventViewer.appenderParameterName)));
		final String eventPattern = configuration.getInitParameter (EventViewer.eventPatternParameterName);
		
		this.rootLogger = (Logger) LoggerFactory.getLogger (org.slf4j.Logger.ROOT_LOGGER_NAME);
		final LoggerContext context = this.rootLogger.getLoggerContext ();
		
		final Object appender = context.getObject (appenderName);
		if (appender == null)
			throw (new ServletException (String.format (
					"logback event viewer `%s` parameter value `%s` is wrong (no appender of such name found)",
					EventViewer.appenderParameterName, appenderName)));
		if (!(appender instanceof EventViewerAppender))
			throw (new ServletException (String.format (
					"logback event viewer `%s` parameter value `%s` is wrong (appender has wrong class `%s`)",
					EventViewer.appenderParameterName, appenderName, appender.getClass ().getName ())));
		
		super.init ();
		
		this.appender = (EventViewerAppender) appender;
		
		this.layout = new HTMLLayout ();
		this.layout.setContext (context);
		if (eventPattern != null)
			this.layout.setPattern (eventPattern);
		this.layout.start ();
	}
	
	protected void doGet (final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		synchronized (this.appender.monitor) {
			this.appender.drainEvents ();
			response.setHeader ("Content-Type", "text/html");
			final PrintWriter stream = response.getWriter ();
			stream.write (this.layout.getFileHeader ());
			stream.write (this.layout.getPresentationHeader ());
			for (final ILoggingEvent event : this.appender.getEvents ())
				stream.write (this.layout.doLayout (event));
			stream.write (this.layout.getPresentationFooter ());
			stream.write (this.layout.getFileFooter ());
			stream.close ();
		}
	}
	
	private EventViewerAppender appender;
	private HTMLLayout layout;
	private Logger rootLogger;
	
	public static final String appenderParameterName = "appender";
	public static final String eventPatternParameterName = "event-pattern";
	private static final long serialVersionUID = 1L;
}
