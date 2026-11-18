package com.cloudlabcopilot.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface KnowledgeCardMapper extends BaseMapper<KnowledgeCardEntity> {

    @Select("SELECT * FROM knowledge_card WHERE status = 'PUBLISHED' AND review_status = 'APPROVED' "
            + "AND (#{techStack} IS NULL OR #{techStack} = '' OR tech_stack = #{techStack} OR tech_stack = 'OTHER') "
            + "AND (title ILIKE #{like} OR symptom ILIKE #{like} OR safe_next_step ILIKE #{like} "
            + "     OR search_vector @@ plainto_tsquery('simple', #{keywords})) "
            + "ORDER BY updated_at DESC LIMIT 6")
    List<KnowledgeCardEntity> search(
            @Param("techStack") String techStack,
            @Param("keywords") String keywords,
            @Param("like") String like);
}