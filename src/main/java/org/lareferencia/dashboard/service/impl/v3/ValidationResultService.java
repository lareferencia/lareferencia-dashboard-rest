
package org.lareferencia.dashboard.service.impl.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lareferencia.core.domain.NetworkSnapshot;
import org.lareferencia.core.repository.jpa.NetworkSnapshotRepository;
import org.lareferencia.core.service.validation.IValidationStatisticsService;
import org.lareferencia.core.service.validation.ValidationStatObservation;
import org.lareferencia.core.service.validation.ValidationStatisticsException;
import org.lareferencia.core.service.validation.ValidationStatsObservationsResult;
import org.lareferencia.core.service.validation.ValidationStatsResult;
import org.lareferencia.core.service.validation.ValidationRuleOccurrencesCount;
import org.lareferencia.core.service.validation.OccurrenceCount;
import org.lareferencia.dashboard.service.IRecordValidationResult;
import org.lareferencia.dashboard.service.IValidationInformationService;
import org.lareferencia.dashboard.service.ValueCount;
import org.lareferencia.dashboard.service.ValidationInformationServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ValidationResultService implements IValidationInformationService {

	private static Logger logger = LogManager.getLogger(ValidationResultService.class);

	@Autowired
	private NetworkSnapshotRepository snapshotRepository;

	@Autowired
	private IValidationStatisticsService validationService;

	/**
	 * Obtains validation results by snapshot ID using the core-lib API
	 * 
	 * @param networkAcronym
	 * @param snapshotID
	 * @return
	 * @throws ValidationInformationServiceException
	 */
	public ValidationStatsResult validationResultByHarvestingID(String networkAcronym, Long snapshotID)
			throws ValidationInformationServiceException {

		NetworkSnapshot snapshot = obtainNetworkSnapshotAndCheckAcronymCorrespondece(networkAcronym, snapshotID);

		try {

			return validationService.queryValidatorRulesStatsBySnapshot(snapshot, new ArrayList<>());

		} catch (ValidationStatisticsException e) {
			logger.error("Error querying validation statistics: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Error retrieving validation results: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
			throw new ValidationInformationServiceException(
					"Unexpected error retrieving validation results: " + e.getMessage());
		}
	}

	/**
	 * Obtains paginated record validation results using the core-lib API
	 */
	public Page<IRecordValidationResult> recordValidationResultsByHarvestingID(
			String networkAcronym, Long snapshotID,
			Optional<Boolean> isValid, Optional<Boolean> isTransformed,
			Optional<List<String>> validRuleIds, Optional<List<String>> invalidRuleIds,
			Optional<String> oaiIdentifier, Pageable pageable) throws ValidationInformationServiceException {

		obtainNetworkSnapshotAndCheckAcronymCorrespondece(networkAcronym, snapshotID);

		try {

			// Construir filtros
			List<String> filters = buildFilters(isValid, isTransformed, validRuleIds, invalidRuleIds, oaiIdentifier);

			// Consultar observaciones paginadas
			ValidationStatsObservationsResult queryResult = validationService
					.queryValidationStatsObservationsBySnapshotID(
							snapshotID, filters, pageable);

			// Convertir resultados a IRecordValidationResult
			List<IRecordValidationResult> results = queryResult.getContent().stream()
					.map(obs -> new ValidationStatObservationAdapter(obs))
					.collect(Collectors.toList());

			return new PageImpl<>(results, pageable, queryResult.getTotalElements());

		} catch (ValidationStatisticsException e) {
			logger.error("Error querying validation observations: " + e.getMessage(), e);
			throw new ValidationInformationServiceException(
					"Error retrieving record validation results: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
			throw new ValidationInformationServiceException(
					"Unexpected error retrieving record validation results: " + e.getMessage());
		}
	}

	@Override
	public List<ValueCount> validOccurrenceCountByHarvestingIDAndRuleID(String networkAcronym, Long harvestingID,
			Long ruleID) throws ValidationInformationServiceException {
		return getOccurrenceCountByRuleID(networkAcronym, harvestingID, ruleID, true);
	}

	@Override
	public List<ValueCount> invalidOccurrenceCountByHarvestingIDAndRuleID(String networkAcronym, Long harvestingID,
			Long ruleID) throws ValidationInformationServiceException {
		return getOccurrenceCountByRuleID(networkAcronym, harvestingID, ruleID, false);
	}

	/**
	 * Helper method to get occurrence counts for a specific rule
	 */
	private List<ValueCount> getOccurrenceCountByRuleID(String networkAcronym, Long snapshotID, Long ruleID,
			boolean isValid) throws ValidationInformationServiceException {

		obtainNetworkSnapshotAndCheckAcronymCorrespondece(networkAcronym, snapshotID);

		try {
			// Construir filtros específicos para la regla
			List<String> filters = new ArrayList<>();

			// Consultar ocurrencias usando la API optimizada del core
			ValidationRuleOccurrencesCount result = validationService
					.queryValidRuleOccurrencesCountBySnapshotID(snapshotID, ruleID, filters);

			List<OccurrenceCount> sourceList = isValid ? result.getValidRuleOccrs() : result.getInvalidRuleOccrs();

			if (sourceList == null) {
				return new ArrayList<>();
			}

			// Mapear a ValueCount (ya vienen ordenados del core)
			return sourceList.stream()
					.map(occ -> new ValueCount(occ.getValue(), occ.getCount()))
					.collect(Collectors.toList());

		} catch (ValidationStatisticsException e) {
			logger.error("Error querying occurrence counts: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Error retrieving occurrence counts: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
			throw new ValidationInformationServiceException(
					"Unexpected error retrieving occurrence counts: " + e.getMessage());
		}
	}

	/**
	 * Build filters for querying validation statistics
	 */
	private List<String> buildFilters(Optional<Boolean> isValid, Optional<Boolean> isTransformed,
			Optional<List<String>> validRuleIds, Optional<List<String>> invalidRuleIds,
			Optional<String> oaiIdentifier) {

		List<String> filters = new ArrayList<>();

		if (oaiIdentifier.isPresent()) {
			String identifierFilter = "identifier@@" + oaiIdentifier.get();
			filters.add(identifierFilter);
			logger.info("Added identifier filter: {}", identifierFilter);
		}

		if (isValid.isPresent()) {
			String validFilter = "isValid@@" + isValid.get().toString();
			filters.add(validFilter);
			logger.info("Added isValid filter: {}", validFilter);
		}

		if (isTransformed.isPresent()) {
			String transformedFilter = "isTransformed@@" + isTransformed.get().toString();
			filters.add(transformedFilter);
			logger.info("Added isTransformed filter: {}", transformedFilter);
		}

		if (validRuleIds.isPresent()) {
			for (String ruleId : validRuleIds.get()) {
				String ruleFilter = "validRulesID@@" + ruleId;
				filters.add(ruleFilter);
				logger.info("Added validRuleIds filter: {}", ruleFilter);
			}
		}

		if (invalidRuleIds.isPresent()) {
			for (String ruleId : invalidRuleIds.get()) {
				String ruleFilter = "invalidRulesID@@" + ruleId;
				filters.add(ruleFilter);
				logger.info("Added invalidRulesID filter: {}", ruleFilter);
			}
		}

		logger.info("Total filters built: {}", filters);
		return filters;
	}

	/**
	 * Extract rule counts from aggregations metadata
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Integer> extractRuleCounts(Map<String, Object> aggregations, String ruleType) {
		Map<String, Integer> counts = new HashMap<>();

		if (aggregations != null && aggregations.containsKey(ruleType)) {
			Object ruleData = aggregations.get(ruleType);
			if (ruleData instanceof Map) {
				Map<String, Object> ruleMap = (Map<String, Object>) ruleData;
				for (Map.Entry<String, Object> entry : ruleMap.entrySet()) {
					if (entry.getValue() instanceof Number) {
						counts.put(entry.getKey(), ((Number) entry.getValue()).intValue());
					}
				}
			}
		}

		return counts;
	}

	/**
	 * Get integer value from metadata map
	 */
	private Integer getIntegerFromMetadata(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return 0;
	}

	/**
	 * Check If Snapshots exists and corresponds to the given network acronym
	 * 
	 * @throws ValidationInformationServiceException
	 */
	private NetworkSnapshot obtainNetworkSnapshotAndCheckAcronymCorrespondece(String networkAcronym, Long snapshotID)
			throws ValidationInformationServiceException {
		Optional<NetworkSnapshot> opSnapshot = snapshotRepository.findById(snapshotID);
		if (opSnapshot.isPresent() && opSnapshot.get().getNetwork().getAcronym().equals(networkAcronym)) {
			return opSnapshot.get();
		} else {
			throw new ValidationInformationServiceException("Harvesting w/ ID:" + snapshotID + " do not exist");
		}
	}

	/**
	 * Adapter class to convert ValidationStatObservation to IRecordValidationResult
	 * Eliminates the need for the separate adapter class
	 */
	private static class ValidationStatObservationAdapter implements IRecordValidationResult {

		private final ValidationStatObservation observation;

		public ValidationStatObservationAdapter(ValidationStatObservation observation) {
			this.observation = observation;
		}

		@Override
		public String getId() {
			return observation.getId();
		}

		@Override
		public String getIdentifier() {
			return observation.getIdentifier();
		}

		@Override
		public Long getSnapshotID() {
			return observation.getSnapshotId();
		}

		@Override
		public String getOrigin() {
			return observation.getOrigin();
		}

		@Override
		public String getSetSpec() {
			return observation.getSetSpec();
		}

		@Override
		public String getMetadataPrefix() {
			return observation.getMetadataPrefix();
		}

		@Override
		public Boolean getIsValid() {
			return observation.getIsValid();
		}

		@Override
		public Boolean getIsTransformed() {
			return observation.getIsTransformed();
		}

		@Override
		public Map<String, List<String>> getValidOccurrencesByRuleID() {
			return observation.getValidOccurrencesByRuleID();
		}

		@Override
		public Map<String, List<String>> getInvalidOccurrencesByRuleID() {
			return observation.getInvalidOccurrencesByRuleID();
		}

		@Override
		public List<String> getValidRulesID() {
			return observation.getValidRulesIDList();
		}

		@Override
		public List<String> getInvalidRulesID() {
			return observation.getInvalidRulesIDList();
		}
	}
}
