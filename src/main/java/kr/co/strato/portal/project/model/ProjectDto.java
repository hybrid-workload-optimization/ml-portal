package kr.co.strato.portal.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProjectDto {

	private Long id;
	private String projectName;
	private String description;
	private String createUserId;
	private String createUserName;
	private String createdAt;
	private String updateUserId;
	private String updateUserName;
	private String updatedAt;
	private String deletedYn;
	
	private int page;
	private int size;
	private String userId;
	private Long clusterCount;
	private Long userCount;
	private String projectUserName;
}