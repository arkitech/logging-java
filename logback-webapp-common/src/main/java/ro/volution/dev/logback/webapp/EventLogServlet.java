
package ro.volution.dev.logback.webapp;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;


public class EventLogServlet
		extends HttpServlet
{
	public EventLogServlet ()
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
		final String appenderName = configuration.getInitParameter (EventLogServlet.appenderParameterName);
		if ((appenderName == null) || appenderName.isEmpty ())
			throw (new ServletException (String.format (
					"logback event viewer `%s` parameter is not set; aborting!", EventLogServlet.appenderParameterName)));
		final String eventPattern = configuration.getInitParameter (EventLogServlet.eventPatternParameterName);
		final String htmlHeadResourceName = configuration.getInitParameter (EventLogServlet.htmlHeadResourceParameterName);
		
		this.context = (LoggerContext) LoggerFactory.getILoggerFactory ();
		
		final Object appender = this.context.getObject (appenderName);
		if (appender == null)
			throw (new ServletException (String.format (
					"logback event viewer `%s` parameter value `%s` is wrong (no appender of such name found)",
					EventLogServlet.appenderParameterName, appenderName)));
		if (!(appender instanceof EventLogAppender))
			throw (new ServletException (String.format (
					"logback event viewer `%s` parameter value `%s` is wrong (appender has wrong class `%s`)",
					EventLogServlet.appenderParameterName, appenderName, appender.getClass ().getName ())));
		
		final InputStream htmlHeadStream;
		if (htmlHeadResourceName != null) {
			htmlHeadStream = EventLogServlet.class.getClassLoader ().getResourceAsStream (htmlHeadResourceName);
			if (htmlHeadStream == null)
				throw (new ServletException (String.format (
						"logback event viewer `%s` parameter value `%s` is wrong (no resource of such name found)",
						EventLogServlet.htmlHeadResourceParameterName, htmlHeadResourceName)));
		} else
			htmlHeadStream =
					EventLogServlet.class.getClassLoader ().getResourceAsStream (EventLogServlet.defaultHtmlHeadResource);
		final StringBuilder htmlHead;
		if (htmlHeadStream != null) {
			htmlHead = new StringBuilder ();
			final BufferedReader cssReader = new BufferedReader (new InputStreamReader (htmlHeadStream));
			final char[] buffer = new char[1024];
			while (true) {
				final int read;
				try {
					read = cssReader.read (buffer);
				} catch (final IOException exception) {
					throw (new ServletException (exception));
				}
				if ((read == 0) || (read == -1))
					break;
				htmlHead.insert (htmlHead.length (), buffer, 0, read);
			}
			try {
				cssReader.close ();
			} catch (final IOException exception) {
				throw (new ServletException (exception));
			}
		} else
			htmlHead = null;
		
		super.init ();
		
		this.appender = (EventLogAppender) appender;
		
		this.layout = new EventLogLayout ();
		this.layout.setContext (this.context);
		if (eventPattern != null)
			this.layout.setPattern (eventPattern);
		this.layout.start ();
		
		if (htmlHead != null)
			this.htmlHead = htmlHead.toString ();
	}
	
	protected void doError (final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		response.setStatus (HttpServletResponse.SC_NOT_FOUND);
		response.setHeader ("Content-Type", "text/html");
		final PrintWriter stream = response.getWriter ();
		this.doPageHeader (request, response, stream);
		stream.write ("<p>invalid URI</p>\n");
		this.doPageFooter (request, response, stream);
		stream.close ();
	}
	
	protected void doEventLog (final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		response.setStatus (HttpServletResponse.SC_OK);
		response.setHeader ("Content-Type", "text/html");
		final PrintWriter stream = response.getWriter ();
		this.doPageHeader (request, response, stream);
		final EventLogFilter filter = new EventLogFilter (request);
		stream.write (this.layout.getPresentationHeader ());
		stream.write (this.layout.doHeaderLayout ());
		for (final ILoggingEvent event : this.appender)
			if (filter.decide (event) != FilterReply.DENY)
				stream.write (this.layout.doLayout (event));
		stream.write (this.layout.getPresentationFooter ());
		this.doPageFooter (request, response, stream);
		stream.close ();
	}
	
	protected void doGet (final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		final String path = request.getPathInfo ();
		if ((path == null) || path.equals ("/"))
			this.doMain (request, response);
		else if (path.equals ("/event-log"))
			this.doEventLog (request, response);
		else
			this.doError (request, response);
	}
	
	protected void doMain (final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		final String requestUri = request.getRequestURI ();
		final String query = request.getQueryString ();
		if (!requestUri.endsWith ("/")) {
			response.setStatus (HttpServletResponse.SC_MOVED_TEMPORARILY);
			response.setHeader ("Location", requestUri + "/" + ((query != null) ? query : ""));
			response.getOutputStream ().close ();
			return;
		}
		response.setStatus (HttpServletResponse.SC_OK);
		response.setHeader ("Content-Type", "text/html");
		final PrintWriter stream = response.getWriter ();
		this.doPageHeader (request, response, stream);
		stream.write ("<iframe class=\"EventLog\" src=\"./event-log");
		if ((query != null) && !query.isEmpty ()) {
			stream.write ("?");
			stream.write (query);
		}
		stream.write ("\" frameborder=\"0\" scrolling=\"auto\" />");
		this.doPageFooter (request, response, stream);
		stream.close ();
	}
	
	protected void doPageFooter (
			@SuppressWarnings ("unused") final HttpServletRequest request,
			@SuppressWarnings ("unused") final HttpServletResponse response, final PrintWriter stream)
	{
		stream.write ("</body>\n");
		stream.write ("</html>\n");
	}
	
	protected void doPageHeader (
			@SuppressWarnings ("unused") final HttpServletRequest request,
			@SuppressWarnings ("unused") final HttpServletResponse response, final PrintWriter stream)
	{
		stream.write ("<html>\n");
		stream.write ("<head>\n");
		if (this.htmlHead != null)
			stream.write (this.htmlHead);
		stream.write ("</head>\n");
		stream.write ("<body>\n");
	}
	
	private EventLogAppender appender;
	private LoggerContext context;
	private String htmlHead;
	private EventLogLayout layout;
	
	public static final String appenderParameterName = "appender";
	public static final String defaultHtmlHeadResource = "ro/volution/dev/logback/webapp/event-log.html-head";
	public static final String eventPatternParameterName = "event-pattern";
	public static final String htmlHeadResourceParameterName = "html-head-resource";
	private static final long serialVersionUID = 1L;
}
