package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wei.mapper.NoticeMapper;
import com.wei.pojo.Notice;
import com.wei.pojo.enums.NoticeStatus;
import com.wei.pojo.enums.NoticeType;
import com.wei.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public Page<Notice> getNoticeList(int pageNum, int pageSize, String status) {
        Page<Notice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Notice::getStatus, NoticeStatus.fromCode(status));
        }
        wrapper.orderByDesc(Notice::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public Notice createNotice(String title, String content, String type, Long createdBy) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(NoticeType.fromCode(type));
        notice.setCreatedBy(createdBy);
        notice.setStatus(NoticeStatus.VISIBLE);
        save(notice);
        return notice;
    }
}
