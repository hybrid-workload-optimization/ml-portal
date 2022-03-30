package kr.co.strato.domain.ingress.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.strato.domain.cronjob.model.QCronJobEntity;
import kr.co.strato.domain.deployment.model.QDeploymentEntity;
import kr.co.strato.domain.ingress.model.IngressEntity;
import kr.co.strato.domain.ingress.model.QIngressEntity;
import kr.co.strato.domain.ingress.model.QIngressRuleEntity;
import kr.co.strato.domain.job.model.QJobEntity;
import kr.co.strato.domain.namespace.model.QNamespaceEntity;
import kr.co.strato.domain.persistentVolumeClaim.model.QPersistentVolumeClaimEntity;
import kr.co.strato.domain.pod.model.QPodEntity;
import kr.co.strato.domain.pod.model.QPodJobEntity;
import kr.co.strato.domain.pod.model.QPodPersistentVolumeClaimEntity;
import kr.co.strato.domain.pod.model.QPodReplicaSetEntity;
import kr.co.strato.domain.pod.model.QPodStatefulSetEntity;
import kr.co.strato.domain.replicaset.model.QReplicaSetEntity;
import kr.co.strato.domain.service.model.QServiceEndpointEntity;
import kr.co.strato.domain.service.model.QServiceEntity;
import kr.co.strato.domain.statefulset.model.QStatefulSetEntity;

public class CustomIngressRepositoryImpl implements CustomIngressRepository{
    private final JPAQueryFactory jpaQueryFactory;

    public CustomIngressRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public Page<IngressEntity> getIngressList(Pageable pageable,String name,Long namespaceId) {

        QIngressEntity qIngressEntity = QIngressEntity.ingressEntity;
        QNamespaceEntity qNamespaceEntity = QNamespaceEntity.namespaceEntity;


        BooleanBuilder builder = new BooleanBuilder();
        if(namespaceId != null && namespaceId > 0L){
            builder.and(qNamespaceEntity.id.eq(namespaceId));
        }

        QueryResults<IngressEntity> results =
                jpaQueryFactory
                        .select(qIngressEntity)
                        .from(qIngressEntity)
                        .leftJoin(qIngressEntity.namespace, qNamespaceEntity)
                        .where(builder)
                        .orderBy(qIngressEntity.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetchResults();

        List<IngressEntity> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }
    
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIngress(Long ingressId) {
    	  QIngressEntity qIngressEntity = QIngressEntity.ingressEntity;
    	  QIngressRuleEntity qIngressRuleEntity = QIngressRuleEntity.ingressRuleEntity;
    	  
    	  jpaQueryFactory.delete(qIngressRuleEntity)
          .where(qIngressRuleEntity.ingress.id.eq(ingressId))
          .execute();
    	  
    	  
          jpaQueryFactory.delete(qIngressEntity)
          .where(qIngressEntity.id.eq(ingressId))
          .execute();
    	  
    }

}
