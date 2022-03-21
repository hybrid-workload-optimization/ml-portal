package kr.co.strato.portal.workload.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import kr.co.strato.domain.cronjob.model.CronJobEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CronJobDtoMapper {
	CronJobDtoMapper INSTANCE = Mappers.getMapper(CronJobDtoMapper.class);

	@Mappings({
		@Mapping(source = "cronJobIdx", target = "idx"), 
		@Mapping(source = "cronJobName", target = "name"),
		@Mapping(source = "cronJobUid", target = "uid"),
		@Mapping(source = "namespaceEntity.id", target = "namespaceIdx"),
		@Mapping(source = "namespaceEntity.name", target = "namespaceName")
	})
	@Named("toDto")
	public CronJobDto toDto(CronJobEntity entity);

	@Mappings({
		@Mapping(source = "idx", target = "cronJobIdx"),
		@Mapping(source = "name", target = "cronJobName"),
		@Mapping(source = "uid", target = "cronJobUid"),
		@Mapping(source = "namespaceIdx", target = "namespaceEntity.id"),
		@Mapping(source = "namespaceName", target = "namespaceEntity.name")
	})
	@Named("toEntity")
	public CronJobEntity toEntity(JobDto dto);
}