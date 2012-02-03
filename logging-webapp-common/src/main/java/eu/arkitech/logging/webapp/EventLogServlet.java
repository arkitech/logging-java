/*
 * #%L
 * arkitech-logging-webapp-common
 * %%
 * Copyright (C) 2011 - 2012 Arkitech
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.arkitech.logging.webapp;


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
import com.google.common.base.Strings;
import eu.arkitech.logging.datastore.common.Datastore;
import eu.arkitech.logging.datastore.common.DatastoreAppender;
import eu.arkitech.logging.datastore.common.ImmutableDatastore;
import org.slf4j.LoggerFactory;


public class EventLogServlet
		extends HttpServlet
{
	public EventLogServlet ()
	{
		super ();
	}
	
	@Override
	public void destroy ()
	{
		super.destroy ();
		this.datastore = null;
		this.layout.stop ();
		this.layout = null;
	}
	
	@Override
	public void init (final ServletConfig configuration)
			throws ServletException
	{
		final String datastoreName = configuration.getInitParameter (EventLogServlet.datastoreParameterName);
		final String eventPattern = configuration.getInitParameter (EventLogServlet.eventPatternParameterName);
		final String htmlHeadResourceName = configuration.getInitParameter (EventLogServlet.htmlHeadResourceParameterName);
		this.context = (LoggerContext) LoggerFactory.getILoggerFactory ();
		if (Strings.isNullOrEmpty (datastoreName))
			throw (new ServletException (String.format ("logback event viewer `%s` parameter is not set; aborting!", EventLogServlet.datastoreParameterName)));
		final Object datastore_ = this.context.getObject (datastoreName);
		if (datastore_ == null)
			throw (new ServletException (String.format ("logback event viewer `%s` parameter value `%s` is wrong (no datastore of such name found)", EventLogServlet.datastoreParameterName, datastoreName)));
		final ImmutableDatastore datastore;
		if (datastore_ instanceof ImmutableDatastore)
			datastore = (ImmutableDatastore) datastore_;
		else if (datastore_ instanceof DatastoreAppender) {
			final Datastore datastore__ = ((DatastoreAppender) datastore_).getDatastore ();
			if ((datastore__ != null) && (datastore__ instanceof ImmutableDatastore))
				datastore = (ImmutableDatastore) datastore__;
			else
				throw (new ServletException (String.format ("logback event viewer `%s` parameter value `%s` is wrong (datastore has wrong class `%s`)", EventLogServlet.datastoreParameterName, datastoreName, datastore_.getClass ().getName ())));
		} else
			throw (new ServletException (String.format ("logback event viewer `%s` parameter value `%s` is wrong (datastore has wrong class `%s`)", EventLogServlet.datastoreParameterName, datastoreName, datastore_.getClass ().getName ())));
		final InputStream htmlHeadStream;
		if (!Strings.isNullOrEmpty (htmlHeadResourceName)) {
			htmlHeadStream = EventLogServlet.class.getClassLoader ().getResourceAsStream (htmlHeadResourceName);
			if (htmlHeadStream == null)
				throw (new ServletException (String.format ("logback event viewer `%s` parameter value `%s` is wrong (no resource of such name found)", EventLogServlet.htmlHeadResourceParameterName, htmlHeadResourceName)));
		} else
			htmlHeadStream = EventLogServlet.class.getClassLoader ().getResourceAsStream (EventLogServlet.defaultHtmlHeadResource);
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
		this.datastore = (ImmutableDatastore) datastore;
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
		for (final ILoggingEvent event : this.datastore.select (System.currentTimeMillis (), Long.MIN_VALUE, 200, filter))
			stream.write (this.layout.doLayout (event));
		stream.write (this.layout.getPresentationFooter ());
		this.doPageFooter (request, response, stream);
		stream.close ();
	}
	
	@Override
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
		final EventLogFilter filter = new EventLogFilter (request);
		final String level = ((filter.levelValue != null) ? filter.levelValue.levelStr.toLowerCase () : null);
		final String mdcApplication;
		final String mdcComponent;
		final String mdcNode;
		if (filter.mdcValues != null) {
			mdcApplication = filter.mdcValues.get ("application");
			mdcComponent = filter.mdcValues.get ("component");
			mdcNode = filter.mdcValues.get ("node");
		} else {
			mdcApplication = null;
			mdcComponent = null;
			mdcNode = null;
		}
		final boolean mdcStrict = filter.mdcStrict;
		stream.write ("<div class=\"EventFilter\">\n");
		stream.write ("<form action=\"\" method=\"GET\">\n");
		stream.write ("Level:&nbsp;<input type=\"text\" name=\"level\" value=\"" + ((level != null) ? level.replace ("\"", "\\\"") : "") + "\">\n");
		stream.write ("MDC.Application:&nbsp;<input type=\"text\" name=\"mdc.application\" value=\"" + ((mdcApplication != null) ? mdcApplication.replace ("\"", "\\\"") : "") + "\" />\n");
		stream.write ("MDC.Component:&nbsp;<input type=\"text\" name=\"mdc.component\" value=\"" + ((mdcComponent != null) ? mdcComponent.replace ("\"", "\\\"") : "") + "\">\n");
		stream.write ("MDC.Node:&nbsp;<input type=\"text\" name=\"mdc.node\" value=\"" + ((mdcNode != null) ? mdcNode.replace ("\"", "\\\"") : "") + "\">\n");
		stream.write ("MDC Strict:&nbsp;<input type=\"checkbox\" name=\"mdc_strict\" " + (mdcStrict ? " checked=\"on\"" : "") + " />\n");
		stream.write ("<input type=\"submit\" value=\"Refresh!\" />\n");
		stream.write ("</form>");
		stream.write ("</div>\n");
		stream.write ("<div class=\"EventLog\">\n");
		stream.write ("<iframe class=\"EventLog\" src=\"./event-log");
		if ((query != null) && !query.isEmpty ()) {
			stream.write ("?");
			stream.write (query);
		}
		stream.write ("\" frameborder=\"0\" scrolling=\"auto\" />");
		stream.write ("</div>\n");
		this.doPageFooter (request, response, stream);
		stream.close ();
	}
	
	protected void doPageFooter (@SuppressWarnings ("unused") final HttpServletRequest request, @SuppressWarnings ("unused") final HttpServletResponse response, final PrintWriter stream)
	{
		stream.write ("</body>\n");
		stream.write ("</html>\n");
	}
	
	protected void doPageHeader (@SuppressWarnings ("unused") final HttpServletRequest request, @SuppressWarnings ("unused") final HttpServletResponse response, final PrintWriter stream)
	{
		stream.write ("<html>\n");
		stream.write ("<head>\n");
		if (this.htmlHead != null)
			stream.write (this.htmlHead);
		stream.write ("</head>\n");
		stream.write ("<body>\n");
	}
	
	private LoggerContext context;
	private ImmutableDatastore datastore;
	private String htmlHead;
	private EventLogLayout layout;
	public static final String datastoreParameterName = "datastore";
	public static final String defaultHtmlHeadResource = "eu/arkitech/logback/webapp/event-log.html-head";
	public static final String eventPatternParameterName = "event-pattern";
	public static final String htmlHeadResourceParameterName = "html-head-resource";
	private static final long serialVersionUID = 1L;
}
