package com.cinema.model;

import java.util.List;

public record MemberLookupResult(
        Integer customerId,
        String fullName,
        String phone,
        String email,
        String membershipRank,
        Integer availablePoints,
        Integer tierPoints,
        List<AvailableVoucher> vouchers
) {
}
