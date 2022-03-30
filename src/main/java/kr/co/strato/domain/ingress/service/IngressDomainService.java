package kr.co.strato.domain.ingress.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import kr.co.strato.domain.cluster.model.ClusterEntity;
import kr.co.strato.domain.ingress.model.IngressControllerEntity;
import kr.co.strato.domain.ingress.model.IngressEntity;
import kr.co.strato.domain.ingress.repository.IngressControllerRepository;
import kr.co.strato.domain.ingress.repository.IngressRepository;
import kr.co.strato.domain.namespace.model.NamespaceEntity;
import kr.co.strato.domain.namespace.repository.NamespaceRepository;
import kr.co.strato.domain.storageClass.model.StorageClassEntity;
import kr.co.strato.global.error.exception.NotFoundResourceException;

@Service
public class IngressDomainService {
	
	@Autowired
	private IngressRepository ingressRepository;
	@Autowired
	private NamespaceRepository namespaceRepository;
	
	
	@Autowired
	private IngressControllerRepository ingressControllerRepository;
	
	public Long register(IngressEntity ingressEntity) {
		ingressRepository.save(ingressEntity);
		return ingressEntity.getId();
	}
	public Page<IngressEntity> getList(Pageable pageable) {
		return ingressRepository.findAll(pageable);
	}
	
	public Page<IngressEntity> getIngressList(Pageable pageable,String name,Long namespaceId) {
		return ingressRepository.getIngressList(pageable,name, namespaceId);
	}
	
	public boolean delete(Long id) {
		ingressRepository.deleteIngress(id);
		return true;
	}
	
	public IngressEntity getDetail(Long id) {
		Optional<IngressEntity> ingress = ingressRepository.findById(id);
		if (ingress.isPresent()) {
			return ingress.get();
		} else {
			throw new NotFoundResourceException(id.toString());
		}
	}
	
    public Long update(IngressEntity updateEntity,Long ingressId) {
    	IngressEntity oldEntity = get(ingressId);
        changeToNewData(oldEntity, updateEntity);
    	ingressRepository.save(oldEntity);
		return oldEntity.getId();
	}
    
	public IngressControllerEntity findByDefaultYn(String defaultYn) {
		IngressControllerEntity ingressDefault = ingressControllerRepository.findByDefaultYn(defaultYn);
		return ingressDefault;
	}
	
	public IngressControllerEntity findIngressControllerByName(String name) {
		IngressControllerEntity ingressDefault = ingressControllerRepository.findByName(name);
		return ingressDefault;
	}
	
	public NamespaceEntity findByName(String name){
		NamespaceEntity namespaceEntity = namespaceRepository.findByName(name);
		return namespaceEntity;
	}
	public IngressEntity get(Long ingressId) {
		IngressEntity namespaceEntity = ingressRepository.findById(ingressId)
				.orElseThrow(() -> new NotFoundResourceException("ingress id:" + ingressId));

		return namespaceEntity;
	}

	private void changeToNewData(IngressEntity oldEntity, IngressEntity newEntity) {
		oldEntity.setUid(newEntity.getUid());
		oldEntity.setCreatedAt(newEntity.getCreatedAt());
		oldEntity.setIngressClass(newEntity.getIngressClass());
	}
	
	
}
