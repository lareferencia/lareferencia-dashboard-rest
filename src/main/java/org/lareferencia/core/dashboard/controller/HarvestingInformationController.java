
/*******************************************************************************
 * Copyright (c) 2013, 2020 LA Referencia / Red CLARA and others
 *
 * This file is part of LRHarvester v4.x software
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

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lareferencia.backend.validation.ValidationStatsObservationsResult;
import org.lareferencia.core.dashboard.security.ISecurityService;
import org.lareferencia.core.dashboard.service.HarvesterInfoServiceException;
import org.lareferencia.core.dashboard.service.IHarvestingInformationService;
import org.lareferencia.core.dashboard.service.IHarvestingResult;
import org.lareferencia.core.dashboard.service.IHarvestingSource;
import org.lareferencia.core.dashboard.service.impl.v3.HarvestingInformationService;
import org.lareferencia.core.util.date.DateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@Tag(name = "Harvesting", description = "Harvesting Information")
@RequestMapping("/api/v2/harvesting/source/")
@CrossOrigin
public class HarvestingInformationController {

	private static Logger logger = LogManager.getLogger(HarvestingInformationController.class);

	@Autowired
	IHarvestingInformationService hService;

	@Autowired
	HttpServletRequest request;

	@Autowired
	ISecurityService securityService;

	@Autowired
	DateHelper dateHelper;

	@Operation(summary = "Returns a list harvesting data sources with paging/sorting using {pageable}")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns a list harvesting data sources with paging/sorting using {pageable}") })
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	HttpEntity<Page<IHarvestingSource>> getSources(@ParameterObject Pageable pageable) throws HarvesterInfoServiceException {

		Page<IHarvestingSource> result = null;

		// if its a admin request show all sources
		if (securityService.isAdminRequest(request)) {
			result = hService.listSources(pageable);
		} else { // else get the groups of this user and return a filtered source list using
					// groups as whitelist
			result = hService.listSources(securityService.getRequestGroups(request), pageable);
		}

		return new ResponseEntity<Page<IHarvestingSource>>(result, HttpStatus.OK);
	}

	// @Operation(summary = "Returns a harvesting source info by source id")
	// @ApiResponses(value = { @ApiResponse(code = 200, message = "Returns a
	// harvesting source info by source id") })
	// @RequestMapping(value = "/by_id/{sourceID}", method = RequestMethod.GET)
	// HttpEntity<IHarvestingSource> getSourceById(@PathVariable("sourceID") Long
	// sourceID)
	// throws HarvesterInfoServiceException {
	//
	// IHarvestingSource result = hService.getSourceByID(sourceID);
	//
	// return new ResponseEntity<IHarvestingSource>(result, HttpStatus.OK);
	// }

	@Operation(summary = "Returns a harvesting source info by source acronym")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns a harvesting source info by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}", method = RequestMethod.GET)
	HttpEntity<IHarvestingSource> getSourceByAcronym(@PathVariable("sourceAcronym") String sourceAcronym)
			throws HarvesterInfoServiceException {

		IHarvestingSource result = hService.getSourceByAcronym(sourceAcronym);

		return new ResponseEntity<IHarvestingSource>(result, HttpStatus.OK);
	}

	// @ApiOperation(value = "Returns a harvesting source harvesting history by
	// source id")
	// @ApiResponses(value = {
	// @ApiResponse(code = 200, message = "Returns a harvesting source harvesting
	// info by source id") })
	// @RequestMapping(value = "/by_id/{sourceID}/history", method =
	// RequestMethod.GET)
	// HttpEntity<Page<IHarvestingResult>>
	// getHarvestingHistoryById(@PathVariable("sourceID") Long sourceID,
	// Pageable pageable) throws HarvesterInfoServiceException {
	//
	// Page<IHarvestingResult> result =
	// hService.getHarvestingHistoryBySourceID(sourceID, pageable);
	//
	// return new ResponseEntity<Page<IHarvestingResult>>(result, HttpStatus.OK);
	// }

	@Operation(summary = "Returns a harvesting source harvesting history by source acronym")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns a harvesting source harvesting info by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/history", method = RequestMethod.GET)
	HttpEntity<Page<IHarvestingResult>> getHarvestingHistoryByAcronym(
			@PathVariable("sourceAcronym") String sourceAcronym, @ParameterObject Pageable pageable)
			throws HarvesterInfoServiceException {

		try {
			Page<IHarvestingResult> result = hService.getHarvestingHistoryBySourceAcronym(sourceAcronym, pageable);
			return new ResponseEntity<Page<IHarvestingResult>>(result, HttpStatus.OK);

		} catch (HarvesterInfoServiceException e) {
			logger.error("getHarvestingHistoryByAcronym: " + e.getMessage(), e);
			return new ResponseEntity<Page<IHarvestingResult>>(HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			logger.error("getHarvestingHistoryByAcronym: " + e.getMessage(), e);
			return new ResponseEntity<Page<IHarvestingResult>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@Operation(summary = "Returns a harvesting source harvesting history within a time interval by source acronym")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns a harvesting source harvesting info for a given time interval by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/history/{startDate}/{endDate}", method = RequestMethod.GET)
	HttpEntity<Page<IHarvestingResult>> getHarvestingHistoryByAcronymAndDate(
			@PathVariable("sourceAcronym") String sourceAcronym, @PathVariable("startDate") String fromDate,
			@PathVariable("endDate") String toDate, @ParameterObject Pageable pageable)
	{

		// converts from dates to localdatetime at the start and end of the given dates
		LocalDateTime startDate = dateHelper.parseDate(fromDate).toLocalDate().atStartOfDay();
		LocalDateTime endDate = dateHelper.parseDate(toDate).toLocalDate().atTime(LocalTime.MAX);

		try {
			Page<IHarvestingResult> result = hService.getHarvestingHistoryBySourceAcronym(sourceAcronym, startDate,
					endDate, pageable);
			return new ResponseEntity<Page<IHarvestingResult>>(result, HttpStatus.OK);

		} catch (HarvesterInfoServiceException e) {
			logger.error("getHarvestingHistoryByAcronymAndDate: " + e.getMessage());
			return new ResponseEntity<Page<IHarvestingResult>>(HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			logger.error("getHarvestingHistoryByAcronymAndDate: " + e.getMessage());
			return new ResponseEntity<Page<IHarvestingResult>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	// @ApiOperation(value = "Returns a harvesting source last good known harvesting
	// by source id")
	// @ApiResponses(value = {
	// @ApiResponse(code = 200, message = "Returns a harvesting source last good
	// known harvesting by source id") })
	// @RequestMapping(value = "/by_id/{sourceID}/lkg", method = RequestMethod.GET)
	// HttpEntity<IHarvestingResult>
	// getLGKSnapshotBySourceId(@PathVariable("sourceID") Long sourceID)
	// throws HarvesterInfoServiceException {
	//
	// IHarvestingResult result =
	// hService.getLastKnownGoodHarvestingBySourceID(sourceID);
	//
	// return new ResponseEntity<IHarvestingResult>(result, HttpStatus.OK);
	// }

	@Operation(summary = "Returns a harvesting source last good known harvesting by source acronym")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns a harvesting source last good known harvesting by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/lkg", method = RequestMethod.GET)
	HttpEntity<IHarvestingResult> getLGKSnapshotBySourceAcronym(@PathVariable("sourceAcronym") String sourceAcronym)
			throws HarvesterInfoServiceException {

		IHarvestingResult result = hService.getLastKnownGoodSHarvestingBySourceAcronym(sourceAcronym);

		return new ResponseEntity<IHarvestingResult>(result, HttpStatus.OK);
	}


	@Operation(summary = "Returns the metadata for a record by snapshot id and identifier")
	@ApiResponses(value = {
	@ApiResponse(responseCode = "200", description = "Returns the metadata for a record by snapshot id and identifier") })
	@RequestMapping(value = "/{sourceAcronym}/record", method = RequestMethod.GET)

	HttpEntity<String> getRecordMetadataBySnapshotAndIdentifier(@PathVariable("sourceAcronym") String sourceAcronym,
			@Parameter(description = "OAI Identifier", required = true) @RequestParam("identifier") String identifier,
    		@Parameter(description = "Harvesting ID", required = true, example = "1") @RequestParam("harvestingID") Long harvestingID
    	) throws HarvesterInfoServiceException {

		String result = hService.getRecordMetadataBySnapshotAndIdentifier(harvestingID, identifier);

		return new ResponseEntity<String>(result, HttpStatus.OK);

	}

	
	
}
