package kr.co.strato.domain.cluster.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cluster")
public class ClusterEntity {
	
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cluster_idx", unique = true)
    private Long clusterIdx;
	
	@Column(name = "cluster_name")
	private String clusterName;
	
	@Column
	private String provider;
	
	@Column(name = "provider_version")
	private String providerVersion;
	
	@Column(name = "kube_config")
	private String kubeConfig;
	
	@Column
	private String description;
	
	@Column
	private String status;
	
	@Column(name = "create_user_id")
	private String createUserId;
	
	@Column(name = "create_user_name")
	private String createUserName;
	
	@Column(name = "created_at")
	private String createdAt;
	
	@Column(name = "update_user_id")
	private String updateUserId;
	
	@Column(name = "update_user_name")
	private String updateUserName;
	
	@Column(name = "updated_at")
	private String updatedAt;
	
	@Column(name = "cluster_id")
	private Long clusterId;
	
}
