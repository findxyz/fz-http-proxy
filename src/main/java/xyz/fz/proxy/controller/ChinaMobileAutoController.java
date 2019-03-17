package xyz.fz.proxy.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.fz.proxy.service.impl.ChinaMobileAutoServiceImpl;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/cmAuto")
public class ChinaMobileAutoController {

    @Resource
    private ChinaMobileAutoServiceImpl chinaMobileAutoService;

    @RequestMapping(value = "/", produces = {"application/json"})
    public String records(@RequestParam String code) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String date = sdf.format(new Date());
        if ((date + "42").equals(code)) {
            return chinaMobileAutoService.recordsSnapshot();
        } else {
            return "";
        }
    }
}
