package org.metro.dao.model;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * <p> Created by pengshuolin on 2019/6/8
 */
@Data
public class PageEntity {

    public static final Integer defaultPageSize = 10;

    private Integer pageNo;
    private Integer pageSize;
    private Integer pageCount;
    private Integer total;
    private List<?> data;

    public static PageEntity of(PageFilter page, Integer total, List<?> data) {
        Integer pageNo = ObjectUtils.defaultIfNull(page.getPageNo(), 0);
        Integer pageSize = ObjectUtils.defaultIfNull(page.getPageSize(), defaultPageSize);
        return new PageEntity(total, pageSize, pageNo, data);
    }

    public static PageEntity empty() {
        return new PageEntity(0, defaultPageSize, 0, Collections.emptyList());
    }

    private PageEntity(Integer total, Integer pageSize, Integer pageNo, List<?> list) {
        this.total = total;
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.pageCount = (total / pageSize) + Math.min(1, total % pageSize);
        this.data = list;
    }
}
