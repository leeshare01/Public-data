package com.eshop.user.controller;

import com.eshop.common.dto.PageParam;
import com.eshop.common.dto.PageResult;
import com.eshop.common.result.Result;
import com.eshop.common.result.ResultCode;
import com.eshop.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String phone = body.get("phone");
        String email = body.get("email");
        String nickname = body.get("nickname");

        if (username == null || password == null) {
            return Result.error(ResultCode.BAD_REQUEST, "用户名和密码不能为空");
        }
        return Result.success("注册成功", userService.register(username, password, phone, email, nickname));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String loginType = body.get("loginType");
        String account = body.get("account");
        String password = body.get("password");

        if (account == null || password == null) {
            return Result.error(ResultCode.BAD_REQUEST, "账号和密码不能为空");
        }
        return Result.success("登录成功", userService.login(account, password));
    }

    /** 获取当前用户信息 */
    @GetMapping("/me")
    public Result<Map<String, Object>> getMe(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    /** 更新个人信息 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                      @RequestBody Map<String, Object> body) {
        userService.updateProfile(userId, body);
        return Result.success("更新成功", null);
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestHeader("X-User-Id") Long userId,
                                       @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error(ResultCode.BAD_REQUEST, "原密码和新密码不能为空");
        }
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功", null);
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId) {
        // 后续可加入 Redis Token 黑名单机制
        return Result.success("已退出", null);
    }

    // ========== 管理员接口 ==========

    /** 管理员 — 用户总数 */
    @GetMapping("/admin/count")
    public Result<Long> adminCount(@RequestHeader(value = "X-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        return Result.success(userService.count());
    }

    /** 管理员 — 用户列表 */
    @GetMapping("/admin/list")
    public Result<PageResult<Map<String, Object>>> adminList(
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!"ADMIN".equals(role)) return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        PageParam pageParam = new PageParam();
        pageParam.setPage(page);
        pageParam.setSize(size);
        return Result.success(userService.adminPage(pageParam));
    }
}
