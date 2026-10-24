package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.exception.AddressBookBusinessException;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        addressBook.setUserId(requireCurrentUserId());
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setId(null);
        addressBook.setUserId(requireCurrentUserId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        Long userId = requireCurrentUserId();
        AddressBook addressBook = addressBookMapper.getById(id);
        if (addressBook == null || !userId.equals(addressBook.getUserId())) {
            throw new AddressBookBusinessException("地址不存在");
        }
        return addressBook;
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        getById(addressBook.getId());
        addressBook.setUserId(requireCurrentUserId());
        // 默认地址只由 setDefault 修改，避免普通编辑绕过默认切换规则。
        addressBook.setIsDefault(null);
        addressBookMapper.update(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        AddressBook owned = getById(addressBook.getId());
        // 在清除旧默认地址前校验归属，使用服务端实体忽略请求中其余字段。
        addressBook = new AddressBook();
        addressBook.setId(owned.getId());
        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(requireCurrentUserId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        getById(id);
        addressBookMapper.deleteOwnedById(id, requireCurrentUserId());
    }

    private Long requireCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new AddressBookBusinessException("用户未登录");
        }
        return userId;
    }

}
