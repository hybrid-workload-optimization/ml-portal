package kr.co.strato.portal.setting.model;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import kr.co.strato.domain.setting.model.SettingEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ToolsDtoMapper {
	ToolsDtoMapper INSTANCE = Mappers.getMapper(ToolsDtoMapper.class);
	
	@Mappings({
		@Mapping(source = "idx",      target = "settingIdx"),
		@Mapping(source = "type",     target = "settingType"),
		@Mapping(source = "key",      target = "settingKey"),
		@Mapping(source = "value",    target = "settingValue")
	})
	@Named("toolsDtoToEntity")
	public SettingEntity toEntity(ToolsDto dto);
	
	@Mappings({
		@Mapping(source = "settingIdx",      target = "idx"),
		@Mapping(source = "settingType",     target = "type"),
		@Mapping(source = "settingKey",      target = "key"),
		@Mapping(source = "settingValue",    target = "value")
	})
	public ToolsDto toDto(SettingEntity entity);
	
	@IterableMapping(qualifiedByName = "toolsDtoToEntity")
	public List<SettingEntity> toEntityList(List<ToolsDto> dtoList);
}
