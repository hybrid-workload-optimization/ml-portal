package kr.co.strato.portal.ml.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageData {
	public static final String JOB_TYPE_PROVISIONING = "provisioning";
	public static final String JOB_TYPE_SCALE = "scale";
	public static final String JOB_TYPE_MODIFY = "modify";
	public static final String JOB_TYPE_DELETE = "delete";
	
	
	private Long workJobIdx;
	private String callbackUrl;
	private String jobType;
	private Object param;

	@Override
	public String toString() {
		return "MessageData [workJobIdx=" + workJobIdx + ", callbackUrl=" + callbackUrl + ", jobType=" + jobType
				+ ", param=" + param + "]";
	}	
	
}
