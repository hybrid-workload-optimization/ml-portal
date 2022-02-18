package kr.co.strato.adapter.k8s.cluster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ClusterAdapterDto {

	private Long kubeConfigId;
	
	private String provider;
	
	private String configContents;
	
}