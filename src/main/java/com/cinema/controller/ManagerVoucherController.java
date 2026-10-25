package com.cinema.controller;

import com.cinema.dto.VoucherForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.AdminPage;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.ManagerPromotionFilter;
import com.cinema.model.ManagerPromotionRow;
import com.cinema.model.ManagerPromotionStats;
import com.cinema.model.ManagerVoucherRow;
import com.cinema.service.ManagerPromotionService;
import com.cinema.service.ManagerVoucherService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Screen M11 (tab "Vouchers / Coupons") + M11.1 add, M11.2 detail, M11.3 edit, M11.4 delete confirm.
 * URLs:
 *  GET  /manager/vouchers                     list + KPI + filters
 *  GET  /manager/vouchers/new[?promotionId=]  add form
 *  POST /manager/vouchers                     create
 *  GET  /manager/vouchers/{id}                detail, ?confirmDelete=1 opens the delete dialog
 *  GET  /manager/vouchers/{id}/edit           edit form
 *  POST /manager/vouchers/{id}                update
 *  POST /manager/vouchers/{id}/status         pause / resume / activate / stop
 *  POST /manager/vouchers/{id}/delete         hard delete (only when never used)
 * Manager only.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Controller
@RequestMapping("/manager/vouchers")
public class ManagerVoucherController {

    private static final Logger log = LoggerFactory.getLogger(ManagerVoucherController.class);

    private final ManagerVoucherService voucherService;
    private final ManagerPromotionService promotionService;
    private final SessionService sessionService;

    public ManagerVoucherController(ManagerVoucherService voucherService, ManagerPromotionService promotionService,
                                    SessionService sessionService) {
        this.voucherService = voucherService;
        this.promotionService = promotionService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       HttpSession session, Model model) {
        if (!isManager(session)) return "redirect:/login";
        addShell(session, model);
        ManagerPromotionFilter filter = ManagerPromotionFilter.of(keyword, status, type, page, size);
        model.addAttribute("tab", "vouchers");
        model.addAttribute("filter", filter);
        model.addAttribute("pageSizes", ManagerPromotionFilter.PAGE_SIZES);
        model.addAttribute("loadError", false);
        try {
            AdminPage<ManagerVoucherRow> result = voucherService.search(filter);
            model.addAttribute("voucherPage", result);
            model.addAttribute("stats", promotionService.getStats());
            model.addAttribute("promotionCount", promotionService.getAll().size());
            model.addAttribute("voucherCount", voucherService.countAll());
        } catch (DataAccessException ex) {
            log.error("Cannot load vouchers", ex);
            model.addAttribute("loadError", true);
            model.addAttribute("voucherPage", AdminPage.of(List.<ManagerVoucherRow>of(), 1, filter.size()));
            model.addAttribute("stats", ManagerPromotionStats.empty());
            model.addAttribute("promotionCount", 0);
            model.addAttribute("voucherCount", 0);
        }
        return "manager/promotions/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Integer promotionId, HttpSession session, Model model) {
        if (!isManager(session)) return "redirect:/login";
        addShell(session, model);
        VoucherForm form = new VoucherForm();
        form.setPromotionId(promotionId);
        model.addAttribute("form", form);
        return showForm(model, null);
    }

    @PostMapping
    public String create(@ModelAttribute("form") VoucherForm form, HttpSession session, Model model,
                         RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            int id = voucherService.create(form);
            redirect.addFlashAttribute("successMessage", "Voucher " + form.getCode() + " has been "
                    + ("DRAFT".equals(form.getStatus()) ? "saved as a draft." : "created and activated."));
            return "redirect:/manager/vouchers/" + id;
        } catch (PromotionAdminException ex) {
            addShell(session, model);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("fieldErrors", ex.getFieldErrors());
            return showForm(model, null);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") int voucherId,
                         @RequestParam(required = false) Integer confirmDelete,
                         HttpSession session, Model model, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerVoucherRow voucher = voucherService.get(voucherId);
            addShell(session, model);
            model.addAttribute("voucher", voucher);
            model.addAttribute("confirmDelete", confirmDelete != null);
            return "manager/promotions/voucher-detail";
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/vouchers";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") int voucherId, HttpSession session, Model model,
                           RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerVoucherRow voucher = voucherService.get(voucherId);
            if (voucher.isStopped()) {
                redirect.addFlashAttribute("errorMessage", "A stopped voucher cannot be edited.");
                return "redirect:/manager/vouchers/" + voucherId;
            }
            addShell(session, model);
            model.addAttribute("form", VoucherForm.from(voucher));
            return showForm(model, voucher);
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/vouchers";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") int voucherId, @ModelAttribute("form") VoucherForm form,
                         HttpSession session, Model model, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            voucherService.update(voucherId, form);
            redirect.addFlashAttribute("successMessage", "Voucher " + form.getCode() + " has been updated.");
            return "redirect:/manager/vouchers/" + voucherId;
        } catch (PromotionAdminException ex) {
            ManagerVoucherRow current;
            try {
                current = voucherService.get(voucherId);
            } catch (PromotionAdminException notFound) {
                redirect.addFlashAttribute("errorMessage", notFound.getMessage());
                return "redirect:/manager/vouchers";
            }
            addShell(session, model);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("fieldErrors", ex.getFieldErrors());
            return showForm(model, current);
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable("id") int voucherId, @RequestParam String status,
                               @RequestParam(required = false) String returnTo,
                               HttpSession session, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerVoucherRow v = voucherService.changeStatus(voucherId, status);
            redirect.addFlashAttribute("successMessage", ManagerPromotionController.statusMessage("Voucher " + v.getCode(), status));
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "list".equals(returnTo) ? "redirect:/manager/vouchers" : "redirect:/manager/vouchers/" + voucherId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") int voucherId, HttpSession session, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerVoucherRow v = voucherService.delete(voucherId);
            redirect.addFlashAttribute("successMessage", "Voucher " + v.getCode() + " has been deleted.");
            return "redirect:/manager/vouchers";
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/vouchers/" + voucherId;
        }
    }

    // ------------------------------------------------------------------ helpers

    private String showForm(Model model, ManagerVoucherRow current) {
        model.addAttribute("tab", "vouchers");
        model.addAttribute("kind", "voucher");
        model.addAttribute("current", current);
        model.addAttribute("editing", current != null);
        model.addAttribute("listUrl", "/manager/vouchers");
        model.addAttribute("formAction", current != null ? "/manager/vouchers/" + current.getVoucherId() : "/manager/vouchers");
        model.addAttribute("cancelUrl", current != null ? "/manager/vouchers/" + current.getVoucherId() : "/manager/vouchers");
        if (!model.containsAttribute("fieldErrors")) model.addAttribute("fieldErrors", Map.of());
        List<ManagerPromotionRow> promotions;
        try {
            promotions = promotionService.getAll().stream().filter(p -> !p.isStopped()).toList();
            model.addAttribute("ranks", voucherService.getRanks());
        } catch (DataAccessException ex) {
            log.error("Cannot load voucher form options", ex);
            promotions = List.of();
            model.addAttribute("ranks", List.of());
        }
        model.addAttribute("promotions", promotions);
        return "manager/promotions/form";
    }

    private boolean isManager(HttpSession session) {
        return sessionService.hasRole(session, "MANAGER");
    }

    private void addShell(HttpSession session, Model model) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        model.addAttribute("userName", user != null ? user.getFullName() : null);
        model.addAttribute("userRole", sessionService.currentRole(session));
    }
}
