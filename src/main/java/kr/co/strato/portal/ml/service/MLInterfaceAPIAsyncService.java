package kr.co.strato.portal.ml.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import kr.co.strato.domain.cluster.model.ClusterEntity;
import kr.co.strato.domain.cluster.service.ClusterDomainService;
import kr.co.strato.domain.machineLearning.model.MLClusterMappingEntity;
import kr.co.strato.domain.machineLearning.model.MLEntity;
import kr.co.strato.domain.machineLearning.model.MLProjectMappingEntity;
import kr.co.strato.domain.machineLearning.model.MLResourceEntity;
import kr.co.strato.domain.machineLearning.service.MLClusterMappingDomainService;
import kr.co.strato.domain.machineLearning.service.MLDomainService;
import kr.co.strato.domain.machineLearning.service.MLProjectDomainService;
import kr.co.strato.domain.machineLearning.service.MLResourceDomainService;
import kr.co.strato.domain.project.model.ProjectEntity;
import kr.co.strato.global.model.PageRequest;
import kr.co.strato.global.model.ResponseWrapper;
import kr.co.strato.global.util.Base64Util;
import kr.co.strato.global.util.DateUtil;
import kr.co.strato.portal.ml.model.MLDto;
import kr.co.strato.portal.ml.model.MLDto.ListArg;
import kr.co.strato.portal.ml.model.MLDtoMapper;
import kr.co.strato.portal.ml.model.MLResourceDto;
import kr.co.strato.portal.ml.model.MLStepCode;
import kr.co.strato.portal.project.model.ProjectRequestDto;
import kr.co.strato.portal.project.model.ProjectUserDto;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MLInterfaceAPIAsyncService {
	
	@Autowired
	private ServiceFactory serviceFactory;
	
	@Autowired
	private MLDomainService mlDomainService;
	
	@Autowired
	private MLResourceDomainService mlResourceDomainService;
	
	@Autowired
	private ClusterDomainService clusterDomainService;
	
	@Autowired
	private MLClusterMappingDomainService mlClusterMappingDomainService;
	
	@Autowired
	private CallbackService callbackService;
	
	@Autowired
	private MLClusterAPIAsyncService mlClusterAPIService;
	
	@Autowired
	MLProjectDomainService mlProjectDoaminService;
	
	@Autowired
	private MLProjectService mlProjectService;
	
	
	public String apply(MLDto.ApplyArg applyDto) {
		log.info("ML apply start.");
		boolean isNew = false;
		Long clusterIdx = null;
		if(applyDto.getMlId() == null) {
			//새로운 mlId 생성
			String newMlId = genMlId();
			applyDto.setMlId(newMlId);
			
			isNew = true;
		} else {
			MLEntity entity = mlDomainService.get(applyDto.getMlId());
			clusterIdx = entity.getClusterIdx();
		}
		
		String userId = applyDto.getUserId();
		String mlName = applyDto.getName();
		String mlId = applyDto.getMlId();
		String yamStr = Base64Util.decode(applyDto.getYaml());
		String stepCode = applyDto.getMlStepCode();
		
		log.info("ML ID: {}", mlId);
		log.info("ML Name: {}", mlName);
		log.info("StepCode: {}", stepCode);
		log.info("Yaml: {}", yamStr);
		
		//클러스터 선택
		ClusterEntity clusterEntity = null;
		
		if(clusterIdx == null) {
			clusterEntity = getCluster(mlName, stepCode, yamStr);
		} else {
			clusterEntity = clusterDomainService.get(clusterIdx);
		}
		
		
		if(clusterEntity != null) {		
			String provisioningStatus = clusterEntity.getProvisioningStatus();
			
			clusterIdx = clusterEntity.getClusterIdx();
			String now = DateUtil.currentDateTime();
			
			//ML 저장
			MLEntity entity = MLDtoMapper.INSTANCE.toEntity(applyDto);
			entity.setClusterIdx(clusterIdx);
			entity.setUpdatedAt(now);
			if(isNew) {
				entity.setCreatedAt(now);
				entity.setStatus(MLEntity.STATUS_PENDING);
			}
			
			entity.setYaml(yamStr);
			Long mlIdx = mlDomainService.save(entity);
			
			
			//MLCluster, ML mapping 정보 저장
			now = DateUtil.currentDateTime();
			MLClusterMappingEntity mappingEntity = new MLClusterMappingEntity();
			mappingEntity.setCluster(clusterEntity);
			mappingEntity.setMl(entity);
			mappingEntity.setCreatedAt(now);
			mappingEntity.setUpdatedAt(now);
			mlClusterMappingDomainService.save(mappingEntity);
			
			
			if(provisioningStatus.equals(ClusterEntity.ProvisioningStatus.STARTED.name())) {
				//Async 동작 - Kafka로 메세지 푸시
				log.info("클러스터 배포 대기");
				log.info("Cluster name: {}", clusterEntity.getClusterName());
				log.info("ML apply 종료");
					
			} else if(provisioningStatus.equals(ClusterEntity.ProvisioningStatus.FINISHED.name())) {				
				applyResource(entity, isNew);
			}
			
			/*
			//Project 생성
			ProjectRequestDto param = new ProjectRequestDto();
			//ML 생성자를 프로젝트 이름으로 (생각해 봐야 함..)
			param.setProjectName(userId);
			ProjectUserDto manager = ProjectUserDto.builder().userId("ml@strato.co.kr").build();
			param.setProjectManager(manager);
			
			ProjectEntity projectEntity = mlProjectService.createProject(param);
			
			//ML - Project Mapping
			MLProjectMappingEntity projectMappingEntity = new MLProjectMappingEntity();
			projectMappingEntity.setMl(entity);
			projectMappingEntity.setProject(projectEntity);
			mlProjectDoaminService.save(projectMappingEntity);
			*/
			
			return mlId;
		}
		return null;
	}
	
	/**
	 * 클러스터 생성 완료 후 ML
	 * @param mlClusterEntity
	 */
	public void applyContinue(ClusterEntity clusterEntity) {
		List<MLClusterMappingEntity> list = mlClusterMappingDomainService.getByClusterIdx(clusterEntity.getClusterIdx());
		for(MLClusterMappingEntity mappingEntity : list) {
			MLEntity mlEntity = mappingEntity.getMl();
			applyResource(mlEntity);
		}
	}
	
	
	public void applyResource(MLEntity entity) {
		applyResource(entity, true);
	}
	
	public void applyResource(MLEntity entity, boolean isNew) {
		Long mlIdx = entity.getId();
		String mlId = entity.getMlId();
		Long clusterIdx = entity.getClusterIdx();
		String yamlStr = entity.getYaml();
		String now = DateUtil.currentDateTime();
		
		log.info("ML Resource 실행.");
		log.info("ML Name: {}", entity.getName());
		log.info("ML ID: {}", entity.getMlId());
		log.info("Cluster Name: {}", clusterIdx);
		
		Yaml yaml = new Yaml();
		for(Object object : yaml.loadAll(yamlStr)) {
			if(object instanceof Map) {
				Map map = (Map) object;
				
				//Resource apply
				applyResource(map, mlIdx, clusterIdx, now, isNew);
			} else {
				log.error("yaml 파일이 잘못되었습니다.");
			}
		}
		
		//시작으로 상태 업데이트
		entity.setStatus(MLEntity.STATUS_STARTED);
		mlDomainService.save(entity);
		
		String callbackUrl = entity.getCallbackUrl();
		if(callbackUrl != null) {
			MLDto.Detail detail = getMl(mlId);
			callbackService.sendCallback(callbackUrl, new ResponseWrapper<>(detail));
		}
	}
	
	private void applyResource(Map map, Long mlIdx, Long clusterIdx, String createAt, boolean isNew) {
		String kind = null;
		if(map.get("kind") != null) {
			kind = (String)map.get("kind");
		}
		
		String name = null;
		if(map.get("metadata") != null) {
			if(((Map)map.get("metadata")).get("name") != null) {
				name = (String)((Map)map.get("metadata")).get("name");
			}
		}
		
		String output = new Yaml().dump(map);
		
		Long resourceId = null;
		MLResourceEntity resEntity = null;
		if(!isNew) {
			resEntity = mlResourceDomainService.get(mlIdx, name);
			if(resEntity != null) {
				resourceId = resEntity.getResourceId();
			}
		} else {
			resEntity = new MLResourceEntity();
			resEntity.setCreatedAt(createAt);
		}
		
		MLServiceInterface serviceInterface = getServiceInterface(kind);
		if(serviceInterface != null) {
			resourceId = serviceInterface.mlResourceApply(clusterIdx, resourceId, output);
		}
		
		log.info("Apply Resource");
		log.info("ID: {}, Kind: {}", resourceId, kind);
		log.info("Yaml: {}", output);
		
		resEntity.setUpdatedAt(createAt);
		resEntity.setClusterIdx(clusterIdx);
		resEntity.setKind(kind);
		resEntity.setMlResName(name);
		resEntity.setMlIdx(mlIdx);
		resEntity.setYaml(output);
		resEntity.setResourceId(resourceId);
		
		mlResourceDomainService.save(resEntity);
	}
	
	
	
	/**
	 * ml 삭제
	 * @param mlId
	 * @return
	 */
	public void deleteAsync(String mlId) {
		log.info("ML delete start.");
		log.info("ML ID: {}.", mlId);
		MLDto.Detail mlDetail = getMl(mlId);
		if(mlDetail != null) {
			List<MLClusterMappingEntity> list = mlClusterMappingDomainService.getByMlIdx(mlDetail.getId());
			for(MLClusterMappingEntity entity : list) {
				Long clusterIdx = entity.getCluster().getClusterIdx();
				mlClusterAPIService.deleteMlCluster(clusterIdx);
			}
		}
	}
	
	@Transactional
	public boolean delete(String mlId) {
		log.info("ML delete start.");
		log.info("ML ID: {}.", mlId);
		MLDto.Detail mlDetail = getMl(mlId);
		if(mlDetail != null) {
			
			Long mlIdx = mlDetail.getId();
			List<MLResourceDto> resources = mlDetail.getResources();
			String stepCode = mlDetail.getMlStepCode();
			for(MLResourceDto resDto : resources) {
				String kind = resDto.getKind();
				Long resId = resDto.getResourceId();
				String yaml = resDto.getYaml();
				
				MLServiceInterface serviceInterface = getServiceInterface(kind);
				if(serviceInterface != null) {
					//실제 리소스 삭제.
					log.info("ML K8S 리소스 삭제. resId: {}", resId);
					boolean isDelete = serviceInterface.delete(resId, yaml);
				}
			}
			
			log.info("ML Cluster Mapping 삭제.");
			
			//클러스터 맵핑 삭제
			List<MLClusterMappingEntity> list = mlClusterMappingDomainService.getByMlIdx(mlIdx);
			mlClusterMappingDomainService.deleteByMlIdx(mlIdx);
			
			//클러스터 삭제
			if(!stepCode.equals(MLStepCode.SERVICE.getCode())) {				
				if(list != null) {
					for(MLClusterMappingEntity mappingEntity : list) {
						
						ClusterEntity cluster = mappingEntity.getCluster();
						if(cluster != null) {
							Long clusterIdx = mappingEntity.getCluster().getClusterIdx();
							
							log.info("Cluster 삭제. clusterIdx: {}", clusterIdx);
							mlClusterAPIService.deleteMlCluster(clusterIdx);
						}
						
					}
				}
			}
			
			//리소스 삭제
			log.info("ML 리소스 삭제. ML ID: {}", mlIdx);
			mlResourceDomainService.deleteByMlIdx(mlIdx);
			
			//ml 삭제
			log.info("ML 삭제. ML ID: {}", mlIdx);
			mlDomainService.delete(mlIdx);
			
			return true;
		}
		log.error("ML is null");
		log.error("ML ID: {}", mlId);
		return false;
	}
	
	@Transactional
	public boolean deletePre(Long clusterIdx) {		
		ClusterEntity clusterEntity = clusterDomainService.get(clusterIdx);
		
		List<MLClusterMappingEntity> list = mlClusterMappingDomainService.getByClusterIdx(clusterEntity.getClusterIdx());
		for(MLClusterMappingEntity mappingEntity : list) {
			MLEntity mlEntity = mappingEntity.getMl();
			Long mlIdx = mlEntity.getId();
			
			//리소스 삭제
			log.info("ML 리소스 삭제. ML ID: {}", mlIdx);
			mlResourceDomainService.deleteByMlIdx(mlIdx);
			
			//클러스터 맵핑 정보 삭제.
			mlClusterMappingDomainService.deleteByCluster(clusterEntity);
			
			//ml 삭제
			log.info("ML 삭제. ML ID: {}", mlIdx);
			mlDomainService.delete(mlIdx);
		}
		return true;
	}
	
	/**
	 * ml 상세
	 * @param mlId
	 * @return
	 */
	public MLDto.Detail getMl(String mlId) {
		MLEntity entity = mlDomainService.get(mlId);
		
		List<MLResourceEntity> resEntitys = mlResourceDomainService.getList(entity.getId());		
		List<MLResourceDto> resources = new ArrayList<>();
		for(MLResourceEntity resEntity : resEntitys) {			
			MLResourceDto resDto = MLDtoMapper.INSTANCE.toResDto(resEntity);
			resources.add(resDto);
		}	
		
		MLDto.Detail detail = MLDtoMapper.INSTANCE.toDetailDto(entity);
		detail.setResources(resources);
		return detail;
	}
	
	/**
	 * ml 완료
	 * @param mlId
	 */
	public void finish(String mlId) {
		MLEntity entity = mlDomainService.get(mlId);
		entity.setUpdatedAt(DateUtil.currentDateTime());
		entity.setStatus("finished");
		mlDomainService.save(entity);
		
		//클러스터 삭제 처리
		mlClusterAPIService.deleteMlCluster(entity.getClusterIdx());
	}
	
	/**
	 * ML 리스트 반환.
	 * @param param
	 * @return
	 */
	public Object getMlList(ListArg param) {
		String userId = param.getUserId();
		String name = param.getName();
		
		List<MLEntity> list = mlDomainService.getList(userId, name);
		
		List<MLDto.ListDto> mlList = list.stream().map(c -> {
			return MLDtoMapper.INSTANCE.toListDto(c);
		}).collect(Collectors.toList());
		
		PageRequest pageRequest = param.getPageRequest();
		if(pageRequest != null) {
			Page<MLDto.ListDto> pages = new PageImpl<>(mlList, pageRequest.of(), mlList.size());
			return pages;
		} else {
			return mlList;
		}
	}
	
	
	/**
	 * 새로운 ID 생성.
	 * @return
	 */
	public String genMlId() {
		String uuid = UUID.randomUUID().toString();
		return uuid;
	}
	
	/**
	 * 배포하려는 적잘한 클러스터를 선택 또는 생성하여 ID 반환.
	 * @param yaml
	 * @return
	 */
	public ClusterEntity getCluster(String mlName, String stepCode, String yaml) {
		ClusterEntity clusterEntity = mlClusterAPIService.provisioningJobCluster(mlName, yaml);
		return clusterEntity;
	}
	
	
	private MLServiceInterface getServiceInterface(String kind) {
		return serviceFactory.getMLServiceInterface(kind);
	}
	
	
}
 