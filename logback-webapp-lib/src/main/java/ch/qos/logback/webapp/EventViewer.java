
package ch.qos.logback.webapp;


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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.html.CssBuilder;
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
		final String htmlHeadResourceName = configuration.getInitParameter (EventViewer.htmlHeadResourceParameterName);
		
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
		
		final InputStream htmlHeadStream;
		if (htmlHeadResourceName != null) {
			htmlHeadStream = EventViewer.class.getClassLoader ().getResourceAsStream (htmlHeadResourceName);
			if (htmlHeadStream == null)
				throw (new ServletException (String.format (
						"logback event viewer `%s` parameter value `%s` is wrong (no resource of such name found)",
						EventViewer.htmlHeadResourceParameterName, htmlHeadResourceName)));
		} else
			htmlHeadStream = EventViewer.class.getClassLoader ().getResourceAsStream (EventViewer.defaultHtmlHeadResource);
		final StringBuffer htmlHead;
		if (htmlHeadStream != null) {
			htmlHead = new StringBuffer ();
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
		
		this.appender = (EventViewerAppender) appender;
		
		this.layout = new HTMLLayout ();
		this.layout.setContext (context);
		if (eventPattern != null)
			this.layout.setPattern (eventPattern);
		if (htmlHead != null) {
			this.layout.setCssBuilder (new CssBuilder () {
				public void addCss (final StringBuilder sink)
				{
					sink.append (htmlHead);
				}
			});
		}
		this.layout.start ();
		
		new Thread () {
			public void run () {
				final Logger logger = (Logger) LoggerFactory.getLogger (this.getClass ().getName ());
				int index = 0;
				while (true) {
					logger.info (Integer.toString (index));
					index++;
					try {
						Thread.sleep (500);
					} catch (final InterruptedException exception) {
						break;
					}
				}
			}
		}.start ();
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
	public static final String htmlHeadResourceParameterName = "html-head-resource";
	public static final String defaultHtmlHeadResource = "logback-event-viewer.html-head";
	public static final String eventPatternParameterName = "event-pattern";
	private static final long serialVersionUID = 1L;
}
