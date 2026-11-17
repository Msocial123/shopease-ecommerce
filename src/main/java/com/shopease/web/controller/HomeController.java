package com.shopease.web.controller;

import com.shopease.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CatalogService catalogService;

    public HomeController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featured", catalogService.featured(8));
        model.addAttribute("newest", catalogService.newest(8));
        return "home";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
