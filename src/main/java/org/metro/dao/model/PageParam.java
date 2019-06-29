package org.metro.dao.model;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import javax.validation.Valid;

/**
 * <p> Created by pengshuolin on 2019/6/8
 */
@Data
public class PageParam<T> {

    @Valid
    private T filter;
    private Integer pageNo;
    private Integer pageSize;

    public Integer getPageNo() {
        return ObjectUtils.defaultIfNull(pageNo, 0);
    }

    public Integer getPageSize() {
        return ObjectUtils.defaultIfNull(pageSize, 10);
    }
}
