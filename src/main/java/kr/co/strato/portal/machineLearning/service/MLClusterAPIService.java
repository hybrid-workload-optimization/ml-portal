package kr.co.strato.portal.machineLearning.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.co.strato.adapter.k8s.node.service.NodeAdapterService;
import kr.co.strato.domain.machineLearning.model.MLClusterEntity;
import kr.co.strato.domain.machineLearning.service.MLClusterDomainService;
import kr.co.strato.portal.machineLearning.model.MLClusterDto;
import kr.co.strato.portal.machineLearning.model.MLClusterDtoMapper;
import kr.co.strato.portal.machineLearning.model.MLClusterType;

@Service
public class MLClusterAPIService {
	
	@Autowired
	private MLClusterDomainService mlClusterDomainService;
	
	@Autowired
	private NodeAdapterService nodeAdapterService;

	/**
	 * Service Cluster 리스트 반환.
	 * @param pageRequest
	 * @return
	 */
	public List<MLClusterDto.List> getServiceClusterList() {
		List<MLClusterEntity> list = mlClusterDomainService.getList(MLClusterType.SERVICE_CLUSTER.getType());
		
		List<MLClusterDto.List> result = new ArrayList<>(); 
		for(MLClusterEntity entity : list) {
			MLClusterDto.List l = MLClusterDtoMapper.INSTANCE.toListDto(entity);
			result.add(l);
		}
		return result;
	}
	
	public MLClusterDto.Detail getServiceClusterDetail(Long clusterId) {
		MLClusterEntity entity = mlClusterDomainService.get(clusterId);
		if(entity != null) {
			MLClusterDto.Detail d = MLClusterDtoMapper.INSTANCE.toDetailDto(entity);
			return d;
		}
		return null;
	}
	
	public String getPrometheusUrl(Long clusterId) {
		/*
		MLClusterEntity entity = mlClusterDomainService.get(clusterId);
		
		String host = "external-ip";
		List<String> nodeIps = nodeAdapterService.getWorkerNodeIps(entity.getCluster().getClusterId());
		if(nodeIps != null && nodeIps.size() > 0) {
			host = nodeIps.get(0);
		}
		
		String url = String.format("http://%s:30005", host);
		*/
		String url = "http://210.217.178.114:30015/";
		return url;
	}	
}
