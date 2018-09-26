package cn.edu.neu.electricity.controller;

import cn.edu.neu.electricity.database.DatabaseController;
import org.json.JSONObject;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Date;
import java.sql.SQLException;

@Controller
@RequestMapping("/")
public class FrontendController {

    @RequestMapping("/")
    public String index(Model model) throws SQLException {
        model.addAttribute("users", DatabaseController.getAllElectricityUsers());
        model.addAttribute("exceptions", DatabaseController.getAllExceptions());
        return "index";
    }

    @RequestMapping("/user/{userId}")
    public String userPage(Model model, @PathVariable("userId") Integer userId) throws SQLException {
        model.addAttribute("userId", userId);
        model.addAttribute("me", DatabaseController.getMyUser(userId));
        model.addAttribute("devices", DatabaseController.getMyDevices(userId));
        model.addAttribute("records", DatabaseController.getAllMyRecords(userId));
        model.addAttribute("orders", DatabaseController.getAllPaymentOrders(userId));
        model.addAttribute("bankaccounts", DatabaseController.getAllBankAccounts(userId));
        model.addAttribute("balancelogs", DatabaseController.getAllBalanceLogs(userId));
        model.addAttribute("totalfee", String.valueOf(DatabaseController.getAllFeeToPay(userId)));
        return "user";
    }

    @RequestMapping("/meter-recorder")
    public String meterRecorderPage(Model model) throws SQLException {
        return "meter-recorder";
    }

    @RequestMapping("/meter-recorder/submit")
    public String meterRecorderSubmit(Model model, HttpServletRequest request) throws SQLException {
        try {
            int deviceId = Integer.valueOf(request.getParameter("device-id"));
            double currentMeter = Double.valueOf(request.getParameter("current-meter"));
            Date date = Date.valueOf(request.getParameter("current-date"));
            JSONObject result = DatabaseController.submitElectricityRecord(deviceId,currentMeter, date);
            model.addAttribute("result", result);
        } catch (Exception ex){
            ex.printStackTrace();
            JSONObject result = new JSONObject();
            result.put("RESULT", -2);
            result.put("FEE", 0);
            model.addAttribute("result", result);
        }
        return "meter-recorder-result";
    }

    @RequestMapping("/pay-order/submit/{userId}")
    public String paymentSubmit(Model model, HttpServletRequest request, @PathVariable("userId") Integer userId) {
        model.addAttribute("userId", userId);
        try {
            int deviceId = Integer.valueOf(request.getParameter("device-id"));
            double amount = Double.valueOf(request.getParameter("amount"));
            String bankId = request.getParameter("bank-id");
            String bankAccountId = request.getParameter("bank-account-id");
            JSONObject result = DatabaseController.submitOrderPayment(deviceId,amount,bankAccountId, bankId, userId);
            model.addAttribute("result", result);
        } catch (Exception ex) {
            ex.printStackTrace();
            JSONObject result = new JSONObject();
            result.put("RESULT", -2);
            model.addAttribute("result", result);
        }
        return "payment-result";
    }

    @RequestMapping("/pay-order/balance/{userId}")
    public String chargeWithBalance(Model model, @PathVariable("userId") Integer userId) {
        model.addAttribute("userId", userId);
        try {
            JSONObject result = DatabaseController.chargeWithBalance(userId);
            model.addAttribute("result", result);
        } catch (Exception ex) {
            ex.printStackTrace();
            JSONObject result = new JSONObject();
            result.put("RESULT", -2);
            model.addAttribute("result", result);
        }
        return "charge-with-balance-result";
    }

    @RequestMapping("/reversal/{userId}/{orderId}/{amount}")
    public String reversal(Model model, @PathVariable("userId") Integer userId, @PathVariable("orderId") Integer orderId, @PathVariable("amount") Double amount) {
        model.addAttribute("userId", userId);
        try {
            JSONObject result = DatabaseController.reversalOrderPayment(orderId, amount);
            model.addAttribute("result", result);
        } catch (Exception ex) {
            ex.printStackTrace();
            JSONObject result = new JSONObject();
            result.put("RESULT", -2);
            model.addAttribute("result", result);
        }
        return "reversal-result";
    }

    @RequestMapping("/late-fee")
    public String reversal(Model model, HttpServletRequest request) {
        JSONObject result = new JSONObject();
        try {
            DatabaseController.calculateLateFee(Date.valueOf(request.getParameter("current-date")));
            model.addAttribute("title", "违约金计算完成");
            model.addAttribute("subtitle", "所有逾期未缴费的电费清单违约金均已被更新");
            model.addAttribute("content", "您可继续进行更多操作。");
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("title", "违约金计算出现问题");
            model.addAttribute("subtitle", "可能由您的非法输入导致");
            model.addAttribute("content", "请检查您的输入是否正确。");
        }
        return "management-result";
    }

    @RequestMapping("/check/total")
    public String checkTotal(Model model, HttpServletRequest request) {

        try {
            JSONObject result = new JSONObject();
            Date date = Date.valueOf(request.getParameter("date"));
            String bank = request.getParameter("bank");
            Double amount = Double.valueOf(request.getParameter("amount"));
            int count = Integer.valueOf(request.getParameter("count"));
            int res = DatabaseController.checkTotal(date,bank,amount,count);
            if(res==0) {
                model.addAttribute("title", "总账持平");
                model.addAttribute("subtitle", "两方账目正常");
            } else if(res==-1){
                model.addAttribute("title", "交易笔数不符");
                model.addAttribute("subtitle", "请执行明细对账");
            } else if(res==-2){
                model.addAttribute("title", "交易金额不符");
                model.addAttribute("subtitle", "请执行明细对账");
            }
            model.addAttribute("content", "您可继续进行更多操作。");
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("title", "总账计算出现问题");
            model.addAttribute("subtitle", "可能由您的非法输入导致");
            model.addAttribute("content", "请检查您的输入是否正确。");
        }
        return "management-result";
    }

    @RequestMapping("/check/detail")
    public String checkDetail(Model model, HttpServletRequest request) {
        JSONObject result = new JSONObject();
        try {
            Date date = Date.valueOf(request.getParameter("date"));
            int res = DatabaseController.checkDetail(date);
            if(res==0) {
                model.addAttribute("title", "明细正常持平");
                model.addAttribute("subtitle", "两方账目正常");
            } else {
                model.addAttribute("title", "共有"+res+"笔交易信息不符");
                model.addAttribute("subtitle", "请查看异常记录表");
            }
            model.addAttribute("content", "您可继续进行更多操作。");
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("title", "违约金计算出现问题");
            model.addAttribute("subtitle", "可能由您的非法输入导致");
            model.addAttribute("content", "请检查您的输入是否正确。");
        }
        return "management-result";
    }
}
