package com.x.processplatform.assemble.designer;

import com.x.base.core.project.Context;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.message.MessageConnector;
import com.x.processplatform.assemble.designer.schedule.CleanElementVersion;

public class ThisApplication {

	protected static Context context;

	public static ProjectionExecuteQueue projectionExecuteQueue = new ProjectionExecuteQueue();
	public static MappingExecuteQueue mappingExecuteQueue = new MappingExecuteQueue();

	public static Context context() {
		return context;
	}

	public static void init() {
		try {
			LoggerFactory.setLevel(Config.logLevel().x_processplatform_assemble_designer());
			MessageConnector.start(context());
			projectionExecuteQueue.start();
			mappingExecuteQueue.start();
			context.schedule(CleanElementVersion.class, "20 20 5 * * ?");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void destroy() {
		try {
			projectionExecuteQueue.stop();
			mappingExecuteQueue.stop();
			MessageConnector.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}