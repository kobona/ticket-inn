package org.metropolis.controller;

import org.metropolis.service.ObjectStorageService;
import org.metropolis.web.vo.RestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Controller
@RequestMapping("/object")
public class ObjectStorageController {

    @Autowired
    private ObjectStorageService oss;

    @PostMapping
    public @ResponseBody RestEntity upload(
           @RequestParam("file") MultipartFile file) {
        oss.storeObject(file);
        return RestEntity.ok();
    }

    @GetMapping("/{objectId}")
    public void download(
           @PathVariable Integer objectId, HttpServletResponse response) {
        oss.loadObject(objectId, response);
    }

    @PostMapping("/{objectId}")
    public @ResponseBody RestEntity refresh(
           @PathVariable Integer objectId, @RequestParam("file") MultipartFile file) {
        oss.refreshObject(objectId, file);
        return RestEntity.ok();
    }

    @PostMapping("/image")
    public @ResponseBody RestEntity image(
           @RequestParam("prevId") Integer prevId,
           @RequestParam("namePrefix") String namePrefix,
           @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return RestEntity.ok(oss.listImage(prevId, namePrefix, limit));
    }
}
