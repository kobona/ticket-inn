package org.metro.dao.model;

import lombok.Data;

import javax.validation.Valid;

/**
 * <p> Created by pengshuolin on 2019/6/8
 */
@Data
public class PageFilter<T> {

    @Valid
    private T filter;
    private Integer pageNo;
    private Integer pageSize;

}
