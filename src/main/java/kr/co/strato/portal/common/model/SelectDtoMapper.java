package kr.co.strato.portal.common.model;

import kr.co.strato.domain.cluster.model.ClusterEntity;
import kr.co.strato.domain.namespace.model.NamespaceEntity;
import kr.co.strato.domain.project.model.ProjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SelectDtoMapper {
    SelectDtoMapper INSTANCE = Mappers.getMapper(SelectDtoMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "text", source = "projectName")
    @Mapping(target = "value", source = "id")
    public SelectDto toDto(ProjectEntity entity);

    @Mapping(target = "id", source = "clusterIdx")
    @Mapping(target = "text", source = "clusterName")
    @Mapping(target = "value", source = "clusterIdx")
    public SelectDto toDto(ClusterEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "text", source = "name")
    @Mapping(target = "value", source = "id")
    public SelectDto toDto(NamespaceEntity entity);

}