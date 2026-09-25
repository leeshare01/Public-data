package com.eshop.user.controller;

import com.eshop.common.result.Result;
import com.eshop.user.entity.Address;
import com.eshop.user.service.AddressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Result<List<Address>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(addressService.list(userId));
    }

    @PostMapping
    public Result<Address> create(@RequestHeader("X-User-Id") Long userId, @RequestBody Address address) {
        return Result.success("新增成功", addressService.create(userId, address));
    }

    @PutMapping("/{id}")
    public Result<Address> update(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long id, @RequestBody Address address) {
        return Result.success("更新成功", addressService.update(userId, id, address));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        addressService.delete(userId, id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        addressService.setDefault(userId, id);
        return Result.success("已设为默认地址", null);
    }
}
