/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.slee.resource.statistics;

import java.util.concurrent.TimeUnit;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.*;

import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.management.ResourceManagement;
import org.restcomm.commons.statistics.reporter.RestcommStatsReporter;

public class StatisticsResourceAdaptor implements ResourceAdaptor
{
	private static final String TIMER_INTERVAL_IN_SECONDS = "org.restcomm.slee.resource.statistics.TIMER_INTERVAL_IN_SECONDS";

	private transient Tracer tracer;

	private ResourceAdaptorContext raContext;
	private SleeContainer sleeContainer;
	private ResourceManagement resourceManagement;

	private Integer timerInterval;
	private StatisticsTimerTask timerTask;

	public StatisticsResourceAdaptor()
	{
	}

	public ResourceAdaptorContext getResourceAdaptorContext()
	{
		return raContext;
	}

	// Restcomm Statistics
	protected static final String STATISTICS_SERVER = "statistics.server";
	protected static final String DEFAULT_STATISTICS_SERVER = "https://statistics.restcomm.com/rest/";

	private RestcommStatsReporter statsReporter = new RestcommStatsReporter();
	private CountersFacility countersFacility = new CountersFacility();
	// lifecycle methods

	public void setResourceAdaptorContext(ResourceAdaptorContext raContext)
	{
		this.raContext = raContext;
		this.tracer = raContext.getTracer(StatisticsResourceAdaptor.class.getSimpleName());

		this.sleeContainer = SleeContainer.lookupFromJndi();
		if (this.sleeContainer != null)
		{
			this.resourceManagement = sleeContainer.getResourceManagement();
		}
	}

	public void raConfigure(ConfigProperties properties)
	{
		if (tracer.isFineEnabled())
		{
			tracer.fine("Configuring RA.");
		}

		this.timerInterval = (Integer) properties.getProperty(TIMER_INTERVAL_IN_SECONDS).getValue();
	}

	public void raActive()
	{
		if (statsReporter == null)
		{
			statsReporter = new RestcommStatsReporter();
		}

		String statisticsServer = Version.getVersionProperty(STATISTICS_SERVER);
		if (statisticsServer == null || !statisticsServer.contains("http"))
		{
			statisticsServer = DEFAULT_STATISTICS_SERVER;
		}

		if (tracer.isFineEnabled())
		{
			tracer.fine("statisticsServer: " + statisticsServer);
		}

		// define remote server address (optionally)
		statsReporter.setRemoteServer(statisticsServer);
		String projectName = System.getProperty("RestcommProjectName", "jainslee");
		String projectType = System.getProperty("RestcommProjectType", "community");
		String projectVersion = System.getProperty("RestcommProjectVersion", Version.getVersionProperty(Version.RELEASE_VERSION));

		if (tracer.isFineEnabled())
		{
			tracer.fine("Restcomm Stats " + projectName + " " + projectType + " " + projectVersion);
		}

		statsReporter.setProjectName(projectName);
		statsReporter.setProjectType(projectType);
		statsReporter.setVersion(projectVersion);

		Version.printVersion();

		if (resourceManagement != null)
		{
			timerTask = new StatisticsTimerTask(resourceManagement, tracer, statsReporter, countersFacility);
			raContext.getTimer().schedule(timerTask, 0, TimeUnit.MILLISECONDS.convert(timerInterval, TimeUnit.SECONDS));
		}
	}

	public void raStopping()
	{
	}

	public void raInactive()
	{
		statsReporter.stop();
		statsReporter = null;
	}

	public void raUnconfigure()
	{
		this.timerInterval = null;
		if (this.timerTask != null)
		{
			this.timerTask.cancel();
			this.timerTask = null;
		}
	}

	public void unsetResourceAdaptorContext()
	{
		raContext = null;
		tracer = null;

		sleeContainer = null;
		resourceManagement = null;
	}

	// config management methods
	public void raVerifyConfiguration(ConfigProperties properties) throws javax.slee.resource.InvalidConfigurationException
	{
	}

	public void raConfigurationUpdate(ConfigProperties properties)
	{
		throw new UnsupportedOperationException();
	}

	// event filtering methods
	public void serviceActive(ReceivableService service)
	{
	}

	public void serviceStopping(ReceivableService service)
	{
	}

	public void serviceInactive(ReceivableService service)
	{
	}

	// mandatory callbacks
	public void administrativeRemove(ActivityHandle handle)
	{
	}

	public Object getActivity(ActivityHandle activityHandle)
	{
		return null;
	}

	public ActivityHandle getActivityHandle(Object activity)
	{
		return null;
	}

	// optional call-backs
	public void activityEnded(ActivityHandle handle)
	{
	}

	public void activityUnreferenced(ActivityHandle activityHandle)
	{
	}

	public void eventProcessingFailed(ActivityHandle arg0, FireableEventType arg1, Object arg2, Address arg3, ReceivableService arg4, int arg5, FailureReason arg6)
	{
	}

	public void eventProcessingSuccessful(ActivityHandle arg0, FireableEventType arg1, Object arg2, Address arg3, ReceivableService arg4, int arg5)
	{
	}

	public void eventUnreferenced(ActivityHandle arg0, FireableEventType arg1, Object event, Address arg3, ReceivableService arg4, int arg5)
	{
	}

	public void queryLiveness(ActivityHandle activityHandle)
	{
	}

	// interface accessors
	public Object getResourceAdaptorInterface(String arg0)
	{
		return null;
	}

	public Marshaler getMarshaler()
	{
		return null;
	}

	// ra logic
}
