package com.eshop.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eshop.common.exception.BusinessException;
import com.eshop.common.result.ResultCode;
import com.eshop.user.entity.Address;
import com.eshop.user.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址管理
 */
@Service
public class AddressService {

    private final AddressMapper addressMapper;

    public AddressService(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public List<Address> list(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault).orderByDesc(Address::getCreateTime));
    }

    @Transactional
    public Address create(Long userId, Address address) {
        address.setUserId(userId);
        if (address.getIsDefault() == null) address.setIsDefault(0);
        if (address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public Address update(Long userId, Long id, Address address) {
        Address existing = getById(userId, id);
        if (address.getReceiverName() != null) existing.setReceiverName(address.getReceiverName());
        if (address.getReceiverPhone() != null) existing.setReceiverPhone(address.getReceiverPhone());
        if (address.getProvince() != null) existing.setProvince(address.getProvince());
        if (address.getCity() != null) existing.setCity(address.getCity());
        if (address.getDistrict() != null) existing.setDistrict(address.getDistrict());
        if (address.getDetailAddress() != null) existing.setDetailAddress(address.getDetailAddress());
        if (address.getIsDefault() != null) {
            if (address.getIsDefault() == 1) clearDefault(userId);
            existing.setIsDefault(address.getIsDefault());
        }
        addressMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        getById(userId, id);
        addressMapper.deleteById(id);
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        getById(userId, id);
        clearDefault(userId);
        Address addr = new Address();
        addr.setId(id);
        addr.setIsDefault(1);
        addressMapper.updateById(addr);
    }

    private void clearDefault(Long userId) {
        Address defaultAddr = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId).eq(Address::getIsDefault, 1));
        if (defaultAddr != null) {
            defaultAddr.setIsDefault(0);
            addressMapper.updateById(defaultAddr);
        }
    }

    private Address getById(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }
}
