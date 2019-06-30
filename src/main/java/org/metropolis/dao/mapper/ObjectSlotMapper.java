package org.metropolis.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.metropolis.dao.model.ObjectSlot;

import java.util.List;

public interface ObjectSlotMapper {
    int deleteByPrimaryKey(Integer objId);

    int insertSelective(ObjectSlot record);

    ObjectSlot selectByPrimaryKey(Integer objId);

    int updateByPrimaryKeySelective(ObjectSlot record);

    int updateByPrimaryKey(ObjectSlot record);

    List<ObjectSlot> listByType(@Param("filter") ObjectSlot filter, @Param("limit") int limit);
}