package kr.co.strato.portal.common.service;

import kr.co.strato.domain.cluster.model.ClusterEntity;
import kr.co.strato.domain.cluster.service.ClusterDomainService;
import kr.co.strato.domain.namespace.model.NamespaceEntity;
import kr.co.strato.domain.namespace.service.NamespaceDomainService;
import kr.co.strato.domain.project.model.ProjectEntity;
import kr.co.strato.domain.project.service.ProjectDomainService;
import kr.co.strato.portal.cluster.service.ClusterService;
import kr.co.strato.portal.common.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SelectService {
    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private ClusterDomainService clusterDomainService;

    @Autowired
    private NamespaceDomainService namespaceDomainService;

    public List<SelectDto> getSelectProjects(){
        List<ProjectEntity> projects = projectDomainService.getProjects();
        List<SelectDto> selectProjects =  projects.stream().map( e -> SelectDtoMapper.INSTANCE.toDto(e)).collect(Collectors.toList());

        return selectProjects;
    }

    public List<SelectDto> getSelectClusters(Long projectIdx){
        List<ClusterEntity> clusters = clusterDomainService.getListByProjectIdx(projectIdx);
        List<SelectDto> selectClusters = clusters.stream().map( e  -> SelectDtoMapper.INSTANCE.toDto(e)).collect(Collectors.toList());
        return selectClusters;
    }

    public List<SelectDto> getSelectNamespaces(Long clusterIdx){
        List<NamespaceEntity> namespaces = namespaceDomainService.findByClusterIdx(clusterIdx);
        List<SelectDto> selectNamespaces = namespaces.stream().map(e -> SelectDtoMapper.INSTANCE.toDto(e)).collect(Collectors.toList());
        return selectNamespaces;
    }

}