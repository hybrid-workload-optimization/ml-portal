package kr.co.strato.portal.workload.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import kr.co.strato.adapter.k8s.common.model.ResourceType;
import kr.co.strato.adapter.k8s.pod.model.PodMapper;
import kr.co.strato.adapter.k8s.pod.service.PodAdapterService;
import kr.co.strato.adapter.k8s.statefulset.service.StatefulSetAdapterService;
import kr.co.strato.domain.pod.model.PodEntity;
import kr.co.strato.domain.pod.model.PodPersistentVolumeClaimEntity;
import kr.co.strato.domain.pod.service.PodDomainService;
import kr.co.strato.domain.statefulset.model.StatefulSetEntity;
import kr.co.strato.global.error.exception.InternalServerException;
import kr.co.strato.global.util.Base64Util;
import kr.co.strato.global.util.DateUtil;
import kr.co.strato.portal.workload.model.PodDto;
import kr.co.strato.portal.workload.model.PodDtoMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PodService {
    @Autowired
    private PodDomainService podDomainService;
    
    @Autowired
    private PodAdapterService podAdapterService;
    
    @Autowired
    private StatefulSetAdapterService statefulSetAdapterService;
    
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createPod(PodDto.ReqCreateDto reqCreateDto){
        Long clusterId = reqCreateDto.getClusterId();
        String yaml = Base64Util.decode(reqCreateDto.getYaml());

        List<Pod> pods = podAdapterService.create(clusterId, yaml);

        List<Long> ids = pods.stream().map( s -> {
        	try {
                String namespaceName = s.getMetadata().getNamespace();
                // ownerReferences 추가
                PodEntity pod = toEntity(s);

                Long id = podDomainService.register(pod, clusterId, namespaceName, null);

                return id;
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
                throw new InternalServerException("json 파싱 에러");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new InternalServerException("pod register error");
            }
        }).collect(Collectors.toList());

        return ids;
    }
    
    public Page<PodDto.ResListDto> getPods(Pageable pageable, PodDto.SearchParam searchParam) {
    	if (searchParam.getNamespaceId() == null && searchParam.getNodeId() == null) {
    		// clusterId만 파라미터로 보낸 경우 저장
    		Long clusterId = searchParam.getClusterId();
    		List<Pod> k8sPods = podAdapterService.getList(clusterId, null, null, null);
    		
    		// TODO Pod 삭제 (mapping table은 Cascade로...)
    		
    		// TODO 왜 건너뛰는거지
    		List<Long> ids = k8sPods.stream().map( s -> {
    			try {
    				PodEntity pod = PodMapper.INSTANCE.toEntity(s);
    				String namespaceName = pod.getNamespace().getName();
    				String kind = pod.getKind();
    				Long id = podDomainService.register(pod, clusterId, namespaceName, kind);
    				return id;
    			} catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new InternalServerException("pod register error");
                }
    		}).collect(Collectors.toList());
    	}
    	// TODO k8s 데이터 저장
    	
    	
    	
    	Page<PodEntity> pods = podDomainService.getPods(pageable, searchParam.getProjectId(), searchParam.getClusterId(), searchParam.getNamespaceId(), searchParam.getNodeId());
        List<PodDto.ResListDto> dtos = pods.stream().map(e -> PodDtoMapper.INSTANCE.toResListDto(e)).collect(Collectors.toList());
        Page<PodDto.ResListDto> pages = new PageImpl<>(dtos, pageable, pods.getTotalElements());
        
    	return pages;
    }
    
    /**
     * k8s pod model -> pod entity
     * @param pod
     * @return
     * @throws JsonProcessingException
     */
    @Transactional(rollbackFor = Exception.class)
    private PodEntity toEntity(Pod pod) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String name = pod.getMetadata().getName();
        String uid = pod.getMetadata().getUid();
        String label = mapper.writeValueAsString(pod.getMetadata().getLabels());
        String annotations = mapper.writeValueAsString(pod.getMetadata().getAnnotations());
        String createAt = pod.getMetadata().getCreationTimestamp();
        String ownerUid = pod.getMetadata().getOwnerReferences().get(0).getUid();

        PodEntity podEntity = PodEntity.builder()
                .podName(name)
                .podUid(uid)
                .label(label)
                .ownerUid(ownerUid)
                .annotation(annotations)
                .createdAt(DateUtil.strToLocalDateTime(createAt))
                .build();

        return podEntity;
    }
    
    public PodDto.ResDetailDto getPodDetail(Long podId){
    	// get pod entity
    	PodEntity entity = podDomainService.get(podId);
    	
    	PodDto.ResDetailDto dto = PodDtoMapper.INSTANCE.toResDetailDto(entity);
    	return dto;
    }
    
    public PodDto.ResOwnerDto getPodOwnerInfo(Long podId, String resourceType) {
    	PodDto.ResOwnerDto dto = new PodDto.ResOwnerDto();
    	if (resourceType.equals(ResourceType.statefulSet.get())) {
    		StatefulSetEntity entity = podDomainService.getPodStatefulSet(podId);
    		
    		Long clusterId = entity.getNamespace().getClusterIdx().getClusterId();
    		String namespace = entity.getNamespace().getName();
    		String statefulSetName = entity.getPodStatefulSets().get(0).getStatefulSet().getStatefulSetName();
    		
    		StatefulSet k8sStatefulSet = statefulSetAdapterService.get(clusterId, namespace, statefulSetName);
    		
    		dto = PodDtoMapper.INSTANCE.toResStatefulSetOwnerInfoDto(entity, k8sStatefulSet, resourceType);
    	}
    	
    	
    	return dto;
    }
    
    public ResponseEntity<ByteArrayResource> getLogDownloadFile(Long clusterId, String namespaceName, String podName) {
    	ResponseEntity<ByteArrayResource> result = podAdapterService.getLogDownloadFile(clusterId, namespaceName, podName);
    	return result;
    }
    
    public List<PodDto.ResListDto> getPodOwnerPodList(Long clusterId, PodDto.OwnerSearchParam searchParam) {
		String nodeName = searchParam.getNodeName();
		String ownerUid = searchParam.getOwnerUid();
		String namespace = searchParam.getNamespaceName();
    	List<Pod> k8sPods = podAdapterService.getList(clusterId, nodeName, ownerUid, namespace);
    	List<PodDto.ResListDto> dtoList = new ArrayList<>();
    	
    	for(Pod k8sPod : k8sPods) {
    		PodEntity podEntity = PodMapper.INSTANCE.toEntity(k8sPod);
    		
    		PodDto.ResListDto dto = PodDtoMapper.INSTANCE.toResK8sListDto(podEntity);
    		dtoList.add(dto);
    	}
    	
    	return dtoList;
    }
}
