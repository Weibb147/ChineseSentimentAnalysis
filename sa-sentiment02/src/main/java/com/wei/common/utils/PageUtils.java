package com.wei.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageUtils<T> {
    private long total;   // 总记录数
    private long pages;   // 总页数
    private long current; // 当前页
    private long size;    // 每页大小
    private List<T> records; // 数据列表

    public static <E> PageUtils<E> from(IPage<E> page) {
        PageUtils<E> result = new PageUtils<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }
}