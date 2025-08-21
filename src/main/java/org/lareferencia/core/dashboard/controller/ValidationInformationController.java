
/*******************************************************************************
 * Copyright (c) 2013, 2020 LA Referencia / Red CLARA and others
 *
 * This	@ApiOperation(value = "Returns invalid metadata occurrences/count for a given validation rule by {harvestingID} and {ruleID}")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns invalid metadata occurrences/count for a given validation rule by {harvestingID} and {ruleID}") })  
    @RequestMapping(value = "/{sourceAcronym}/{harvestingID}/invalid_occrs/{ruleID}", method = RequestMethod.GET)
    HttpEntity< List<ValueCount> > invalidOccurrenceCountByHarvestingIDAndRuleID(
    		@ApiParam(value = "Source acronym", required = true) @PathVariable("sourceAcronym") String sourceAcronym, 
    		@ApiParam(value = "Harvesting ID", required = true, example = "1") @PathVariable("harvestingID") Long harvestingID, 
    		@ApiParam(value = "Rule ID", required = true, example = "1") @PathVariable("ruleID") Long ruleID) {e is part of LRHarvester v4.x software
 *
 *  This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *     
 *     For any further information please contact
 *     Lautaro Matas <lmatas@gmail.com>
 *******************************************************************************/
package org.lareferencia.core.dashboard.controller;

import org.lareferencia.core.dashboard.service.IRecordValidationResult;
import org.lareferencia.backend.services.validation.IValidationStatisticsService;
import org.lareferencia.backend.domain.validation.ValidationStatsQueryResult;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.lareferencia.core.dashboard.service.IValidationInformationService;
import org.lareferencia.core.dashboard.service.IValidationResult;
import org.lareferencia.core.dashboard.service.ValidationInformationServiceException;
import org.lareferencia.core.dashboard.service.ValueCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(value = "Validation Information", tags="Validation")
@RequestMapping("/api/v2/validation/source")
@CrossOrigin
public class ValidationInformationController {
	
	@Autowired
	IValidationInformationService vService;

	@Autowired
	IValidationStatisticsService validationService;

	
	@ApiOperation(value = "Returns validation results info by harvesting id")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns validation results info by harvesting id") })  
    @RequestMapping(value = "/{sourceAcronym}/{harvestingID}", method = RequestMethod.GET)
    HttpEntity<IValidationResult> getValidationResults(
    		@ApiParam(value = "Source acronym", required = true) @PathVariable("sourceAcronym") String sourceAcronym, 
    		@ApiParam(value = "Harvesting ID", required = true, example = "1") @PathVariable("harvestingID") Long harvestingID) {

		IValidationResult result = null;
		
		try {
		
			result = vService.validationResultByHarvestingID(sourceAcronym, harvestingID);
	        return new ResponseEntity<IValidationResult>(result, HttpStatus.OK);

		} catch (Exception e) {
	        return new ResponseEntity<IValidationResult>(result, HttpStatus.NOT_FOUND);
		}
    	
    }

