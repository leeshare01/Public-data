package com.eshop.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eshop.common.constant.AuthConstant;
import com.eshop.common.exception.BusinessException;
import com.eshop.common.result.ResultCode;
import com.eshop.common.dto.PageParam;
import com.eshop.common.dto.PageResult;
import com.eshop.user.entity.User;
import com.eshop.user.mapper.UserMapper;
import com.eshop.user.util.JwtUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 注册
     */
    @Transactional
    public Map<String, Object> register(String username, String password, String phone, String email, String nickname) {
        // 检查用户名是否已存在
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        // 检查邮箱是否已存在
        if (email != null && !email.isEmpty()
                && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone != null ? phone : "");
        user.setEmail(email != null ? email : "");
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(AuthConstant.ROLE_USER);
        user.setStatus(1);
        userMapper.insert(user);

        return Map.of("userId", user.getId(), "username", user.getUsername());
    }

    /**
     * 登录
     */
    public Map<String, Object> login(String account, String password) {
        // 支持用户名/邮箱/手机号登录
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or().eq(User::getEmail, account)
                .or().eq(User::getPhone, account));

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "role", user.getRole()
        );
    }

    /**
     * 获取用户信息
     */
    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "gender", user.getGender() != null ? user.getGender() : 0,
                "role", user.getRole()
        );
    }

    /**
     * 更新个人信息
     */
    public void updateProfile(Long userId, Map<String, Object> profile) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (profile.containsKey("nickname")) user.setNickname((String) profile.get("nickname"));
        if (profile.containsKey("phone")) user.setPhone((String) profile.get("phone"));
        if (profile.containsKey("email")) user.setEmail((String) profile.get("email"));
        if (profile.containsKey("avatar")) user.setAvatar((String) profile.get("avatar"));
        if (profile.containsKey("gender")) user.setGender((Integer) profile.get("gender"));
        userMapper.updateById(user);
    }

    /**
     * 用户总数
     */
    public long count() {
        return userMapper.selectCount(null);
    }

    /**
     * 管理员分页查询用户列表（不返回密码）
     */
    public PageResult<Map<String, Object>> adminPage(PageParam pageParam) {
        IPage<User> page = userMapper.selectPage(
                new Page<>(pageParam.getPage(), pageParam.getSize()), null);
        List<Map<String, Object>> records = page.getRecords().stream().map(u ->
            Map.<String, Object>of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nickname", u.getNickname() != null ? u.getNickname() : "",
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "email", u.getEmail() != null ? u.getEmail() : "",
                "role", u.getRole(),
                "status", u.getStatus()
            )
        ).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 修改密码
     */
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
