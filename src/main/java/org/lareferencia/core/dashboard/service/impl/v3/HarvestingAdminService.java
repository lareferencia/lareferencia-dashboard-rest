
package org.lareferencia.core.dashboard.service.impl.v3;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lareferencia.core.dashboard.service.IHarvestingAdminService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class HarvestingAdminService implements IHarvestingAdminService {

	private static Logger logger = LogManager.getLogger(HarvestingAdminService.class);

	@Override
	public String dummy(String name) {
		return "Hello " + name;
	}
	
}
