package com.cloudlabcopilot.session.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudlabcopilot.session.infrastructure.entity.SessionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SessionMapper extends BaseMapper<SessionEntity> {

    @Select("SELECT * FROM lab_session WHERE session_id = #{sessionId} FOR UPDATE")
    SessionEntity selectForUpdate(@Param("sessionId") String sessionId);

    @Update("UPDATE lab_session SET current_step_no = #{nextStepNo}, last_active_at = now(), "
            + "row_version = row_version + 1, updated_at = now() "
            + "WHERE session_id = #{sessionId} AND current_step_no = #{expectStepNo} "
            + "AND status = 'IN_PROGRESS'")
    int casAdvance(
            @Param("sessionId") String sessionId,
            @Param("expectStepNo") int expectStepNo,
            @Param("nextStepNo") int nextStepNo);

    @Select("SELECT COUNT(*) FROM lab_session WHERE lab_id = #{labId} AND operator_id = #{operatorId} "
            + "AND status IN ('IN_PROGRESS','PAUSED')")
    long countActive(@Param("labId") Long labId, @Param("operatorId") String operatorId);

    @Select("SELECT COUNT(*) FROM lab_session WHERE lab_id = #{labId}")
    long countByLab(@Param("labId") Long labId);

    @Select("SELECT COUNT(*) FROM lab_session WHERE lab_id = #{labId} AND version_no = #{versionNo} "
            + "AND status IN ('IN_PROGRESS','PAUSED')")
    long countActiveByVersion(@Param("labId") Long labId, @Param("versionNo") int versionNo);
}