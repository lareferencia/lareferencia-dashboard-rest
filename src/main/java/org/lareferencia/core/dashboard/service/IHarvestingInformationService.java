
package org.lareferencia.core.dashboard.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public interface IHarvestingInformationService {
	
	Page<IHarvestingSource> listSources(Pageable pageable)  throws HarvesterInfoServiceException;
	
	Page<IHarvestingSource> listSources(List<String> whiteList, Pageable pageable)  throws HarvesterInfoServiceException;
	
	IHarvestingSource getSourceByID(Long sourceID)  throws HarvesterInfoServiceException;
	
	IHarvestingSource getSourceByAcronym(String acronym)  throws HarvesterInfoServiceException;
	
	IHarvestingResult getLastKnownGoodHarvestingBySourceID(Long sourceID) throws HarvesterInfoServiceException;
	
	IHarvestingResult getLastKnownGoodSHarvestingBySourceAcronym(String sourceAcronym) throws HarvesterInfoServiceException;
		
	Page<IHarvestingResult> getHarvestingHistoryBySourceID(Long sourceID, Pageable pageable) throws HarvesterInfoServiceException;
	
	Page<IHarvestingResult> getHarvestingHistoryBySourceAcronym(String sourceAcronym , Pageable pageable) throws HarvesterInfoServiceException;

	Page<IHarvestingResult> getHarvestingHistoryBySourceAcronym(String sourceAcronym , LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) throws HarvesterInfoServiceException;
 
    String getRecordMetadataByRecordIDAndSourceAcronym(String sourceAcronym, Long recordID) throws HarvesterInfoServiceException;

	String getRecordMetadataBySnapshotAndIdentifier(Long snapshotId, String identifier) throws HarvesterInfoServiceException;

	

}