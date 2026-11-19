package com.cloudlabcopilot.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface KnowledgeCardMapper extends BaseMapper<KnowledgeCardEntity> {

        @Select("SELECT id, tech_stack, title, symptom, safe_next_step, verification_method, "
                        + "case_origin, source_type, source_ref, applicable_versions, status, review_status, "
                        + "created_at, updated_at "
                        + "FROM knowledge_card WHERE status = 'PUBLISHED' AND review_status = 'APPROVED' "
                        + "AND (CAST(#{techStack} AS VARCHAR) IS NULL OR CAST(#{techStack} AS VARCHAR) = '' "
                        + "     OR tech_stack = CAST(#{techStack} AS VARCHAR) OR tech_stack = 'OTHER') "
                        + "AND (title ILIKE #{like} OR symptom ILIKE #{like} OR safe_next_step ILIKE #{like}) "
                        + "ORDER BY updated_at DESC LIMIT 6")
        List<KnowledgeCardEntity> search(
                        @Param("techStack") String techStack,
                        @Param("keywords") String keywords,
                        @Param("like") String like);
}