package com.wei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wei.pojo.Notice;

public interface NoticeService extends IService<Notice> {
    /**
     * 分页查询公告列表
     * @param status 公告状态 (可选，null表示所有)
     */
    Page<Notice> getNoticeList(int pageNum, int pageSize, String status);

    /**
     * 创建公告
     */
    Notice createNotice(String title, String content, String type, Long createdBy);
}
