package com.cinema.service;

import com.cinema.dao.CustomerProfileDao;
import com.cinema.model.CustomerProfile;
import com.cinema.dto.ProfileUpdateForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {

    private final CustomerProfileDao customerProfileDao;

    public CustomerProfileService(CustomerProfileDao customerProfileDao) {
        this.customerProfileDao = customerProfileDao;
    }

    public CustomerProfile getProfile(int accountId) {
        return customerProfileDao.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile was not found."));
    }

    @Transactional
    public void updateProfile(int accountId, ProfileUpdateForm form) {
        getProfile(accountId);
        if (form.getPhone() != null && customerProfileDao.existsByPhoneExcludingAccount(form.getPhone(), accountId)) {
            throw new IllegalArgumentException("This phone number is already used by another account.");
        }
        customerProfileDao.updateProfile(accountId, form);
    }

    @Transactional
    public void updateAvatar(int accountId, String avatarUrl) {
        getProfile(accountId);
        customerProfileDao.updateAvatar(accountId, avatarUrl);
    }
}
