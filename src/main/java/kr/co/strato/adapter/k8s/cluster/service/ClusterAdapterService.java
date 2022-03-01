package kr.co.strato.adapter.k8s.cluster.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.strato.adapter.k8s.cluster.model.ClusterAdapterDto;
import kr.co.strato.adapter.k8s.cluster.model.ClusterInfoAdapterDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClusterAdapterService {

	@Autowired
	ClusterAdapterClient clusterAdapterClient;
	
	public String registerCluster(ClusterAdapterDto clusterAdapterDto) throws Exception {
		log.debug("[Register Cluster] request : {}", clusterAdapterDto.toString());
		String response = clusterAdapterClient.postCluster(clusterAdapterDto);
		log.debug("[Register Cluster] response : {}", response);
		
		return response;
	}
	
	public boolean updateCluster(ClusterAdapterDto clusterAdapterDto) throws Exception {
		log.debug("[Update Cluster] request : {}", clusterAdapterDto.toString());
		boolean response = clusterAdapterClient.putCluster(clusterAdapterDto);
		log.debug("[Update Cluster] response : {}", response);
		
		return response;
	}
	
	public boolean deleteCluster(Long kubeConfigId) throws Exception {
		log.debug("[Delete Cluster] request : {}", kubeConfigId);
		boolean response = clusterAdapterClient.deleteCluster(kubeConfigId);
		log.debug("[Delete Cluster] response : {}", response);
		
		return response;
	}
	
	public boolean isClusterConnection(String configContents) throws Exception {
		log.debug("[Check Cluster Connection] request : {}", configContents);
		
		ClusterAdapterDto requestDto = ClusterAdapterDto.builder()
				.configContents(configContents)
				.build();
		
		boolean response = clusterAdapterClient.isClusterConnection(requestDto);
		log.debug("[Check Cluster Connection] response : {}", response);
		
		return response;
	}
	
	public ClusterInfoAdapterDto getClusterInfo(Long kubeConfigId) throws Exception {
		log.debug("[Get Cluster Info] request : {}", kubeConfigId);
		String response = clusterAdapterClient.getClusterInfo(kubeConfigId);
		log.debug("[Get Cluster Info] response : {}", response);
		
		ObjectMapper mapper = new ObjectMapper();
		ClusterInfoAdapterDto result = mapper.readValue(response, new TypeReference<ClusterInfoAdapterDto>(){});
        
		return result;
	}
}