	@ApiOperation(value = "Query validation statistics using core-lib API (direct access)")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns validation statistics query result") })  
    @RequestMapping(value = "/stats/{networkAcronym}/{harvestingID}/query", method = RequestMethod.GET)
    HttpEntity<ValidationStatsQueryResult> queryValidationStats(
    		@ApiParam(value = "Network acronym", required = true) @PathVariable("networkAcronym") String networkAcronym, 
    		@ApiParam(value = "Harvesting ID", required = true, example = "1") @PathVariable("harvestingID") Long harvestingID,
    		@ApiParam(value = "Query filters") @RequestParam(value = "filters", required = false) List<String> filters,
    		@ApiParam(value = "Page number", example = "0") @RequestParam(value = "pageNumber", required = false, defaultValue = "0") Integer page,
    		@ApiParam(value = "Page size", example = "20") @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer size) {

		try {
			
			// Preparar filtros
			List<String> queryFilters = filters != null ? filters : new ArrayList<>();
			
			// Consultar observaciones usando la nueva API
			ValidationStatsQueryResult result = validationService.queryValidationStatsObservationsBySnapshotID(
				harvestingID, queryFilters, PageRequest.of(page, size));
				
			return new ResponseEntity<>(result, HttpStatus.OK);
			
		} catch (Exception e) {
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
	@ApiOperation(value = "Returns validation results on each record by harvesting id")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns validation results on each record by harvesting id") })  
    @RequestMapping(value = "/{sourceAcronym}/{harvestingID}/records", method = RequestMethod.GET)
    HttpEntity< Page<IRecordValidationResult> > getRecordValitationResult(
    		@ApiParam(value = "Source acronym", required = true) @PathVariable("sourceAcronym") String sourceAcronym, 
    		@ApiParam(value = "Harvesting ID", required = true, example = "1") @PathVariable("harvestingID") Long harvestingID, 
    		@ApiParam(value = "Filter by validity status", required = false) @RequestParam(value = "is_valid", required = false) Boolean isValid, 
    		@ApiParam(value = "Filter by transformation status", required = false) @RequestParam(value = "is_transformed", required = false) Boolean isTransformed, 
    		@ApiParam(value = "Filter by valid rules IDs", required = false) @RequestParam(value = "valid_rules", required = false) List<String> validRules, 
     		@ApiParam(value = "Filter by invalid rules IDs", required = false) @RequestParam(value = "invalid_rules", required = false) List<String> invalidRules, 
    		@ApiParam(value = "Filter by OAI identifier", required = false) @RequestParam(value = "oai_identifier", required = false) String oaiIdentifier,
    		@ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "pageNumber", required = false, defaultValue = "0") Integer page,
    		@ApiParam(value = "Page size", required = false) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer size

    		) {
		
		Page<IRecordValidationResult> result = null;
		
		try {
			result = vService.recordValidationResultsByHarvestingID(sourceAcronym, harvestingID, 
					Optional.ofNullable(isValid), 
					Optional.ofNullable(isTransformed), 
					Optional.ofNullable(validRules), 
					Optional.ofNullable(invalidRules), 
					Optional.ofNullable(oaiIdentifier), 
					PageRequest.of(page, size));
			return new ResponseEntity< Page<IRecordValidationResult> >(result, HttpStatus.OK);
		} catch (Exception e) {
	        return new ResponseEntity< Page<IRecordValidationResult> >(result, HttpStatus.NOT_FOUND);
		}
    }
    
	@ApiOperation(value = "Returns valid metadata occurrences/count for a given validation rule by {harvestingID} and {ruleID}")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns valid metadata occurrences/count for a given validation rule by {harvestingID} and {ruleID}") })  
    @RequestMapping(value = "/{sourceAcronym}/{harvestingID}/valid_occrs/{ruleID}", method = RequestMethod.GET)
    HttpEntity< List<ValueCount> > validOccurrenceCountByHarvestingIDAndRuleID(
    		@ApiParam(value = "Source acronym", required = true) @PathVariable("sourceAcronym") String sourceAcronym, 
    		@ApiParam(value = "Harvesting ID", required = true, example = "1") @PathVariable("harvestingID") Long harvestingID, 
    		@ApiParam(value = "Rule ID", required = true, example = "1") @PathVariable("ruleID") Long ruleID) {

		List<ValueCount> result = null;
				
		try {
			result = vService.validOccurrenceCountByHarvestingIDAndRuleID(sourceAcronym, harvestingID, ruleID);
			return new ResponseEntity< List<ValueCount> >(result, HttpStatus.OK);
		
		} catch (Exception e) {
			return new ResponseEntity< List<ValueCount> >(result, HttpStatus.NOT_FOUND);
		}
	}
    
	
	@ApiOperation(value = "Returns invalid metadata occurrences/count for a given validation rule by {harvestingID} and {ruleID}")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns invalid metadata occurrences/count for a given validation rule by {harvestingID} and {ruleID}") })  
    @RequestMapping(value = "/{sourceAcronym}/{harvestingID}/invalid_occrs/{ruleID}", method = RequestMethod.GET)
    HttpEntity< List<ValueCount> > invalidOccurrenceCountByHarvestingIDAndRuleID(@PathVariable("sourceAcronym") String sourceAcronym, @PathVariable("harvestingID")  Long harvestingID, @PathVariable("ruleID")  Long ruleID) throws ValidationInformationServiceException {

		List<ValueCount> result = null;
		
		try {
			result = vService.invalidOccurrenceCountByHarvestingIDAndRuleID(sourceAcronym, harvestingID, ruleID);
			return new ResponseEntity< List<ValueCount> >(result, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity< List<ValueCount> >(result, HttpStatus.NOT_FOUND);
		}
    }
    
}
