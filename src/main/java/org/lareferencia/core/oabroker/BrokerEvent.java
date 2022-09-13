package org.lareferencia.core.oabroker;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Table(name = "broker_event" , indexes = { @Index(name = "broker_event_identifier",  columnList="identifier", unique = false),  
										   @Index(name = "broker_network_id",  		 columnList="network_id", unique = false) }  )
@javax.persistence.Entity
public class BrokerEvent  {

	@JsonIgnore
	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected Long id;

	@Column(nullable = false)
	private String identifier;
	
	@Type(type = "org.hibernate.type.TextType")
	private String message;
	
	private String topic;
	
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "network_id")
//	private Network network;
//	
	@Setter(AccessLevel.NONE)
	@JsonIgnore
	@Column(name = "network_id"/*, insertable = false, updatable = false*/)
	private Long networkId;

	public BrokerEvent(String identifier, String message, String topic, Long network_id) {
		super();
		this.identifier = identifier;
		this.message = message;
		this.topic = topic;
		//this.network = network;
		this.networkId = network_id;
	}
}
