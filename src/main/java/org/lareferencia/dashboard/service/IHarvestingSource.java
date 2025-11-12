
package org.lareferencia.dashboard.service;

import java.util.Map;


public interface IHarvestingSource {
	
	Long getId();
	String getName();
	String getAcronym();
	String getInstitutionName();
	String getInstitutionAcronym();
	Map<String, Object> getAttributes();
	Boolean isPublic();

}
