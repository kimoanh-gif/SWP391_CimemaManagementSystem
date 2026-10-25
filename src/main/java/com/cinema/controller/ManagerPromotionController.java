package com.cinema.controller;

import com.cinema.dto.PromotionForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.AdminPage;
import com.cinema.model.AuthenticatedUser;
import com.cinema.model.ManagerPromotionFilter;
import com.cinema.model.ManagerPromotionRow;
import com.cinema.model.ManagerPromotionStats;
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

/**
 * Screen M11 (tab "Promotion campaigns") + M11.1 add, M11.2 detail, M11.3 edit, M11.4 delete confirm.
 * URLs:
 *  GET  /manager/promotions                       list + KPI + filters
 *  GET  /manager/promotions/new                   add form (M11.1)
 *  POST /manager/promotions                       create
 *  GET  /manager/promotions/{id}                  detail (M11.2), ?confirmDelete=1 opens M11.4
 *  GET  /manager/promotions/{id}/edit             edit form (M11.3)
 *  POST /manager/promotions/{id}                  update
 *  POST /manager/promotions/{id}/status           pause / resume / activate / stop
 *  POST /manager/promotions/{id}/delete           hard delete (only when never used)
 * Manager only.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Controller
@RequestMapping("/manager/promotions")
public class ManagerPromotionController {

    private static final Logger log = LoggerFactory.getLogger(ManagerPromotionController.class);

    private final ManagerPromotionService promotionService;
    private final ManagerVoucherService voucherService;
    private final SessionService sessionService;

    public ManagerPromotionController(ManagerPromotionService promotionService, ManagerVoucherService voucherService,
                                      SessionService sessionService) {
        this.promotionService = promotionService;
        this.voucherService = voucherService;
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
        model.addAttribute("tab", "promotions");
        model.addAttribute("filter", filter);
        model.addAttribute("pageSizes", ManagerPromotionFilter.PAGE_SIZES);
        model.addAttribute("loadError", false);
        try {
            AdminPage<ManagerPromotionRow> result = promotionService.search(filter);
            model.addAttribute("promotionPage", result);
            model.addAttribute("stats", promotionService.getStats());
            model.addAttribute("promotionCount", promotionService.getAll().size());
            model.addAttribute("voucherCount", voucherService.countAll());
        } catch (DataAccessException ex) {
            log.error("Cannot load promotions", ex);
            model.addAttribute("loadError", true);
            model.addAttribute("promotionPage", AdminPage.of(List.<ManagerPromotionRow>of(), 1, filter.size()));
            model.addAttribute("stats", ManagerPromotionStats.empty());
            model.addAttribute("promotionCount", 0);
            model.addAttribute("voucherCount", 0);
        }
        return "manager/promotions/list";
    }

    @GetMapping("/new")
    public String createForm(HttpSession session, Model model) {
        if (!isManager(session)) return "redirect:/login";
        addShell(session, model);
        if (!model.containsAttribute("form")) model.addAttribute("form", new PromotionForm());
        return showForm(model, null);
    }

    @PostMapping
    public String create(@ModelAttribute("form") PromotionForm form, HttpSession session, Model model,
                         RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            int id = promotionService.create(form);
            redirect.addFlashAttribute("successMessage", "Promotion \"" + form.getName() + "\" has been "
                    + ("DRAFT".equals(form.getStatus()) ? "saved as a draft." : "created and activated."));
            return "redirect:/manager/promotions/" + id;
        } catch (PromotionAdminException ex) {
            addShell(session, model);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("fieldErrors", ex.getFieldErrors());
            return showForm(model, null);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") int promotionId,
                         @RequestParam(required = false) Integer confirmDelete,
                         HttpSession session, Model model, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerPromotionRow promotion = promotionService.get(promotionId);
            addShell(session, model);
            model.addAttribute("promotion", promotion);
            model.addAttribute("vouchers", promotionService.getVouchers(promotionId));
            model.addAttribute("confirmDelete", confirmDelete != null);
            return "manager/promotions/detail";
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/promotions";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") int promotionId, HttpSession session, Model model,
                           RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerPromotionRow promotion = promotionService.get(promotionId);
            if (promotion.isStopped()) {
                redirect.addFlashAttribute("errorMessage", "A stopped promotion cannot be edited.");
                return "redirect:/manager/promotions/" + promotionId;
            }
            addShell(session, model);
            model.addAttribute("form", PromotionForm.from(promotion));
            return showForm(model, promotion);
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/promotions";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") int promotionId, @ModelAttribute("form") PromotionForm form,
                         HttpSession session, Model model, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            promotionService.update(promotionId, form);
            redirect.addFlashAttribute("successMessage", "Promotion \"" + form.getName() + "\" has been updated.");
            return "redirect:/manager/promotions/" + promotionId;
        } catch (PromotionAdminException ex) {
            ManagerPromotionRow current;
            try {
                current = promotionService.get(promotionId);
            } catch (PromotionAdminException notFound) {
                redirect.addFlashAttribute("errorMessage", notFound.getMessage());
                return "redirect:/manager/promotions";
            }
            addShell(session, model);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("fieldErrors", ex.getFieldErrors());
            return showForm(model, current);
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable("id") int promotionId, @RequestParam String status,
                               @RequestParam(required = false) String returnTo,
                               HttpSession session, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerPromotionRow p = promotionService.changeStatus(promotionId, status);
            redirect.addFlashAttribute("successMessage", statusMessage("Promotion \"" + p.getName() + "\"", status));
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "list".equals(returnTo) ? "redirect:/manager/promotions" : "redirect:/manager/promotions/" + promotionId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") int promotionId, HttpSession session, RedirectAttributes redirect) {
        if (!isManager(session)) return "redirect:/login";
        try {
            ManagerPromotionRow p = promotionService.delete(promotionId);
            redirect.addFlashAttribute("successMessage", "Promotion \"" + p.getName() + "\" and its unused vouchers have been deleted.");
            return "redirect:/manager/promotions";
        } catch (PromotionAdminException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/promotions/" + promotionId;
        }
    }

    // ------------------------------------------------------------------ helpers

    private String showForm(Model model, ManagerPromotionRow current) {
        model.addAttribute("tab", "promotions");
        model.addAttribute("kind", "promotion");
        model.addAttribute("current", current);
        model.addAttribute("editing", current != null);
        model.addAttribute("listUrl", "/manager/promotions");
        model.addAttribute("formAction", current != null ? "/manager/promotions/" + current.getPromotionId() : "/manager/promotions");
        model.addAttribute("cancelUrl", current != null ? "/manager/promotions/" + current.getPromotionId() : "/manager/promotions");
        if (!model.containsAttribute("fieldErrors")) model.addAttribute("fieldErrors", java.util.Map.of());
        return "manager/promotions/form";
    }

    static String statusMessage(String subject, String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        return switch (s) {
            case "PAUSED" -> subject + " has been paused.";
            case "ACTIVE" -> subject + " is now active.";
            case "INACTIVE" -> subject + " has been stopped permanently.";
            default -> subject + " has been updated.";
        };
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
