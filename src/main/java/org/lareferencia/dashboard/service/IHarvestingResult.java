
package org.lareferencia.dashboard.service;

import java.time.LocalDateTime;



public interface IHarvestingResult {
	
	Long getId();
	
	LocalDateTime getStartTime();
	LocalDateTime getEndTime();
	
	String getStatus();

	Integer getHarvestedSize();
	Integer getValidSize();
	Integer getTransformedSize();
	
	Boolean isDeleted();

}
