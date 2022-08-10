package kr.co.strato.domain.machineLearning.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import kr.co.strato.domain.cluster.model.ClusterEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "machine_learning_cluster")
@Getter
@Setter
@ToString
public class MLClusterEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ml_cluster_idx")
	private Long id;
	
	@OneToOne
    @JoinColumn(name = "cluster_idx")
	private ClusterEntity cluster;
	
	private String clusterType;
	private String createdAt;	
	private String updatedAt;
	private String status;
}
