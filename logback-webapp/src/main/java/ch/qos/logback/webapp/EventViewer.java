
package ch.qos.logback.webapp;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;


public class EventViewer
		extends HttpServlet
{
	public EventViewer ()
	{
		super ();
		this.events = new LinkedList<ILoggingEvent> ();
		this.rootLogger = (Logger) LoggerFactory.getLogger (org.slf4j.Logger.ROOT_LOGGER_NAME);
		final LoggerContext context = this.rootLogger.getLoggerContext ();
		this.appender = new Appender (this.events);
		this.appender.setContext (context);
		this.appender.start ();
		this.rootLogger.addAppender (this.appender);
		this.layout = new HTMLLayout ();
		this.layout.setContext (context);
		this.layout.start ();
	}
	
	protected void doGet (final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		this.appender.drain ();
		response.setHeader ("Content-Type", "text/html");
		final PrintWriter stream = response.getWriter ();
		stream.write (this.layout.getFileHeader ());
		stream.write (this.layout.getPresentationHeader ());
		for (final ILoggingEvent event : this.events)
			stream.write (this.layout.doLayout (event));
		stream.write (this.layout.getPresentationFooter ());
		stream.write (this.layout.getFileFooter ());
		stream.close ();
	}
	
	private final Appender appender;
	private final LinkedList<ILoggingEvent> events;
	private final HTMLLayout layout;
	private final Logger rootLogger;
	
	private static final long serialVersionUID = 1L;
	
	public static class Appender
			extends AppenderBase<ILoggingEvent>
	{
		public Appender (final LinkedList<ILoggingEvent> events)
		{
			super ();
			this.events = events;
			this.buffer = new LinkedBlockingQueue<ILoggingEvent> ();
		}
		
		public void drain ()
		{
			while (true) {
				final ILoggingEvent event = this.buffer.poll ();
				if (event == null)
					break;
				this.events.add (event);
			}
		}
		
		protected void append (final ILoggingEvent event)
		{
			this.buffer.add (event);
		}
		
		private final LinkedBlockingQueue<ILoggingEvent> buffer;
		private final LinkedList<ILoggingEvent> events;
	}
}
