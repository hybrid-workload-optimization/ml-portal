package kr.co.strato.portal.networking.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import kr.co.strato.domain.ingress.model.IngressEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngressDtoMapper {

	IngressDtoMapper INSTANCE = Mappers.getMapper(IngressDtoMapper.class);
	
	@Mapping(target = "namespaceIdx" , source = "namespace.id")
	@Mapping(target = "clusterIdx" , source = "cluster.clusterIdx")
	public IngressDto toDto(IngressEntity ingressEntity);
	
}