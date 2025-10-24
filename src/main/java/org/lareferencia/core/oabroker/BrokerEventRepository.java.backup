package org.lareferencia.core.oabroker;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface BrokerEventRepository extends JpaRepository<BrokerEvent, Long> {


	@Modifying
	@Transactional
	@Query("delete from BrokerEvent n where n.networkId = ?1")
	void deleteByNetworkID(Long network_id);
	
	Page<BrokerEvent> findByNetworkId(Long networkId, Pageable page);
	Page<BrokerEvent> findByNetworkIdAndIdentifier(Long networkId, String identifier, Pageable page);
	Page<BrokerEvent> findByNetworkIdAndIdentifierAndTopic(Long networkId, String identifier, String topic, Pageable page);
	Page<BrokerEvent> findByNetworkIdAndTopic(Long id, String topic, Pageable pageable);
		
	
}
