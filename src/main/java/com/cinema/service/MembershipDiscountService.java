package com.cinema.service;
import com.cinema.dao.MembershipDiscountDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class MembershipDiscountService {
    private final MembershipDiscountDAO dao;
    public MembershipDiscountService(MembershipDiscountDAO dao) { this.dao = dao; }
    @Transactional public void refreshOnline(int booking, int account) { refresh(booking, account, false); }
    @Transactional public void refreshCounter(int booking, int staff) { refresh(booking, staff, true); }
    private void refresh(int booking, int owner, boolean counter) {
        var found = dao.lockOrder(booking, owner, counter);
        if (found.isEmpty() || !"PENDING".equalsIgnoreCase(found.get().status())) return;
        var order = found.get();
        var discount = order.voucherId() == null && order.rankId() > 0
                ? order.policy().discount(order.subtotal(), dao.countUsedOrReserved(order.customerId(), order.rankId(), booking))
                : BigDecimal.ZERO;
        dao.saveReservation(booking, order, discount);
    }
}
