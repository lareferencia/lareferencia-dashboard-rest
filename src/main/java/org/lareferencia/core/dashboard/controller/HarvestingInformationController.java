
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

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@Api(value = "Harvesting Information", tags = "Harvesting")
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

	@ApiOperation(value = "Returns a list harvesting data sources with paging/sorting using {pageable}")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns a list harvesting data sources with paging/sorting using {pageable}") })
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	HttpEntity<Page<IHarvestingSource>> getSources(Pageable pageable) throws HarvesterInfoServiceException {

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

	// @ApiOperation(value = "Returns a harvesting source info by source id")
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

	@ApiOperation(value = "Returns a harvesting source info by source acronym")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns a harvesting source info by source acronym") })
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

	@ApiOperation(value = "Returns a harvesting source harvesting history by source acronym")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns a harvesting source harvesting info by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/history", method = RequestMethod.GET)
	HttpEntity<Page<IHarvestingResult>> getHarvestingHistoryByAcronym(
			@PathVariable("sourceAcronym") String sourceAcronym, Pageable pageable)
			throws HarvesterInfoServiceException {

		try {
			Page<IHarvestingResult> result = hService.getHarvestingHistoryBySourceAcronym(sourceAcronym, pageable);
			return new ResponseEntity<Page<IHarvestingResult>>(result, HttpStatus.OK);

		} catch (HarvesterInfoServiceException e) {
			logger.error("getHarvestingHistoryByAcronym: " + e.getMessage());
			return new ResponseEntity<Page<IHarvestingResult>>(HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			logger.error("getHarvestingHistoryByAcronym: " + e.getMessage());
			return new ResponseEntity<Page<IHarvestingResult>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation(value = "Returns a harvesting source harvesting history within a time interval by source acronym")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns a harvesting source harvesting info for a given time interval by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/history/{startDate}/{endDate}", method = RequestMethod.GET)
	HttpEntity<Page<IHarvestingResult>> getHarvestingHistoryByAcronymAndDate(
			@PathVariable("sourceAcronym") String sourceAcronym, @PathVariable("startDate") String fromDate,
			@PathVariable("endDate") String toDate, Pageable pageable)
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

	@ApiOperation(value = "Returns a harvesting source last good known harvesting by source acronym")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns a harvesting source last good known harvesting by source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/lkg", method = RequestMethod.GET)
	HttpEntity<IHarvestingResult> getLGKSnapshotBySourceAcronym(@PathVariable("sourceAcronym") String sourceAcronym)
			throws HarvesterInfoServiceException {

		IHarvestingResult result = hService.getLastKnownGoodSHarvestingBySourceAcronym(sourceAcronym);

		return new ResponseEntity<IHarvestingResult>(result, HttpStatus.OK);
	}


	@Deprecated
	@ApiOperation(value = "Returns the metadata for a record by id and source acronym")
	@ApiResponses(value = {
	@ApiResponse(code = 200, message = "Returns the metadata for a record by id and source acronym") })
	@RequestMapping(value = "/{sourceAcronym}/record/{recordID}", method = RequestMethod.GET)
	HttpEntity<String> getRecordMetadataByID(@PathVariable("sourceAcronym") String sourceAcronym,
			@PathVariable("recordID") Long recordID) throws HarvesterInfoServiceException {

		String result = hService.getRecordMetadataByRecordIDAndSourceAcronym(sourceAcronym, recordID);

		return new ResponseEntity<String>(result, HttpStatus.OK);

	}
	
	@ApiOperation(value = "Returns the metadata for a record by snapshot id and identifier")
	@ApiResponses(value = {
	@ApiResponse(code = 200, message = "Returns the metadata for a record by snapshot id and identifier") })
	@RequestMapping(value = "/{sourceAcronym}/record", method = RequestMethod.GET)

	HttpEntity<String> getRecordMetadataBySnapshotAndIdentifier(@PathVariable("sourceAcronym") String sourceAcronym,
			@ApiParam(value = "OAI Identifier", required = true) @RequestParam("identifier") String identifier,
    		@ApiParam(value = "Harvesting ID", required = true, example = "1") @RequestParam("harvestingID") Long harvestingID
    	) throws HarvesterInfoServiceException {

		String result = hService.getRecordMetadataBySnapshotAndIdentifier(harvestingID, identifier);

		return new ResponseEntity<String>(result, HttpStatus.OK);

	}

	
	
}
