package com.wei.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.utils.AuthContext;
import com.wei.common.utils.ResultUtils;
import com.wei.mapper.UserMapper;
import com.wei.pojo.User;
import com.wei.pojo.UserFeedback;
import com.wei.pojo.dto.FeedbackDTO;
import com.wei.pojo.enums.FeedbackCategory;
import com.wei.pojo.vo.FeedbackVO;
import com.wei.service.UserFeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/submit")
    public ResultUtils<FeedbackVO> submit(@Valid @RequestBody FeedbackDTO dto) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("请先登录");
        }
        UserFeedback fb = userFeedbackService.submitFeedback(userId, dto.getCategory(), dto.getContent());
        FeedbackVO vo = toVOWithUser(fb);
        return ResultUtils.success(vo);
    }
    @PutMapping("/{id}/reply")
    public ResultUtils<Void> reply(@PathVariable Long id, @RequestBody String reply) {
        String role = AuthContext.getCurrentUserRole();
        if (role == null || !"ADMIN".equals(role)) {
            return ResultUtils.error(403, "没有操作权限");
        }
        userFeedbackService.replyFeedback(id, reply);
        return ResultUtils.success();
    }

    @GetMapping("/my")
    public ResultUtils<Page<FeedbackVO>> myList(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("请先登录");
        }
        Page<UserFeedback> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFeedback::getUserId, userId)
               .orderByDesc(UserFeedback::getCreatedAt);
        Page<UserFeedback> fbPage = userFeedbackService.page(page, wrapper);

        Page<FeedbackVO> voPage = new Page<>(fbPage.getCurrent(), fbPage.getSize(), fbPage.getTotal());
        voPage.setRecords(fbPage.getRecords().stream().map(this::toVOWithUser).toList());
        return ResultUtils.success(voPage);
    }

    @GetMapping("/all")
    public ResultUtils<Page<FeedbackVO>> all(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String category) {
        String role = AuthContext.getCurrentUserRole();
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            return ResultUtils.error(403, "没有操作权限");
        }
        Page<UserFeedback> fbPage = userFeedbackService.getFeedbacks(pageNum, pageSize, status, category);
        Page<FeedbackVO> voPage = new Page<>(fbPage.getCurrent(), fbPage.getSize(), fbPage.getTotal());
        voPage.setRecords(fbPage.getRecords().stream().map(this::toVOWithUser).toList());
        return ResultUtils.success(voPage);
    }



    @PutMapping("/{id}")
    public ResultUtils<Void> update(@PathVariable Long id, @Valid @RequestBody FeedbackDTO dto) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("请先登录");
        }
        UserFeedback fb = userFeedbackService.getById(id);
        if (fb == null || !userId.equals(fb.getUserId())) {
            return ResultUtils.error(403, "没有操作权限");
        }
        fb.setCategory(FeedbackCategory.fromCode(dto.getCategory()));
        fb.setContent(dto.getContent());
        userFeedbackService.updateById(fb);
        return ResultUtils.success();
    }

    @DeleteMapping("/{id}")
    public ResultUtils<Void> delete(@PathVariable Long id) {
        Long userId = AuthContext.getCurrentUserId();
        String role = AuthContext.getCurrentUserRole();
        if (userId == null) {
            return ResultUtils.error("请先登录");
        }
        UserFeedback fb = userFeedbackService.getById(id);
        if (fb == null) {
            return ResultUtils.success();
        }
        if (!userId.equals(fb.getUserId()) && (role == null || !"ADMIN".equals(role))) {
            return ResultUtils.error(403, "没有操作权限");
        }
        userFeedbackService.removeById(id);
        return ResultUtils.success();
    }

    private FeedbackVO toVOWithUser(UserFeedback fb) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(fb, vo);
        vo.setCategory(fb.getCategory() != null ? fb.getCategory().getCode() : null);
        vo.setStatus(fb.getStatus() != null ? fb.getStatus().getCode() : null);
        if (fb.getUserId() != null) {
            User u = userMapper.selectById(fb.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setNickname(u.getNickname());
                vo.setPhone(u.getPhone());
                vo.setEmail(u.getEmail());
                vo.setImageUrl(u.getImageUrl());
            }
        }
        return vo;
    }
}
