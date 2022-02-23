package kr.co.strato.portal.cluster.service;

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

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import kr.co.strato.adapter.k8s.common.model.YamlApplyParam;
import kr.co.strato.adapter.k8s.storageClass.service.StorageClassAdapterService;
import kr.co.strato.domain.cluster.model.ClusterEntity;
import kr.co.strato.domain.storageClass.model.StorageClassEntity;
import kr.co.strato.domain.storageClass.service.StorageClassDomainService;
import kr.co.strato.global.error.exception.InternalServerException;
import kr.co.strato.global.util.DateUtil;
import kr.co.strato.portal.cluster.model.ClusterStorageClassDto;
import kr.co.strato.portal.cluster.model.ClusterStorageClassDtoMapper;

@Service
public class ClusterStorageClassService {

	@Autowired
	private StorageClassAdapterService storageClassAdapterService;
	@Autowired
	private StorageClassDomainService storageClassDomainService;
	
	
	public Page<ClusterStorageClassDto> getClusterStorageClassList(String name,Pageable pageable) {
		Page<StorageClassEntity> storageClassPage = storageClassDomainService.findByName(name,pageable);
		List<ClusterStorageClassDto> storageClassList = storageClassPage.getContent().stream().map(c -> ClusterStorageClassDtoMapper.INSTANCE.toDto(c)).collect(Collectors.toList());
		
		Page<ClusterStorageClassDto> page = new PageImpl<>(storageClassList, pageable, storageClassPage.getTotalElements());
		return page;
	}

	@Transactional(rollbackFor = Exception.class)
	public List<StorageClass> getClusterStorageClassListSet(Integer clusterId) {
		List<StorageClass> storageClassList = storageClassAdapterService.getStorageClassList(clusterId);
		
		synClusterStorageClassSave(storageClassList,clusterId);
		return storageClassList;
	}

	public List<Long> synClusterStorageClassSave(List<StorageClass> storageClassList, Integer clusterId) {
		List<Long> ids = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		for (StorageClass sc : storageClassList) {
			try {
				// k8s Object -> Entity
				String name = sc.getMetadata().getName();
				String uid = sc.getMetadata().getUid();
				String createdAt = sc.getMetadata().getCreationTimestamp();
				String provider = sc.getProvisioner();
				String type = sc.getParameters().get("type");
				
				String annotations = mapper.writeValueAsString(sc.getMetadata().getAnnotations());
				String label = mapper.writeValueAsString(sc.getMetadata().getLabels());
				
				ClusterEntity clusterEntity = new ClusterEntity();
				clusterEntity.setClusterIdx(Integer.toUnsignedLong(clusterId));
				
				StorageClassEntity clusterStorageClass = StorageClassEntity.builder().name(name).uid(uid)
						.createdAt(DateUtil.strToLocalDateTime(createdAt))
						.provider(provider).type(type)
						.clusterIdx(clusterEntity)
						.annotation(annotations).label(label)
						.build();

				// save
				Long id = storageClassDomainService.register(clusterStorageClass);
				ids.add(id);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				throw new InternalServerException("parsing error");
			}
		}

		return ids;
	}
	
	public void deleteClusterStorageClass(Integer clusterId, StorageClassEntity storageClassEntity) throws Exception {
		storageClassDomainService.delete(storageClassEntity);
		storageClassAdapterService.deleteStorageClass(clusterId, storageClassEntity.getName());
	}
	
    public ClusterStorageClassDto getClusterStorageClassDetail(Long id){
    	StorageClassEntity storageClassEntity = storageClassDomainService.getDetail(id); 

    	ClusterStorageClassDto clusterStorageClassDto = ClusterStorageClassDtoMapper.INSTANCE.toDto(storageClassEntity);
        return clusterStorageClassDto;
    }
	
	
    public String getClusterStorageClassYaml(Integer kubeConfigId,String name){
     	String namespaceYaml = storageClassAdapterService.getStorageClassYaml(kubeConfigId,name); 
         return namespaceYaml;
     }
    
	
	public List<Long> registerClusterStorageClass(YamlApplyParam yamlApplyParam, Integer clusterId) {
		List<StorageClass> storageClassList = storageClassAdapterService.registerStorageClass(yamlApplyParam.getKubeConfigId(),
				yamlApplyParam.getYaml());
		List<Long> ids = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();
		for (StorageClass sc : storageClassList) {
			try {
				// k8s Object -> Entity
				String name = sc.getMetadata().getName();
				String uid = sc.getMetadata().getUid();
				String createdAt = sc.getMetadata().getCreationTimestamp();
				String provider = sc.getProvisioner();
				String type = sc.getParameters().get("type");
				
				String annotations = mapper.writeValueAsString(sc.getMetadata().getAnnotations());
				String label = mapper.writeValueAsString(sc.getMetadata().getLabels());
				
				ClusterEntity clusterEntity = new ClusterEntity();
				clusterEntity.setClusterIdx(Integer.toUnsignedLong(clusterId));
				
				StorageClassEntity clusterStorageClass = StorageClassEntity.builder().name(name).uid(uid)
						.createdAt(DateUtil.strToLocalDateTime(createdAt))
						.provider(provider).type(type)
						.clusterIdx(clusterEntity)
						.annotation(annotations).label(label)
						.build();

				// save
				Long id = storageClassDomainService.register(clusterStorageClass);
				ids.add(id);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				throw new InternalServerException("parsing error");
			}
		}

		return ids;
	}

}
