package kr.co.strato.portal.ml.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MLProjectDto {
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
	
	private String userId;
	private String userName;
	private Long clusterCount;
	private Long userCount;
	private String projectPmName;
	private String projectPmId;
	private String projectPmEmail;
	
	private String fresh;
	private String owner;
	
	private String type;
	private Long mlCount;
}
