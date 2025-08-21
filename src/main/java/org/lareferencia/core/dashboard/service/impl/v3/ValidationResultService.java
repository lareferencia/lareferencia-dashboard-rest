
package org.lareferencia.core.dashboard.service.impl.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lareferencia.backend.domain.NetworkSnapshot;
import org.lareferencia.backend.domain.ValidatorRule;
import org.lareferencia.backend.domain.validation.ValidationStatsQueryResult;
import org.lareferencia.backend.domain.parquet.ValidationStatObservationParquet;
import org.lareferencia.backend.repositories.jpa.NetworkSnapshotRepository;
import org.lareferencia.backend.services.validation.ValidationStatisticsException;
import org.lareferencia.backend.services.validation.IValidationStatisticsService;
import org.lareferencia.core.dashboard.service.IRecordValidationResult;
import org.lareferencia.core.dashboard.service.IValidationInformationService;
import org.lareferencia.core.dashboard.service.ValueCount;
import org.lareferencia.core.dashboard.service.ValidationResult;
import org.lareferencia.core.dashboard.service.ValidationInformationServiceException;
import org.lareferencia.core.dashboard.service.ValidationRule;
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
	 * @param networkAcronym
	 * @param snapshotID
	 * @return
	 * @throws ValidationInformationServiceException
	 */
	public ValidationResult validationResultByHarvestingID(String networkAcronym, Long snapshotID) throws ValidationInformationServiceException {
		
		NetworkSnapshot snapshot = obtainNetworkSnapshotAndCheckAcronymCorrespondece(networkAcronym, snapshotID);
		
		try {
			
			// Consultar estadísticas agregadas de validación usando la nueva API
			ValidationStatsQueryResult queryResult = validationService.queryValidatorRulesStatsBySnapshot(snapshotID, new ArrayList<>());
			
			ValidationResult result = new ValidationResult();
			
			// Obtener métricas básicas desde las agregaciones (formato parquet)
			Map<String, Object> aggregations = queryResult.getAggregations();
			if (aggregations != null) {
				result.setSize(getIntegerFromMetadata(aggregations, "size"));
				result.validSize = getIntegerFromMetadata(aggregations, "validSize");
				result.transformedSize = getIntegerFromMetadata(aggregations, "transformedSize");
			}
			
			// Procesar las reglas del validador
			if (snapshot.getNetwork() == null || snapshot.getNetwork().getValidator() == null) {
				throw new ValidationInformationServiceException("Validator of Snapshot w/ ID:" + snapshotID + " do not exist");
			}
			
			// Obtener conteos de reglas desde las agregaciones - formato parquet
			Map<String, Integer> validRuleMap = new HashMap<>();
			Map<String, Integer> invalidRuleMap = new HashMap<>();
			
			// El servicio Parquet devuelve los datos en "rulesByID"
			if (aggregations != null && aggregations.containsKey("rulesByID")) {
				@SuppressWarnings("unchecked")
				Map<String, Map<String, Object>> rulesByID = (Map<String, Map<String, Object>>) aggregations.get("rulesByID");
				
				for (Map.Entry<String, Map<String, Object>> ruleEntry : rulesByID.entrySet()) {
					String ruleId = ruleEntry.getKey();
					Map<String, Object> ruleData = ruleEntry.getValue();
					
					Object validCount = ruleData.get("validCount");
					Object invalidCount = ruleData.get("invalidCount");
					
					if (validCount instanceof Number) {
						validRuleMap.put(ruleId, ((Number) validCount).intValue());
					}
					if (invalidCount instanceof Number) {
						invalidRuleMap.put(ruleId, ((Number) invalidCount).intValue());
					}
				}
			}
			
			for (ValidatorRule rule : snapshot.getNetwork().getValidator().getRules()) {
				String ruleID = rule.getId().toString();

				ValidationRule ruleResult = new ValidationRule();
				ruleResult.ruleID = rule.getId();
				ruleResult.validCount = Optional.ofNullable(validRuleMap.get(ruleID)).orElse(0);
				ruleResult.invalidCount = Optional.ofNullable(invalidRuleMap.get(ruleID)).orElse(0);
				ruleResult.name = rule.getName();
				ruleResult.description = rule.getDescription();
				ruleResult.mandatory = rule.getMandatory();
				ruleResult.quantifier = rule.getQuantifier().toString();

				result.rulesByID.put(ruleID, ruleResult);
			}

			return result;
			
		} catch (ValidationStatisticsException e) {
			logger.error("Error querying validation statistics: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Error retrieving validation results: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Unexpected error retrieving validation results: " + e.getMessage());
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
			ValidationStatsQueryResult queryResult = validationService.queryValidationStatsObservationsBySnapshotID(
				snapshotID, filters, pageable);
			
			// Convertir resultados a IRecordValidationResult
			List<IRecordValidationResult> results = queryResult.getContent().stream()
				.map(obs -> new ValidationStatObservationAdapter(obs))
				.collect(Collectors.toList());
			
			return new PageImpl<>(results, pageable, queryResult.getTotalElements());
			
		} catch (ValidationStatisticsException e) {
			logger.error("Error querying validation observations: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Error retrieving record validation results: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Unexpected error retrieving record validation results: " + e.getMessage());
		}
	}
	
	@Override
	public List<ValueCount> validOccurrenceCountByHarvestingIDAndRuleID(String networkAcronym, Long harvestingID, Long ruleID) throws ValidationInformationServiceException {
		return getOccurrenceCountByRuleID(networkAcronym, harvestingID, ruleID, true);
	}

	@Override
	public List<ValueCount> invalidOccurrenceCountByHarvestingIDAndRuleID(String networkAcronym, Long harvestingID, Long ruleID) throws ValidationInformationServiceException {
		return getOccurrenceCountByRuleID(networkAcronym, harvestingID, ruleID, false);
	}
	
	/**
	 * Helper method to get occurrence counts for a specific rule
	 */
	private List<ValueCount> getOccurrenceCountByRuleID(String networkAcronym, Long snapshotID, Long ruleID, boolean isValid) throws ValidationInformationServiceException {
		
		obtainNetworkSnapshotAndCheckAcronymCorrespondece(networkAcronym, snapshotID);
		
		try {
			
			// Construir filtros específicos para la regla
			List<String> filters = new ArrayList<>();
			
			// Consultar observaciones para obtener ocurrencias
			ValidationStatsQueryResult queryResult = validationService.queryValidationStatsObservationsBySnapshotID(
				snapshotID, filters, org.springframework.data.domain.PageRequest.of(0, 10000));
			
			// Procesar observaciones para extraer conteos de ocurrencias
			Map<String, Integer> occurrenceCounts = new HashMap<>();
			
			for (ValidationStatObservationParquet obs : queryResult.getContent()) {
				Map<String, List<String>> occurrenceMap = isValid ? 
					obs.getValidOccurrencesByRuleID() : 
					obs.getInvalidOccurrencesByRuleID();
					
				if (occurrenceMap != null && occurrenceMap.containsKey(ruleID.toString())) {
					List<String> occurrences = occurrenceMap.get(ruleID.toString());
					if (occurrences != null) {
						for (String occurrence : occurrences) {
							occurrenceCounts.put(occurrence, occurrenceCounts.getOrDefault(occurrence, 0) + 1);
						}
					}
				}
			}
			
			// Convertir a lista de ValueCount y ordenar por conteo descendente
			return occurrenceCounts.entrySet().stream()
				.map(entry -> new ValueCount(entry.getKey(), entry.getValue()))
				.sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
				.collect(Collectors.toList());
			
		} catch (ValidationStatisticsException e) {
			logger.error("Error querying occurrence counts: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Error retrieving occurrence counts: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
			throw new ValidationInformationServiceException("Unexpected error retrieving occurrence counts: " + e.getMessage());
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
	 * @throws ValidationInformationServiceException 
	 */
	private NetworkSnapshot obtainNetworkSnapshotAndCheckAcronymCorrespondece(String networkAcronym, Long snapshotID) throws ValidationInformationServiceException {
		Optional<NetworkSnapshot> opSnapshot = snapshotRepository.findById(snapshotID);
		if (opSnapshot.isPresent() && opSnapshot.get().getNetwork().getAcronym().equals(networkAcronym)) {
			return opSnapshot.get();
		} else {
			throw new ValidationInformationServiceException("Harvesting w/ ID:" + snapshotID + " do not exist");
		}
	}
	
	/**
	 * Adapter class to convert ValidationStatObservationParquet to IRecordValidationResult
	 * Eliminates the need for the separate adapter class
	 */
	private static class ValidationStatObservationAdapter implements IRecordValidationResult {
		
		private final ValidationStatObservationParquet observation;
		
		public ValidationStatObservationAdapter(ValidationStatObservationParquet observation) {
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
