package com.example.adminwebjava.controller;

import com.example.adminwebjava.entity.Category;
import com.example.adminwebjava.entity.PaymentPlan;
import com.example.adminwebjava.entity.Poi;
import com.example.adminwebjava.entity.SupportedLanguage;
import com.example.adminwebjava.services.TourGuideService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;

@Controller
public class AdminPageController {
    private final TourGuideService service;
    private final JdbcTemplate jdbc;
    public AdminPageController(TourGuideService service, JdbcTemplate jdbc){this.service=service;this.jdbc=jdbc;}
    @GetMapping({"/admin/pois/create","/pois/create"}) public String poiCreate(Model m){m.addAttribute("poi",new Poi());return "pois/Create";}
    @GetMapping({"/admin/pois/{id}/edit","/pois/{id}/edit"}) public String poiEdit(@PathVariable long id,Model m){m.addAttribute("poiId",id);return "pois/Edit";}
    @GetMapping({"/admin/pois/{id}/delete","/pois/{id}/delete"}) public String poiDelete(@PathVariable long id,Model m){m.addAttribute("poiId",id);return "pois/Delete";}
    @GetMapping({"/admin/tours/create","/tours/create"}) public String tourCreate(){return "Tour/Create";}
    @GetMapping({"/admin/tours/{id}/edit","/tours/{id}/edit"}) public String tourEdit(@PathVariable long id,Model m){m.addAttribute("tourId",id);return "Tour/Edit";}
    @GetMapping({"/admin/tours/{id}/manage-pois","/tours/{id}/manage-pois"}) public String tourManage(@PathVariable long id,Model m){m.addAttribute("tourId",id);m.addAttribute("pois",service.findPois("vi","",""));return "Tour/ManagePois";}
    @GetMapping({"/admin/tour-translations","/admin/TourTranslation"}) public String tourTranslations(Model m){m.addAttribute("translations",List.of());m.addAttribute("tour",new java.util.HashMap<>());return "TourTranslation/Index";}
    @GetMapping({"/admin/tours/{id}/builder","/tours/{id}/builder"}) public String tourBuilder(@PathVariable long id,Model m){m.addAttribute("tourId",id);m.addAttribute("pois",service.findPois("vi","",""));return "Tour/Builder";}
    @GetMapping({"/categories","/admin/categories","/admin/Category"}) public String categories(Model m){m.addAttribute("categories",List.of());m.addAttribute("isAdmin",true);return "Category/Index";}
    @GetMapping({"/categories/create","/admin/categories/create","/admin/Category/Create"}) public String categoryCreate(Model m){m.addAttribute("category",new Category());return "Category/Create";}
    @GetMapping({"/categories/edit/{id}","/admin/categories/edit/{id}","/admin/Category/Edit/{id}"}) public String categoryEdit(@PathVariable int id,Model m){Category c=new Category();c.setId(id);m.addAttribute("category",c);return "Category/Edit";}
    @GetMapping({"/category-translations","/category-translation/index"}) public String categoryTranslations(Model m){m.addAttribute("category",new Category());m.addAttribute("translations",List.of());return "CategoryTranslation/Index";}
    @GetMapping("/category-translation/create") public String categoryTranslationCreate(){return "CategoryTranslation/Create";}
    @GetMapping("/category-translation/edit") public String categoryTranslationEdit(){return "CategoryTranslation/Edit";}
    @GetMapping({"/admin/payment-plans","/admin/PaymentPlan"}) public String paymentPlans(Model m){List<Map<String,Object>> p=jdbc.queryForList("SELECT Id AS id, PlanCode AS planCode, PlanName AS planName, Audience AS audience, Price AS price, DurationDays AS durationDays, Description AS description, IsActive AS isActive FROM payment_plans ORDER BY Id");m.addAttribute("paymentPlans",p);return "PaymentPlan/Index";}
    @GetMapping({"/admin/payment-plans/create","/admin/PaymentPlan/Create"}) public String paymentPlanCreate(Model m){m.addAttribute("paymentPlan",new PaymentPlan());return "PaymentPlan/Create";}
    @GetMapping({"/admin/payment-plans/edit/{id}","/admin/PaymentPlan/Edit/{id}"}) public String paymentPlanEdit(@PathVariable int id,Model m){PaymentPlan p=new PaymentPlan();p.setId(id);m.addAttribute("paymentPlan",p);return "PaymentPlan/Edit";}
    @GetMapping({"/admin/languages","/admin/Language"}) public String languages(Model m){m.addAttribute("languages",List.of());return "Language/Index";}
    @GetMapping({"/admin/languages/create","/admin/Language/Create"}) public String languageCreate(Model m){m.addAttribute("supportedLanguage",new SupportedLanguage());return "Language/Create";}
    @GetMapping({"/admin/languages/edit/{id}","/admin/Language/Edit/{id}"}) public String languageEdit(@PathVariable String id,Model m){SupportedLanguage l=new SupportedLanguage();l.setLanguageCode(id);m.addAttribute("supportedLanguage",l);return "Language/Edit";}
    @GetMapping({"/admin/media/library","/admin/Media/Library"}) public String mediaLibrary(Model m){m.addAttribute("mediaAssets",List.of());return "Media/Library";}
    @GetMapping({"/admin/media","/admin/Media"}) public String media(){return "Media/Index";}
    @GetMapping({"/admin/review","/admin/Review"}) public String review(Model m){m.addAttribute("reviews",List.of());return "Review/Index";}
    @GetMapping({"/admin/analytics","/analytics"}) public String analytics(Model m){m.addAttribute("totalPois",0);m.addAttribute("totalLogs",0);m.addAttribute("topPois",List.of());return "Analytics/index";}
    @GetMapping({"/admin/activity-log","/admin/ActivityLog","/usage-history"}) public String activity(Model m){m.addAttribute("logs",List.of());m.addAttribute("currentPage",1);m.addAttribute("totalPages",1);m.addAttribute("searchString","");return "ActivityLog/index";}
    @GetMapping({"/admin/visitor-traffic","/admin/VisitorTraffic"}) public String visitorTraffic(){return "VisitorTraffic/Index";}
    @GetMapping({"/admin/approval/poi-pending","/admin/Approval/PoiPending"}) public String approval(Model m){m.addAttribute("title","Duyệt POI");m.addAttribute("model",List.of());return "Approval/PoiPending";}
    @GetMapping({"/admin/owners","/admin/OwnerAdmin"}) public String owners(Model m){m.addAttribute("ownerProfiles",List.of());m.addAttribute("totalAssignedPois",0);return "OwnerAdmin/Index";}
    @GetMapping({"/admin/owners/requests","/admin/OwnerAdmin/Requests"}) public String ownerRequests(Model m){m.addAttribute("requests",List.of());return "OwnerAdmin/Requests";}
    @GetMapping({"/admin/payment","/admin/Payment"}) public String payment(Model m){m.addAttribute("paymentTransactions",List.of());m.addAttribute("totalRevenue",0);m.addAttribute("pendingCount",0);return "Payment/Index";}
    @GetMapping({"/admin/purchases","/admin/Purchases"}) public String purchases(){return "Purchases/Index";}
    @GetMapping({"/admin/revenue","/admin/Revenue"}) public String revenue(){return "Revenue/Index";}
    @GetMapping({"/admin/sync-versions","/admin/SyncVersion"}) public String sync(Model m){m.addAttribute("syncVersions",List.of());return "SyncVersion/Index";}
    @GetMapping({"/admin/settings","/admin/Setting"}) public String settings(Model m){m.addAttribute("settings",List.of());return "Setting/Index";}
    @GetMapping({"/admin/users","/admin/User"}) public String users(Model m){m.addAttribute("users",List.of());return "User/Index";}
    @GetMapping({"/admin/tourists","/admin/User/Tourists"}) public String tourists(Model m){m.addAttribute("tourists",List.of());return "User/Tourists";}
}
