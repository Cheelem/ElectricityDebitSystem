package cn.edu.neu.electricity.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class ElectricityInfoController {

    @RequestMapping("/record")
    public void getElectricityRecord(){

    }

}
