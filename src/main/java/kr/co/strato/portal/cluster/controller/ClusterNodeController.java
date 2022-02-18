package kr.co.strato.portal.cluster.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.fabric8.kubernetes.api.model.Node;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import kr.co.strato.adapter.k8s.common.model.YamlApplyParam;
import kr.co.strato.global.error.exception.BadRequestException;
import kr.co.strato.global.model.PageRequest;
import kr.co.strato.global.model.ResponseWrapper;
import kr.co.strato.portal.cluster.model.ClusterNodeDto;
import kr.co.strato.portal.cluster.service.ClusterNodeService;



@RestController
public class ClusterNodeController {

	@Autowired
	private ClusterNodeService nodeService;

	
	/**
	 * @param kubeConfigId
	 * @return
	 * k8s data 호출 및 db저장
	 */
	@GetMapping("/api/v1/cluster/clusterNodesListSet")
	@ResponseStatus(HttpStatus.OK)
	public List<Node> getClusterNodeList(@RequestParam Integer kubeConfigId) {
		if (kubeConfigId == null) {
			throw new BadRequestException("kubeConfigId id is null");
		}
		return nodeService.getClusterNodeList(kubeConfigId);
	}
	
	
	/**
	 * @param pageRequest
	 * @return
	 * page List
	 */
	@GetMapping("/api/v1/cluster/clusterNodes")
	@ResponseStatus(HttpStatus.OK)
    public ResponseWrapper<Page<ClusterNodeDto>> getClusterNodeList(PageRequest pageRequest){
        Page<ClusterNodeDto> results = nodeService.getClusterNodeList(pageRequest.of());
        return new ResponseWrapper<>(results);
    }


	
	@PostMapping("/api/v1/cluster/registerClusterNode")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseWrapper<List<Long>> registerClusterNodes(@RequestBody YamlApplyParam yamlApplyParam ,@RequestParam Integer kubeConfigId) {
		List<Long> ids = nodeService.registerClusterNode(yamlApplyParam,kubeConfigId);

		return new ResponseWrapper<>(ids);
	}

	@DeleteMapping("/api/v1/cluster/deletClusterNode")
	@ResponseStatus(HttpStatus.OK)
	public ResponseWrapper<Boolean> deleteClusterNode(@RequestParam Integer kubeConfigId, 	@RequestParam String name) {
		boolean results = nodeService.deleteClusterNode(kubeConfigId, name);

		return new ResponseWrapper<>(results);
	}
	
}