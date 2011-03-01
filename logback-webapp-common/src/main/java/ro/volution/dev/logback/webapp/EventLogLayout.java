
package ro.volution.dev.logback.webapp;


import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.helpers.Transform;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ContextAwareBase;


public class EventLogLayout
		extends ContextAwareBase
		implements
			Layout<ILoggingEvent>
{
	public EventLogLayout ()
	{
		super ();
		this.pattern = EventLogLayout.defaultPattern;
	}
	
	public String doHeaderLayout ()
	{
		final StringBuilder buffer = new StringBuilder ();
		this.doHeaderLayout (buffer);
		return buffer.toString ();
	}
	
	public String doLayout (final ILoggingEvent event)
	{
		final StringBuilder buffer = new StringBuilder ();
		this.doLayout (event, buffer);
		return buffer.toString ();
	}
	
	public String getContentType ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public String getFileFooter ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public String getFileHeader ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public String getPattern ()
	{
		return (this.pattern);
	}
	
	public String getPresentationFooter ()
	{
		return ("</table>\n");
	}
	
	public String getPresentationHeader ()
	{
		return ("<table class=\"EventLog\">\n");
	}
	
	public boolean isStarted ()
	{
		return (this.started);
	}
	
	public void setPattern (final String pattern)
	{
		this.pattern = pattern;
	}
	
	public void start ()
	{
		this.started = true;
		this.buildConverters ();
	}
	
	public void stop ()
	{
		this.started = false;
	}
	
	protected void buildConverters ()
	{
		this.convertersHead = null;
		this.convertersCount = 0;
		final Converter<ILoggingEvent> convertersHead;
		try {
			final HashMap<String, String> converterMap = new HashMap<String, String> ();
			{
				final Map<String, String> map = PatternLayout.defaultConverterMap;
				if (map != null)
					converterMap.putAll (map);
			}
			{
				@SuppressWarnings ({"unchecked", "rawtypes"}) final Map<String, String> map =
						(Map) this.context.getObject (CoreConstants.PATTERN_RULE_REGISTRY);
				if (map != null)
					converterMap.putAll (map);
			}
			final Parser<ILoggingEvent> parser = new Parser<ILoggingEvent> (this.pattern);
			parser.setContext (this.context);
			final Node parserNodesHead = parser.parse ();
			convertersHead = parser.compile (parserNodesHead, converterMap);
			ConverterUtil.startConverters (convertersHead);
		} catch (final Throwable exception) {
			this.addError (
					String.format (
							"logback event viewer layout encountered an error while parsing the pattern `%s`", this.pattern),
					exception);
			return;
		}
		this.convertersHead = convertersHead;
		{
			this.convertersCount = 0;
			Converter<ILoggingEvent> converter = this.convertersHead;
			while (converter != null) {
				this.convertersCount++;
				converter = converter.getNext ();
			}
		}
	}
	
	protected void doHeaderLayout (final StringBuilder buffer)
	{
		buffer.append ("<tr class=\"EventHeader\">\n");
		Converter<ILoggingEvent> converter = this.convertersHead;
		while (converter != null) {
			final String converterName = this.getConverterName (converter);
			buffer.append ("<th class=\"EventHeader " + converterName.replace ("\"", "\\\"") + "\">");
			buffer.append (Transform.escapeTags (converterName));
			buffer.append ("</th>\n");
			converter = converter.getNext ();
		}
		buffer.append ("</tr>\n");
	}
	
	protected void doLayout (final ILoggingEvent event, final StringBuilder buffer)
	{
		buffer.append ("<tr class=\"Event " + event.getLevel ().levelStr + "\">\n");
		Converter<ILoggingEvent> converter = this.convertersHead;
		while (converter != null) {
			buffer.append ("<td class=\"Event " + this.getConverterName (converter).replace ("\"", "\\\"") + "\">");
			converter.write (buffer, event);
			buffer.append ("</td>\n");
			converter = converter.getNext ();
		}
		buffer.append ("</tr>\n");
		if (event.getThrowableProxy () != null)
			this.doLayout (event.getThrowableProxy (), buffer);
	}
	
	protected void doLayout (final IThrowableProxy throwableHead, final StringBuilder buffer)
	{
		buffer.append ("<tr class=\"Exceptions\">\n");
		buffer.append ("<td class=\"Exceptions\" colspan=\"").append (this.convertersCount).append ("\">\n");
		buffer.append ("<ul class=\"Exceptions\">\n");
		IThrowableProxy throwable = throwableHead;
		while (throwable != null) {
			buffer.append ("<li class=\"Exception\">\n");
			buffer.append ("<p class=\"Exception_Header\">\n");
			buffer
					.append ("<span class=\"Exception_Class\">").append (Transform.escapeTags (throwable.getClassName ()))
					.append ("</span>");
			buffer.append ("<span class=\"Exception_ClassMessageSplit\">&nbsp;::&nbsp;</span>");
			buffer
					.append ("<span class=\"Exception_Message\">").append (Transform.escapeTags (throwable.getMessage ()))
					.append ("</span>");
			buffer.append ("</p>\n");
			final StackTraceElementProxy[] frames = throwable.getStackTraceElementProxyArray ();
			final int commonFramesCount = throwable.getCommonFrames ();
			final int limitFrameIndex = frames.length - commonFramesCount;
			buffer.append ("<ul class=\"Exception_StackFrames\">\n");
			for (int frameIndex = 0; frameIndex < limitFrameIndex; frameIndex++) {
				final StackTraceElement frame = frames[frameIndex].getStackTraceElement ();
				buffer.append ("<li class=\"Exception_StackFrame\">");
				buffer
						.append ("<span class=\"Exception_StackFrame_Class\">")
						.append (Transform.escapeTags (frame.getClassName ())).append ("</span>");
				buffer.append ("<span class=\"Exception_StackFrame_ClassMethodSplit\">&nbsp;::&nbsp;</span>");
				buffer
						.append ("<span class=\"Exception_StackFrame_Method\">")
						.append (Transform.escapeTags (frame.getMethodName ())).append ("</span>");
				buffer.append ("<span class=\"Exception_StackFrame_MethodLineSplit\">&nbsp;::&nbsp;</span>");
				buffer
						.append ("<span class=\"Exception_StackFrame_Method\">").append (frame.getLineNumber ())
						.append ("</span>");
				buffer.append ("</li>\n");
			}
			if (commonFramesCount > 0)
				buffer
						.append ("<li class=\"Exception_StackFrame_Common\"><span class=\"Exception_StackFrame_Common\">...</span></li>\n");
			buffer.append ("</ul>\n");
			buffer.append ("</li>\n");
			throwable = throwable.getCause ();
		}
		buffer.append ("</ul>\n");
		buffer.append ("</td>\n");
		buffer.append ("</tr>\n");
	}
	
	protected String getConverterName (final Converter<ILoggingEvent> converter)
	{
		final String name;
		if (converter instanceof MDCConverter) {
			final MDCConverter mdcConverter = (MDCConverter) converter;
			final String key = mdcConverter.getFirstOption ();
			name = key != null ? "MDC_" + key : "MDC";
		} else {
			final String converterClassName = converter.getClass ().getSimpleName ();
			if (converterClassName.endsWith ("Converter"))
				name = converterClassName.substring (0, converterClassName.lastIndexOf ("Converter"));
			else
				name = "unknown";
		}
		return (name);
	}
	
	protected int convertersCount;
	protected Converter<ILoggingEvent> convertersHead;
	protected String pattern;
	protected boolean started;
	
	public static final String defaultPattern = "%date%level%logger%msg%mdc";
}
