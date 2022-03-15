package kr.co.strato.domain.pod.repository;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.co.strato.domain.pod.model.PodEntity;
import kr.co.strato.domain.statefulset.model.StatefulSetEntity;
import kr.co.strato.portal.workload.model.PodDto;

public interface CustomPodRepository {
	public Page<PodEntity> getPodList(Pageable pageable, Long projectId, Long clusterId, Long namespaceId, Long nodeId);
	
	public StatefulSetEntity getPodStatefulSet(Long podId);
}
