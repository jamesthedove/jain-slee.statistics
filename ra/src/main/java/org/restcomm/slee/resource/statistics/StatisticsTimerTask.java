package org.restcomm.slee.resource.statistics;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import javax.management.ObjectName;
import javax.slee.facilities.Tracer;

import org.mobicents.slee.container.management.ResourceManagement;
import org.restcomm.commons.statistics.reporter.RestcommStatsReporter;

public class StatisticsTimerTask extends TimerTask
{
	private ResourceManagement resourceManagement;
	private Tracer tracer;
	private RestcommStatsReporter statsReporter;
	private CountersFacility countersFacility;

	public StatisticsTimerTask(ResourceManagement resourceManagement, Tracer tracer, RestcommStatsReporter statsReporter, CountersFacility countersFacility)
	{
		this.resourceManagement = resourceManagement;
		this.tracer = tracer;
		this.statsReporter = statsReporter;
		this.countersFacility = countersFacility;
	}

	@Override
	public void run()
	{
		for (String raEntity : resourceManagement.getResourceAdaptorEntities())
		{
			if (tracer.isFineEnabled())
			{
				tracer.fine("RA Entity: " + raEntity);
			}

			Object usageParameterSet = null;
			try
			{
				ObjectName usageMBeanName = resourceManagement.getResourceUsageMBean(raEntity);

				// null for default set
				String usageParameterSetName = null; // "statisitcs";
				usageParameterSet = ManagementFactory.getPlatformMBeanServer().invoke(usageMBeanName, "getInstalledUsageParameterSet", new Object[]
				{ usageParameterSetName }, new String[]
				{ String.class.getName() });

			}
			catch (Exception e)
			{
				if (tracer.isWarningEnabled())
				{
					tracer.warning("Can't get Usage parameter set", e);
				}
			}

			if (usageParameterSet != null)
				updateCounters(usageParameterSet);

			// ManagementFactory.getPlatformMBeanServer()
			// .invoke(usageMBeanName, "resetAllUsageParameters",
			// null, null);

		} // end of for

		// TODO: check counters?
		// if (calls != 0 || messages != 0 || seconds != 0) {
		if (statsReporter != null)
		{
			tracer.info("Calling RestcommStatsReporter.report() ...");
			statsReporter.report();
		}
		// }
	}

	public void updateCounters(Object usageParameterSet)
	{
		Set<String> paramNames = fetchParameterNames(usageParameterSet);
		for (String paramName : paramNames)
		{
			Long count = fetchParameterValue(usageParameterSet, paramName);
			if (count != null && count > 0)
			{
				if (tracer.isFineEnabled())
				{
					tracer.fine(paramName + ":" + count);
				}
				tracer.info("Updating statistics for " + usageParameterSet.getClass().getSimpleName() + ",param:" + paramName + ",value:" + count);
				countersFacility.updateCounter(paramName, count);
			}
			else
			{
				tracer.info("Zero statistics for " + usageParameterSet.getClass().getSimpleName() + ",param:" + paramName);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Set<String> fetchParameterNames(Object usageParameterSet)
	{
		Set<String> parameterNames = null;
		try
		{
			Method method = usageParameterSet.getClass().getMethod("getParameterNames");
			parameterNames = (Set<String>) method.invoke(usageParameterSet);
		}
		catch (Exception e)
		{
			parameterNames = new HashSet<String>();
			if (tracer.isWarningEnabled())
			{
				tracer.warning("Can't get Usage parameter names", e);
			}
		}
		return parameterNames;
	}

	private Long fetchParameterValue(Object usageParameterSet, String paramName)
	{
		Long paramValue = null;
		try
		{
			Method method = usageParameterSet.getClass().getMethod("getParameter", String.class);

			// get and reset
			paramValue = (Long) method.invoke(usageParameterSet, paramName);
			if (paramValue != null && paramValue > 0)
			{
				if (tracer.isFineEnabled())
				{
					tracer.fine(paramName + ": " + paramValue);
				}
			}
		}
		catch (Exception e)
		{
			if (tracer.isWarningEnabled())
			{
				tracer.warning("Can't get Usage parameter value", e);
			}
		}
		return paramValue;
	}
}
