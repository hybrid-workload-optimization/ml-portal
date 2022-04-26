package kr.co.strato.portal.networking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import kr.co.strato.adapter.k8s.common.model.YamlApplyParam;
import kr.co.strato.adapter.k8s.ingress.service.IngressAdapterService;
import kr.co.strato.adapter.k8s.ingressController.model.ServicePort;
import kr.co.strato.domain.IngressController.model.IngressControllerEntity;
import kr.co.strato.domain.cluster.model.ClusterEntity;
import kr.co.strato.domain.cluster.service.ClusterDomainService;
import kr.co.strato.domain.ingress.model.IngressEntity;
import kr.co.strato.domain.ingress.model.IngressRuleEntity;
import kr.co.strato.domain.ingress.service.IngressDomainService;
import kr.co.strato.domain.ingress.service.IngressRuleDomainService;
import kr.co.strato.domain.namespace.model.NamespaceEntity;
import kr.co.strato.domain.project.model.ProjectEntity;
import kr.co.strato.domain.project.service.ProjectDomainService;
import kr.co.strato.global.error.exception.InternalServerException;
import kr.co.strato.global.util.Base64Util;
import kr.co.strato.global.util.DateUtil;
import kr.co.strato.portal.cluster.service.ClusterNodeService;
import kr.co.strato.portal.common.service.ProjectAuthorityService;
import kr.co.strato.portal.networking.model.IngressControllerDto;
import kr.co.strato.portal.networking.model.IngressControllerDtoMapper;
import kr.co.strato.portal.networking.model.IngressDto;
import kr.co.strato.portal.networking.model.IngressDtoMapper;
import kr.co.strato.portal.setting.model.UserDto;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IngressService extends ProjectAuthorityService {

	@Autowired
	private IngressAdapterService ingressAdapterService;

	@Autowired
	private IngressDomainService ingressDomainService;

	@Autowired
	private IngressRuleDomainService ingressRuleDomainService;
	
	@Autowired
	private ClusterNodeService clusterNodeService;

	@Autowired
	private ClusterDomainService clusterDomainService;

	@Autowired
	ProjectDomainService projectDomainService;

	public Page<IngressDto.ResListDto> getIngressList(Pageable pageable, IngressDto.SearchParam searchParam) {
		Page<IngressEntity> ingressPage = ingressDomainService.getIngressList(pageable, searchParam.getClusterIdx(),
				searchParam.getNamespaceIdx());
		List<IngressDto.ResListDto> ingressList = ingressPage.getContent().stream()
				.map(c -> IngressDtoMapper.INSTANCE.toResListDto(c)).collect(Collectors.toList());

		Page<IngressDto.ResListDto> page = new PageImpl<>(ingressList, pageable, ingressPage.getTotalElements());
		return page;
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Ingress> getIngressListSet(Long clusterId) {
		List<Ingress> ingressList = ingressAdapterService.getIngressList(clusterId);

		synIngressSave(ingressList, clusterId);
		return ingressList;
	}

	public List<Long> synIngressSave(List<Ingress> ingressList, Long clusterId) {
		List<Long> ids = new ArrayList<>();
		for (Ingress i : ingressList) {
			try {
				IngressEntity ingress = toEntity(i, clusterId, 44L);

				// save
				Long id = ingressDomainService.register(ingress);
				// ingress rule save
				ingressRuleRegister(i, id);
				ids.add(id);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				throw new InternalServerException("parsing error");
			}
		}

		return ids;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean deleteIngress(Long id) {
		IngressEntity i = ingressDomainService.getDetail(id.longValue());
		String IngressName = i.getName();
		String namespace = i.getNamespace().getName();
		Long clusterId = i.getNamespace().getCluster().getClusterId();

		boolean isDeleted = ingressAdapterService.deleteIngress(clusterId, IngressName, namespace);
		//if (isDeleted) {
			return ingressDomainService.delete(id.longValue());
		//} else {
		//	throw new InternalServerException("k8s Ingress 삭제 실패");
		//}
	}

	public IngressDto.ResDetailDto getIngressDetail(Long id, UserDto loginUser) {
		IngressEntity ingressEntity = ingressDomainService.getDetail(id);
		List<IngressRuleEntity> ruleList = ingressRuleDomainService.findByIngressId(id);

		Long clusterIdx = ingressEntity.getNamespace().getCluster().getClusterIdx();
		ProjectEntity projectEntity = projectDomainService.getProjectDetailByClusterId(clusterIdx);
		Long projectIdx = projectEntity.getId();

		// 메뉴 접근권한 채크.
		chechAuthority(projectIdx, loginUser);

		IngressDto.ResDetailDto ingressDto = IngressDtoMapper.INSTANCE.toResDetailDto(ingressEntity);
		List<IngressDto.RuleList> ruleDto = ruleList.stream().map(c -> IngressDtoMapper.INSTANCE.toRuleListDto(c))
				.collect(Collectors.toList());
		ingressDto.setRuleList(ruleDto);
		ingressDto.setProjectIdx(projectIdx);
		return ingressDto;
	}

	public String getIngressYaml(Long kubeConfigId, String name, String namespace) {
		String yaml = ingressAdapterService.getIngressYaml(kubeConfigId, name, namespace);
		yaml = Base64Util.encode(yaml);
		return yaml;
	}

	public List<Long> registerIngress(IngressDto.ReqCreateDto yamlApplyParam) {
		String yamlDecode = Base64Util.decode(yamlApplyParam.getYaml());
		ClusterEntity clusterEntity = clusterDomainService.get(yamlApplyParam.getKubeConfigId());
		Long clusterId = clusterEntity.getClusterId();
		Long clusterIdx = clusterEntity.getClusterIdx();
		List<Ingress> ingressList = ingressAdapterService.registerIngress(clusterId, yamlDecode);
		List<Long> ids = new ArrayList<>();

		for (Ingress i : ingressList) {
			try {
				// k8s Object -> Entity
				IngressEntity ingress = toEntity(i, clusterId, clusterIdx);
				// save
				Long id = ingressDomainService.register(ingress);

				// ingress rule save
				ingressRuleRegister(i, id);

				ids.add(id);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				throw new InternalServerException("parsing error");
			}
		}

		return ids;
	}

	public List<Long> updateIngress(Long ingressId, YamlApplyParam yamlApplyParam) {
		String yaml = Base64Util.decode(yamlApplyParam.getYaml());

		ClusterEntity clusterEntity = clusterDomainService.get(yamlApplyParam.getKubeConfigId());
		Long clusterId = clusterEntity.getClusterId();
		Long clusterIdx = clusterEntity.getClusterIdx();

		List<Ingress> ingress = ingressAdapterService.registerIngress(clusterId, yaml);

		List<Long> ids = ingress.stream().map(i -> {
			try {
				IngressEntity updateIngress = toEntity(i, clusterId, clusterIdx);

				Long id = ingressDomainService.update(updateIngress, ingressId);

				boolean ruleDel = ingressRuleDomainService.delete(id);

				if (ruleDel) {
					// ingress rule save
					ingressRuleRegister(i, ingressId);
				}
				return id;
			} catch (JsonProcessingException e) {
				log.error(e.getMessage(), e);
				throw new InternalServerException("json 파싱 에러");
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new InternalServerException("Ingress update error");
			}
		}).collect(Collectors.toList());
		return ids;
	}

	private IngressEntity toEntity(Ingress i, Long clusterId, Long clusterIdx) throws JsonProcessingException {
		// k8s Object -> Entity
		String name = i.getMetadata().getName();
		String uid = i.getMetadata().getUid();
		String ingressClass = i.getSpec().getIngressClassName();
		String createdAt = i.getMetadata().getCreationTimestamp();

		IngressControllerEntity ingressControllerEntity = new IngressControllerEntity();

		NamespaceEntity namespaceEntity = ingressDomainService.findByName(i.getMetadata().getNamespace(), clusterIdx);
		if (ingressClass != null) {

			List<Ingress> ingressClassK8s = ingressAdapterService.getIngressClassName(clusterId, ingressClass);
			for (Ingress ic : ingressClassK8s) {
				ingressClass = ic.getMetadata().getName();
			}

			ingressControllerEntity = ingressDomainService.findIngressControllerByName(ingressClass);

		} else {
			ingressControllerEntity = ingressDomainService.findByDefaultYn("Y");
			ingressClass = "default";
		}

		IngressEntity ingress = IngressEntity.builder().name(name).uid(uid).ingressClass(ingressClass)
				.ingressController(ingressControllerEntity).createdAt(DateUtil.strToLocalDateTime(createdAt))
				.namespace(namespaceEntity).build();

		return ingress;
	}

	public void ingressRuleRegister(Ingress i, Long ingressIdx) {
		IngressEntity ingress = ingressDomainService.get(ingressIdx);
		List<IngressRuleEntity> ingressRuls = new ArrayList<>();
		List<IngressRule> rules = i.getSpec().getRules();
		for (IngressRule rule : rules) {

			String host = rule.getHost();
			HTTPIngressRuleValue ruleValue = rule.getHttp();

			if (ruleValue != null) {
				ObjectMapper mapper = new ObjectMapper();
				List<HTTPIngressPath> rulePaths = ruleValue.getPaths();
				for (HTTPIngressPath rulePath : rulePaths) {
					String path = rulePath.getPath();
					String pathType = rulePath.getPathType();
					String protocol = "http";

					IngressBackend backend = rulePath.getBackend();
					IngressServiceBackend serviceBackend = backend.getService();
					String serviceName = serviceBackend.getName();
					ServiceBackendPort servicebackendPort = serviceBackend.getPort();

					Integer portNumber = servicebackendPort.getNumber();

					
					List<String> endpoints = endpoints(protocol, host, path, ingress);				
					String endpointStr = null;
					try {
						endpointStr = mapper.writeValueAsString(endpoints);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

					IngressRuleEntity ingressRuleEntity = IngressRuleEntity.builder()
							.ingress(ingress)
							.host(host)
							.protocol(protocol)
							.path(path)
							.pathType(pathType)
							.service(serviceName)
							.port(portNumber)
							.endpoint(endpointStr)
							.build();
					
					
					
					ingressRuls.add(ingressRuleEntity);
				}
				ingressRuleDomainService.saveAllingress(ingressRuls);
			}
		}
	}
	
	/**
	 * endpoint 리스트 반환.
	 * @param protocol
	 * @param host
	 * @param portNumber
	 * @param path
	 * @return
	 */
	private List<String> endpoints(String protocol, String host, String path, IngressEntity ingress) {		
		List<String> endpoints = new ArrayList<>();
		if(host == null) {
			//엔드포인트 조회 후 아이피 넣어야함.
			IngressControllerEntity ingressController = ingress.getIngressController();
			
			if(ingressController != null) {
				String ingressControllerName = ingressController.getName();
				if(ingressControllerName.equals(IngressEntity.INGRESS_NAME_NGINX)) {
					IngressControllerDto.ResListDto  dto = IngressControllerDtoMapper.INSTANCE.toResListDto(ingressController);
					
					//ingressController가 존재하는 경우에만 Endpoint가 존재함.
					//ingress 생성 후 ingressController를 설치한 경우라면 해당 ingress를 업데이트 해야 동작함.
					
					String serviceType = dto.getServiceType();
					if(serviceType.equals(IngressControllerEntity.SERVICE_TYPE_NODE_PORT)) {
						//Node Port
						Long kubeConfigId = ingress.getNamespace().getCluster().getClusterId();						
						List<String> workerIps = clusterNodeService.getWorkerNodeIps(kubeConfigId);
						
						List<ServicePort> ports = dto.getPort();
						for(ServicePort port : ports) {
							String prot = port.getProtocol();
							Integer p = port.getPort();
							
							for(String ip: workerIps) {
								String endpoint = String.format("%s://%s:%d/%s", prot, ip, p, path);
								endpoints.add(endpoint);
							}
						}
						
					} else if(serviceType.equals(IngressControllerEntity.SERVICE_TYPE_EXTERNAL_IPS)) {
						//ExternalIps
						
						List<String> ips = dto.getExternalIp();
						for(String ip: ips) {
							String endpoint = String.format("%s://%s/%s", protocol, ip, path);
							endpoints.add(endpoint);
						}
					}
				} else {
					log.error("Unknown ingress type: {}", ingressControllerName);
				}
			} 
		} else {
			String endpoint = String.format("%s://%s/%s", protocol, host, path);
			endpoints.add(endpoint);
		}
		return endpoints;
	}

}
