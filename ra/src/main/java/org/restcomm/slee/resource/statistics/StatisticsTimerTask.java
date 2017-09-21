package org.restcomm.slee.resource.statistics;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import javax.management.ObjectName;
import javax.slee.InvalidArgumentException;
import javax.slee.facilities.Tracer;

import org.jboss.mx.util.MBeanServerLocator;
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
		long totalUpdateCount = 0L;
		for (String raEntity : resourceManagement.getResourceAdaptorEntities())
		{
			if (tracer.isFineEnabled())
			{
				tracer.fine("RA Entity: " + raEntity);
			}

			try
			{
				ObjectName usageMBeanName = null;
				try
				{
					usageMBeanName = resourceManagement.getResourceUsageMBean(raEntity);
				}
				catch (InvalidArgumentException e)
				{
				}

				if (usageMBeanName != null)
				{
					// null for default set
					String usageParameterSetName = null; // "statisitcs";
					Object usageParameterSet = MBeanServerLocator.locateJBoss().invoke(usageMBeanName, "getInstalledUsageParameterSet", new Object[]
					{ usageParameterSetName }, new String[]
					{ String.class.getName() });

					long updateCount = updateCountersReturnCount(usageParameterSet);
					
					if (tracer.isFineEnabled())
					{
						tracer.fine("Total update count for " + usageParameterSet.getClass() + ":" + updateCount);
					}
					
					if (updateCount > 0)
					{
						totalUpdateCount += updateCount;
						resetCounters(usageParameterSet);
						
						if (tracer.isFineEnabled())
						{
							tracer.fine("Counters reset for " + usageParameterSet.getClass());
						}
					}
				}
			}
			catch (Exception e)
			{
				// ignoring ra-s that don'e have statistics
			}
		}

		if (totalUpdateCount > 0 && statsReporter != null)
		{
			statsReporter.report();
		}

		if (tracer.isFineEnabled())
		{
			tracer.fine(getClass() + " finished. Total update count:" + totalUpdateCount);
		}
	}

	public Long updateCountersReturnCount(Object usageParameterSet)
	{
		long totalCount = 0L;
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

				countersFacility.updateCounter(paramName, count);
				totalCount += count;
			}
		}

		return totalCount;
	}

	@SuppressWarnings("unchecked")
	private Set<String> fetchParameterNames(Object usageParameterSet)
	{
		Set<String> names = new HashSet<String>();
		try
		{
			Method method = usageParameterSet.getClass().getMethod("getAllParameters");
			Collection<String> parameterNames = (Collection<String>) method.invoke(usageParameterSet);
			if (parameterNames != null)
			{
				names.addAll(parameterNames);
			}
		}
		catch (Exception e)
		{
			if (tracer.isWarningEnabled())
			{
				tracer.warning("Can't get Usage parameter names for " + usageParameterSet.getClass(), e);
			}
		}
		return names;
	}

	private Long fetchParameterValue(Object usageParameterSet, String paramName)
	{
		Long paramValue = null;
		try
		{
			Method method = usageParameterSet.getClass().getMethod("getParameter", String.class, boolean.class);
			paramValue = (Long) method.invoke(usageParameterSet, paramName, false);
		}
		catch (Exception e)
		{
			if (tracer.isWarningEnabled())
			{
				tracer.warning("Can't get Usage parameter value for " + usageParameterSet.getClass(), e);
			}
		}
		return paramValue;
	}

	private void resetCounters(Object usageParameterSet)
	{
		try
		{
			Method resetMethod = usageParameterSet.getClass().getMethod("reset");
			resetMethod.invoke(usageParameterSet);
		}
		catch (Exception e)
		{
			if (tracer.isWarningEnabled())
			{
				tracer.warning("Reset counters failed for " + usageParameterSet.getClass(), e);
			}
		}
	}
}
