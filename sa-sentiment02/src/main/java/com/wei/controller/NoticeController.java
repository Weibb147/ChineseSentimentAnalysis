package com.wei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.utils.AuthContext;
import com.wei.common.utils.ResultUtils;
import com.wei.mapper.UserMapper;
import com.wei.pojo.Notice;
import com.wei.pojo.User;
import com.wei.pojo.dto.NoticeDTO;
import com.wei.pojo.enums.NoticeType;
import com.wei.pojo.vo.NoticeVO;
import com.wei.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取公告列表(分页)
     * status: 可选，不传则返回所有（管理员用），传 "VISIBLE" 则只返回可见（用户用）
     */
    @GetMapping("/list")
    public ResultUtils<Page<NoticeVO>> getNoticeList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Page<Notice> page = noticeService.getNoticeList(pageNum, pageSize, status);

        // 获取发布人信息
        Set<Long> userIds = page.getRecords().stream().map(Notice::getCreatedBy).collect(Collectors.toSet());
        Map<Long, String> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername()));
        } else {
            userMap = Map.of();
        }

        Page<NoticeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(notice -> {
            NoticeVO vo = new NoticeVO();
            BeanUtils.copyProperties(notice, vo);
            // 手动转换枚举为String，确保前端接收到的是code
            if (notice.getType() != null) {
                vo.setType(notice.getType().getCode());
            }
            if (notice.getStatus() != null) {
                vo.setStatus(notice.getStatus().getCode());
            }
            // 设置发布人姓名
            if (notice.getCreatedBy() != null) {
                vo.setPublisher(userMap.getOrDefault(notice.getCreatedBy(), "管理员"));
            }
            return vo;
        }).toList());

        return ResultUtils.success(voPage);
    }

    /**
     * 创建公告 (管理员)
     */
    @PostMapping("/create")
    public ResultUtils<NoticeVO> createNotice(@Valid @RequestBody NoticeDTO dto) {
        Long userId = AuthContext.getCurrentUserId();
        Notice notice = noticeService.createNotice(dto.getTitle(), dto.getContent(), dto.getType(), userId);

        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            vo.setPublisher(user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername());
        }
        return ResultUtils.success(vo);
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    public ResultUtils<Void> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeDTO dto) {
        Notice notice = noticeService.getById(id);
        if (notice != null) {
            notice.setTitle(dto.getTitle());
            notice.setContent(dto.getContent());
            notice.setType(NoticeType.fromCode(dto.getType()));
            if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
                notice.setStatus(com.wei.pojo.enums.NoticeStatus.fromCode(dto.getStatus()));
            }
            noticeService.updateById(notice);
        }
        return ResultUtils.success();
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public ResultUtils<Void> deleteNotice(@PathVariable Long id) {
        noticeService.removeById(id);
        return ResultUtils.success();
    }
}
