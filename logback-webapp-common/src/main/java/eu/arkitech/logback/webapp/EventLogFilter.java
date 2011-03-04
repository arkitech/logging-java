
package eu.arkitech.logback.webapp;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;


class EventLogFilter
		extends Filter<ILoggingEvent>
{
	public EventLogFilter (final HttpServletRequest request)
	{
		super ();
		this.levelValue = Level.toLevel (request.getParameter ("level"), null);
		Map<String, String> mdcValues = null;
		@SuppressWarnings ("unchecked") final Enumeration<String> parameterNames = request.getParameterNames ();
		while (parameterNames.hasMoreElements ()) {
			final String parameterName = parameterNames.nextElement ();
			if (!parameterName.startsWith ("mdc."))
				continue;
			final String mdcValue = request.getParameter (parameterName);
			if ((mdcValue == null) || mdcValue.isEmpty ())
				continue;
			final String mdcKey = parameterName.substring ("mdc.".length ());
			if (mdcValues == null)
				mdcValues = new HashMap<String, String> ();
			mdcValues.put (mdcKey, mdcValue);
		}
		this.mdcValues = mdcValues;
		final String mdcStrictValue = request.getParameter ("mdc_strict");
		if ((mdcStrictValue != null) && mdcStrictValue.equals ("on"))
			this.mdcStrict = true;
		else
			this.mdcStrict = false;
	}
	
	public FilterReply decide (final ILoggingEvent event)
	{
		if ((this.levelValue != null) && !event.getLevel ().isGreaterOrEqual (this.levelValue))
			return (FilterReply.DENY);
		if (this.mdcValues != null) {
			final Map<String, String> eventMdcValues = event.getMdc ();
			if (eventMdcValues == null) {
				if (this.mdcStrict)
					return (FilterReply.DENY);
				else
					return (FilterReply.NEUTRAL);
			}
			for (final String mdcKey : this.mdcValues.keySet ()) {
				final String eventMdcValue = eventMdcValues.get (mdcKey);
				if (eventMdcValue == null) {
					if (this.mdcStrict)
						return (FilterReply.DENY);
				} else if (!eventMdcValue.equals (this.mdcValues.get (mdcKey)))
					return (FilterReply.DENY);
			}
		}
		return (FilterReply.ACCEPT);
	}
	
	protected final Level levelValue;
	protected final boolean mdcStrict;
	protected final Map<String, String> mdcValues;
}
