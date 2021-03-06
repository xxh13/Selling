package selling.sunshine.service.impl;

import com.alibaba.fastjson.JSON;
import com.pingplusplus.Pingpp;
import com.pingplusplus.model.Charge;
import common.sunshine.model.selling.bill.CustomerOrderBill;
import common.sunshine.model.selling.bill.DepositBill;
import common.sunshine.model.selling.bill.OrderBill;
import common.sunshine.model.selling.bill.RefundBill;
import common.sunshine.pagination.DataTableParam;
import common.sunshine.utils.ResponseCode;
import common.sunshine.utils.ResultData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import selling.sunshine.dao.BillDao;
import selling.sunshine.dao.ChargeDao;
import selling.sunshine.service.BillService;
import selling.sunshine.utils.PlatformConfig;
import selling.sunshine.vo.bill.BillSumVo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sunshine on 5/10/16.
 */
@Service
public class BillServiceImpl implements BillService {

    private Logger logger = LoggerFactory.getLogger(BillServiceImpl.class);

    {
        Pingpp.apiKey = PlatformConfig.getValue("pingxx_api_key");
    }

    @Autowired
    private BillDao billDao;

    @Autowired
    private ChargeDao chargeDao;

    @Override
    public ResultData createDepositBill(DepositBill bill, String openId) {
        ResultData result = new ResultData();
        ResultData insertResponse = billDao.insertDepositBill(bill);
        if (insertResponse.getResponseCode() != ResponseCode.RESPONSE_OK) {
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(insertResponse.getDescription());
        }
        bill = (DepositBill) insertResponse.getData();
        Map<String, Object> params = new HashMap<>();
        params.put("order_no", bill.getBillId());
        params.put("amount", bill.getBillAmount() * 100);
        Map<String, Object> app = new HashMap<>();
        app.put("id", PlatformConfig.getValue("pingxx_app_id"));
        params.put("app", app);
        params.put("channel", bill.getChannel());
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("alipay_wap")) {
            Map<String, Object> con = new HashMap<>();
            con.put("success_url", PlatformConfig.getValue("server_url") + "/payment/" + bill.getBillId() + "/result/success");
            con.put("cancel_url", PlatformConfig.getValue("server_url") + "/payment/" + bill.getBillId() + "/result/failure");
            con.put("app_pay", true);
            params.put("extra", con);
        }
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("wx_pub")) {
            Map<String, String> user = new HashMap<>();
            user.put("open_id", openId);
            params.put("extra", user);
        }
        params.put("currency", "cny");
        params.put("client_ip", bill.getClientIp());
        params.put("subject", "代理商账户充值");
        params.put("body", "充值金额为:" + bill.getBillAmount() + "元");
        try {
            Charge charge = Charge.create(params);
            logger.debug(JSON.toJSONString(charge));
            result.setData(charge);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(e.getMessage());
        }
        return result;
    }

    @Override
    public ResultData fetchDepositBill(Map<String, Object> condition) {
        ResultData result = new ResultData();
        ResultData queryResponse = billDao.queryDepositBill(condition);
        result.setResponseCode(queryResponse.getResponseCode());
        if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(queryResponse.getData());
        } else if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(queryResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData updateDepositBill(DepositBill bill) {
        ResultData result = new ResultData();
        ResultData updateResponse = billDao.updateDepositBill(bill);
        result.setResponseCode(updateResponse.getResponseCode());
        if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(updateResponse.getData());
        } else if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(updateResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData createOrderBill(OrderBill bill, String openId) {
        ResultData result = new ResultData();
        ResultData insertResponse = billDao.insertOrderBill(bill);
        if (insertResponse.getResponseCode() != ResponseCode.RESPONSE_OK) {
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(insertResponse.getDescription());
        }
        bill = (OrderBill) insertResponse.getData();
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("coffer")) {
            result.setData(bill);
            return result;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("order_no", bill.getBillId());
        params.put("amount", bill.getBillAmount() * 100);
        Map<String, Object> app = new HashMap<>();
        app.put("id", PlatformConfig.getValue("pingxx_app_id"));
        params.put("app", app);
        params.put("channel", bill.getChannel());
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("alipay_wap")) {
            Map<String, Object> con = new HashMap<>();
            con.put("success_url", PlatformConfig.getValue("server_url") + "/payment/" + bill.getBillId() + "/result/success");
            con.put("cancel_url", PlatformConfig.getValue("server_url") + "/payment/" + bill.getBillId() + "/result/failure");
            con.put("app_pay", true);
            params.put("extra", con);
        }
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("wx_pub")) {
            Map<String, String> user = new HashMap<>();
            user.put("open_id", openId);
            params.put("extra", user);
        }
        params.put("currency", "cny");
        params.put("client_ip", bill.getClientIp());
        params.put("subject", "订单支付");
        params.put("body", "支付金额为:" + bill.getBillAmount() + "元");
        try {
            Charge charge = Charge.create(params);
            logger.debug(JSON.toJSONString(charge));
            result.setData(charge);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(e.getMessage());
        }
        return result;
    }

    @Override
    public ResultData fetchOrderBill(Map<String, Object> condition) {
        ResultData result = new ResultData();
        ResultData queryResponse = billDao.queryOrderBill(condition);
        result.setResponseCode(queryResponse.getResponseCode());
        if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            if (((List) queryResponse.getData()).isEmpty()) {
                result.setResponseCode(ResponseCode.RESPONSE_NULL);
            }
            result.setData(queryResponse.getData());
        } else if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(queryResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData updateOrderBill(OrderBill bill) {
        ResultData result = new ResultData();
        ResultData updateResponse = billDao.updateOrderBill(bill);
        result.setResponseCode(updateResponse.getResponseCode());
        if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(updateResponse.getData());
        } else if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(updateResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData createCustomerOrderBill(CustomerOrderBill bill, String openId) {
        ResultData result = new ResultData();
        ResultData insertResponse = billDao.insertCustomerOrderBill(bill);
        if (insertResponse.getResponseCode() != ResponseCode.RESPONSE_OK) {
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(insertResponse.getDescription());
        }
        bill = (CustomerOrderBill) insertResponse.getData();
        Map<String, Object> params = new HashMap<>();
        params.put("order_no", bill.getBillId());
        params.put("amount", bill.getBillAmount() * 100);
        Map<String, Object> app = new HashMap<>();
        app.put("id", PlatformConfig.getValue("pingxx_app_id"));
        params.put("app", app);
        params.put("app", app);
        params.put("channel", bill.getChannel());
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("alipay_wap")) {
            Map<String, Object> con = new HashMap<>();
            con.put("success_url", PlatformConfig.getValue("server_url") + "/payment/" + bill.getBillId() + "/result/success");
            con.put("cancel_url", PlatformConfig.getValue("server_url") + "/payment/" + bill.getBillId() + "/result/failure");
            con.put("app_pay", true);
            params.put("extra", con);
        }
        if (!StringUtils.isEmpty(bill.getChannel()) && bill.getChannel().equals("wx_pub")) {
            Map<String, String> user = new HashMap<>();
            user.put("open_id", openId);
            params.put("extra", user);
        }
        params.put("currency", "cny");
        params.put("client_ip", bill.getClientIp());
        params.put("subject", "订单支付");
        params.put("body", "支付金额为:" + bill.getBillAmount() + "元");
        try {
            Charge charge = Charge.create(params);
            result.setData(charge);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    common.sunshine.model.selling.charge.Charge info = new common.sunshine.model.selling.charge.Charge(charge);
                    chargeDao.insertCharge(info);
                }
            };
            thread.start();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(e.getMessage());
        }
        return result;
    }

    @Override
    public ResultData fetchCustomerOrderBill(Map<String, Object> condition) {
        ResultData result = new ResultData();
        ResultData queryResponse = billDao.queryCustomerOrderBill(condition);
        result.setResponseCode(queryResponse.getResponseCode());
        if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            if (((List) queryResponse.getData()).isEmpty()) {
                result.setResponseCode(ResponseCode.RESPONSE_NULL);
            }
            result.setData(queryResponse.getData());
        } else if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(queryResponse.getDescription());
        }
        return result;
    }


    @Override
    public ResultData updateCustomerOrderBill(CustomerOrderBill bill) {
        ResultData result = new ResultData();
        ResultData updateResponse = billDao.updateCustomerOrderBill(bill);
        result.setResponseCode(updateResponse.getResponseCode());
        if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(updateResponse.getData());
        } else if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(updateResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData createRefundBill(RefundBill bill) {
        ResultData result = new ResultData();
        ResultData insertResponse = billDao.insertRefundBill(bill);
        result.setResponseCode(insertResponse.getResponseCode());
        if (insertResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(insertResponse.getData());
        } else {
            result.setDescription(insertResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData fetchRefundBill(Map<String, Object> condition) {
        ResultData result = new ResultData();
        ResultData queryResponse = billDao.queryRefundBill(condition);
        result.setResponseCode(queryResponse.getResponseCode());
        if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            if (((List) queryResponse.getData()).isEmpty()) {
                result.setResponseCode(ResponseCode.RESPONSE_NULL);
            }
            result.setData(queryResponse.getData());
        } else if (queryResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(queryResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData updateRefundBill(RefundBill bill) {
        ResultData result = new ResultData();
        ResultData updateResponse = billDao.updateRefundBill(bill);
        result.setResponseCode(updateResponse.getResponseCode());
        if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(updateResponse.getData());
        } else if (updateResponse.getResponseCode() == ResponseCode.RESPONSE_ERROR) {
            result.setDescription(updateResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData fetchBillSum(Map<String, Object> condition) {
        ResultData result = new ResultData();
        ResultData fetchResponse = billDao.queryBillSum(condition);
        result.setResponseCode(fetchResponse.getResponseCode());
        if (fetchResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
            if (((List<BillSumVo>) fetchResponse.getData()).isEmpty()) {
                result.setResponseCode(ResponseCode.RESPONSE_NULL);
            }
            result.setData(fetchResponse.getData());
        } else {
            result.setDescription(fetchResponse.getDescription());
        }
        return result;
    }

    @Override
    public ResultData fetchBillSum(Map<String, Object> condition, DataTableParam param) {
        ResultData result = new ResultData();
        ResultData response = billDao.queryBillSumByPage(condition, param);
        result.setResponseCode(response.getResponseCode());
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(response.getData());
        } else {
            result.setDescription(response.getDescription());
        }
        return result;
    }
}
