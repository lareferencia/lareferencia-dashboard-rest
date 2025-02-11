
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
import org.lareferencia.core.dashboard.service.HarvesterInfoServiceException;
import org.lareferencia.core.dashboard.service.IHarvestingAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@RestController
@Api(value = "Harvesting Admin", tags = "Harvesting, Admin")
@RequestMapping("/api/v2/admin/")
@CrossOrigin
public class HarvestingAdminController {

	private static Logger logger = LogManager.getLogger(HarvestingAdminController.class);

	@Autowired
	IHarvestingAdminService hService;

	@Autowired
	HttpServletRequest request;


	@ApiOperation(value = "Returns dummy string")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns dummy string") })
	@RequestMapping(value = "/dummy", method = RequestMethod.GET)
	HttpEntity<String> dummy() throws HarvesterInfoServiceException {

		String result = hService.dummy("Jhon Doe");
		return new ResponseEntity<String>(result, HttpStatus.OK);
	}

	

}
