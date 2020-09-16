package org.xxpay.shop.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.unbiz.fluentvalidator.annotation.FluentValid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xxpay.common.constant.PayConstant;
import org.xxpay.common.util.*;
import org.xxpay.shop.dao.model.GoodsOrder;
import org.xxpay.shop.service.GoodsOrderService;
import org.xxpay.shop.util.Constant;
import org.xxpay.shop.util.OAuth2RequestParamHelper;
import org.xxpay.shop.util.vx.WxApi;
import org.xxpay.shop.util.vx.WxApiClient;
import org.xxpay.shop.vo.PayOrderReqVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Controller
@RequestMapping("/mallPay")
public class MallOrderController {

    private final static MyLog _log = MyLog.getLog(MallOrderController.class);

    @Autowired
    private GoodsOrderService goodsOrderService;

    static final String mchId = "20001223";
    // 加签key
//    static final String reqKey = "M86l522AV6q613Ii4W6u8K48uW8vM1N6bFgyv769220MdYe9u37N4y7rI5mQ";
    static final String reqKey = "123456";
    // 验签key
//    static final String resKey = "Hpcl522AV6q613KIi46u6g6XuW8vM1N8bFgyv769770MdYe9u37M4y7rIpl8";
    static final String resKey = "456789";
    //static final String baseUrl = "http://api.xxpay.org/api";
//    static final String baseUrl = "http://127.0.0.1:3020/api";
    static final String baseUrl = "http://192.168.1.249:3020/api";
    //static final String notifyUrl = "http://shop.xxpay.org/goods/payNotify";
    static final String notifyUrl = "http://192.168.1.249:8091/goods/payNotify";
    private AtomicLong seq = new AtomicLong(0L);
    private final static String QR_PAY_URL = "http://shop.xxpay.org/goods/qrPay.html";
    static final String AppID = "wx077cb62e341f8a5c";
    static final String AppSecret = "e663ea068f3e4f952f143de1432a35c2";
    private final static String GetOpenIdURL = "http://shop.xxpay.org/goods/getOpenId";
    private final static String GetOpenIdURL2 = "http://shop.xxpay.org/goods/getOpenId2";


    @RequestMapping("/openQrPay.html")
    public String openQrPay(ModelMap model) {
        return "openQrPay";
    }

    @RequestMapping("/qrPay.html")
    public String qrPay(ModelMap model, HttpServletRequest request, @FluentValid PayOrderReqVO payOrderReqVO) {
        String logPrefix = "【二维码扫码支付】";
        String view = "qrPay";
        _log.info("====== 开始接收二维码扫码支付请求 ======");
        String ua = request.getHeader("User-Agent");
        _log.info("{}接收参数:orderId={},ua={}", logPrefix, payOrderReqVO.getOrderId(), ua);
        String client = "alipay";
        String channelId = "ALIPAY_WAP";
        if(StringUtils.isBlank(ua)) {
            String errorMessage = "User-Agent为空！";
            _log.info("{}信息：{}", logPrefix, errorMessage);
            model.put("result", "failed");
            model.put("resMsg", errorMessage);
            return view;
        }else {
            if(ua.contains("Alipay")) {
                client = "alipay";
                channelId = "ALIPAY_WAP";
            }else if(ua.contains("MicroMessenger")) {
                client = "wx";
                channelId = "WX_JSAPI";
            }
        }
        if(client == null) {
            String errorMessage = "请用微信或支付宝扫码";
            _log.info("{}信息：{}", logPrefix, errorMessage);
            model.put("result", "failed");
            model.put("resMsg", errorMessage);
            return view;
        }
        // 先插入订单数据
        Map<String, String> orderMap = null;
        if ("alipay".equals(client)) {
            _log.info("{}{}扫码下单", logPrefix, "支付宝");
            // 下单
            orderMap = createPayOrder(payOrderReqVO);
        }else if("wx".equals(client)){
            _log.info("{}{}扫码", logPrefix, "微信");
            // 判断是否拿到openid，如果没有则去获取
            String openId = request.getParameter("openId");
            if (StringUtils.isNotBlank(openId)) {
                _log.info("{}openId：{}", logPrefix, openId);
                Map params = new HashMap<>();
                params.put("channelId", channelId);
                params.put("openId", openId);
                // 下单
                orderMap = createPayOrder(payOrderReqVO);
            }else {
                String redirectUrl = QR_PAY_URL + "?amount=" + payOrderReqVO.getOrderId();
                String url = GetOpenIdURL2 + "?redirectUrl=" + redirectUrl;
                _log.info("跳转URL={}", url);
                return "redirect:" + url;
            }
        }
        if(orderMap != null) {
            model.put("orderMap", orderMap);
        }
        model.put("amount", AmountUtil.convertCent2Dollar(payOrderReqVO.getOrderPayMoney()+""));
        model.put("payOrder", payOrderReqVO);
        model.put("orderId", payOrderReqVO.getOrderId());
        model.put("client", client);
        return view;
    }

    @RequestMapping("/toAliPay.html")
    @ResponseBody
    public String toAliPay(HttpServletRequest request, PayOrderReqVO payOrderReqVO) {
        String logPrefix = "【支付宝支付】";
        _log.info("====== 开始接收支付宝支付请求 ======");
        String goodsId = "G_0001";
        _log.info("{}接收参数:goodsId={},amount={},channelId={}", logPrefix, goodsId, payOrderReqVO.getOrderPayMoney(), payOrderReqVO.getChannelId());
        // 先插入订单数据
        Map params = new HashMap<>();
        params.put("channelId", payOrderReqVO.getChannelId());
        // 下单
        Map<String, String> orderMap = createPayOrder(payOrderReqVO);
        if(PayConstant.PAY_CHANNEL_ALIPAY_MOBILE.equalsIgnoreCase(payOrderReqVO.getChannelId())) return orderMap.get("payParams");
        return orderMap.get("payUrl");
    }

    private Map createPayOrder(PayOrderReqVO payOrderReqVO) {
        JSONObject paramMap = new JSONObject();
        paramMap.put("mchId", mchId);                       // 商户ID
        paramMap.put("mchOrderNo", payOrderReqVO.getOrderId());           // 商户订单号
        paramMap.put("channelId", payOrderReqVO.getChannelId());             // 支付渠道ID, WX_NATIVE,ALIPAY_WAP
        paramMap.put("amount", payOrderReqVO.getOrderPayMoney());                          // 支付金额,单位分
        paramMap.put("currency", "cny");                    // 币种, cny-人民币
        paramMap.put("clientIp", "114.112.124.236");        // 用户地址,IP或手机号
        paramMap.put("device", "WEB");                      // 设备
        paramMap.put("subject", payOrderReqVO.getOrderName());
        paramMap.put("body", payOrderReqVO.getOrderName());
        paramMap.put("notifyUrl", notifyUrl);         // 回调URL
        paramMap.put("param1", "");                         // 扩展参数1
        paramMap.put("param2", "");                         // 扩展参数2

        JSONObject extra = new JSONObject();
        extra.put("openId", payOrderReqVO.getPayerUserId());
        paramMap.put("extra", extra.toJSONString());  // 附加参数

        String reqSign = PayDigestUtil.getSign(paramMap, reqKey);
        paramMap.put("sign", reqSign);   // 签名
        String reqData = "params=" + paramMap.toJSONString();
        System.out.println("请求支付中心下单接口,请求数据:" + reqData);
        String url = baseUrl + "/pay/create_order?";
        String urlParam = url + reqData;
        _log.info("urlParam:{}", urlParam);
        String result = XXPayUtil.call4Post(urlParam);
        System.out.println("请求支付中心下单接口,响应数据:" + result);
        Map retMap = JSON.parseObject(result);
        if("SUCCESS".equals(retMap.get("retCode"))) {
            // 验签
            String checkSign = PayDigestUtil.getSign(retMap, resKey, "sign", "payParams");
            String retSign = (String) retMap.get("sign");
            if(checkSign.equals(retSign)) {
                System.out.println("=========支付中心下单验签成功=========");
            }else {
                System.err.println("=========支付中心下单验签失败=========");
                return null;
            }
        }
        return retMap;
    }


    /**
     * 获取code
     * @return
     */
    @RequestMapping("/getOpenId")
    public void getOpenId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        _log.info("进入获取用户openID页面");
        String redirectUrl = request.getParameter("redirectUrl");
        String code = request.getParameter("code");
        String openId = "";
        if(!StringUtils.isBlank(code)){//如果request中包括code，则是微信回调
            try {
                openId = WxApiClient.getOAuthOpenId(AppID, AppSecret, code);
                _log.info("调用微信返回openId={}", openId);
            } catch (Exception e) {
                _log.error(e, "调用微信查询openId异常");
            }
            if(redirectUrl.indexOf("?") > 0) {
                redirectUrl += "&openId=" + openId;
            }else {
                redirectUrl += "?openId=" + openId;
            }
            response.sendRedirect(redirectUrl);
        }else{//oauth获取code
            String redirectUrl4Vx = GetOpenIdURL + "?redirectUrl=" + redirectUrl;
            String state = OAuth2RequestParamHelper.prepareState(request);
            String url = WxApi.getOAuthCodeUrl(AppID, redirectUrl4Vx, "snsapi_base", state);
            _log.info("跳转URL={}", url);
            response.sendRedirect(url);
        }
    }

    /**
     * 获取code
     * @return
     */
    @RequestMapping("/getOpenId2")
    public void getOpenId2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        _log.info("进入获取用户openID页面");
        String redirectUrl = request.getParameter("redirectUrl");
        String code = request.getParameter("code");
        String openId = "";
        if(!StringUtils.isBlank(code)){//如果request中包括code，则是微信回调
            try {
                openId = WxApiClient.getOAuthOpenId(AppID, AppSecret, code);
                _log.info("调用微信返回openId={}", openId);
            } catch (Exception e) {
                _log.error(e, "调用微信查询openId异常");
            }
            if(redirectUrl.indexOf("?") > 0) {
                redirectUrl += "&openId=" + openId;
            }else {
                redirectUrl += "?openId=" + openId;
            }
            response.sendRedirect(redirectUrl);
        }else{//oauth获取code
            //http://www.abc.com/xxx/get-weixin-code.html?appid=XXXX&scope=snsapi_base&state=hello-world&redirect_uri=http%3A%2F%2Fwww.xyz.com%2Fhello-world.html
            String redirectUrl4Vx = GetOpenIdURL2 + "?redirectUrl=" + redirectUrl;
            String url = String.format("http://www.xiaoshuding.com/get-weixin-code.html?appid=%s&scope=snsapi_base&state=hello-world&redirect_uri=%s", AppID, WxApi.urlEnodeUTF8(redirectUrl4Vx));
            _log.info("跳转URL={}", url);
            response.sendRedirect(url);
        }
    }

    /**
     * 接收支付中心通知
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/payNotify")
    public void payNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        _log.info("====== 开始处理支付中心通知 ======");
        Map<String,Object> paramMap = request2payResponseMap(request, new String[]{
                "payOrderId","mchId","mchOrderNo","channelId","amount","currency","status", "clientIp",
                "device",  "subject", "channelOrderNo", "param1",
                "param2","paySuccTime","backType","sign"
        });
        _log.info("支付中心通知请求参数,paramMap={}", paramMap);
        if (!verifyPayResponse(paramMap)) {
            String errorMessage = "verify request param failed.";
            _log.warn(errorMessage);
            outResult(response, "fail");
            return;
        }
        String payOrderId = (String) paramMap.get("payOrderId");
        String mchOrderNo = (String) paramMap.get("mchOrderNo");
        String resStr;
        try {
            GoodsOrder goodsOrder = goodsOrderService.getGoodsOrder(mchOrderNo);
            if(goodsOrder != null && goodsOrder.getStatus() == Constant.GOODS_ORDER_STATUS_COMPLETE) {
                outResult(response, "success");
                return;
            }
            // 执行业务逻辑
            int ret = goodsOrderService.updateStatus4Success(mchOrderNo);
            // ret返回结果
            // 等于1表示处理成功,返回支付中心success
            // 其他值,返回支付中心fail,让稍后再通知
            if(ret == 1) {
                ret = goodsOrderService.updateStatus4Complete(mchOrderNo);
                if(ret == 1) {
                    resStr = "success";
                }else {
                    resStr = "fail";
                }
            }else {
                resStr = "fail";
            }
        }catch (Exception e) {
            resStr = "fail";
            _log.error(e, "执行业务异常,payOrderId=%s.mchOrderNo=%s", payOrderId, mchOrderNo);
        }
        _log.info("响应支付中心通知结果:{},payOrderId={},mchOrderNo={}", resStr, payOrderId, mchOrderNo);
        outResult(response, resStr);
        _log.info("====== 支付中心通知处理完成 ======");
    }

    @RequestMapping("/notify_test")
    public void notifyTest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        outResult(response, "success");
    }



    void outResult(HttpServletResponse response, String content) {
        response.setContentType("text/html");
        PrintWriter pw;
        try {
            pw = response.getWriter();
            pw.print(content);
            _log.error("response xxpay complete.");
        } catch (IOException e) {
            _log.error(e, "response xxpay write exception.");
        }
    }

    public Map<String, Object> request2payResponseMap(HttpServletRequest request, String[] paramArray) {
        Map<String, Object> responseMap = new HashMap<>();
        for (int i = 0;i < paramArray.length; i++) {
            String key = paramArray[i];
            String v = request.getParameter(key);
            if (v != null) {
                responseMap.put(key, v);
            }
        }
        return responseMap;
    }

    public boolean verifyPayResponse(Map<String,Object> map) {
        String mchId = (String) map.get("mchId");
        String payOrderId = (String) map.get("payOrderId");
        String mchOrderNo = (String) map.get("mchOrderNo");
        String amount = (String) map.get("amount");
        String sign = (String) map.get("sign");

        if (StringUtils.isEmpty(mchId)) {
            _log.warn("Params error. mchId={}", mchId);
            return false;
        }
        if (StringUtils.isEmpty(payOrderId)) {
            _log.warn("Params error. payOrderId={}", payOrderId);
            return false;
        }
        if (StringUtils.isEmpty(amount) || !NumberUtils.isNumber(amount)) {
            _log.warn("Params error. amount={}", amount);
            return false;
        }
        if (StringUtils.isEmpty(sign)) {
            _log.warn("Params error. sign={}", sign);
            return false;
        }

        // 验证签名
        if (!verifySign(map)) {
            _log.warn("verify params sign failed. payOrderId={}", payOrderId);
            return false;
        }

        // 根据payOrderId查询业务订单,验证订单是否存在
        GoodsOrder goodsOrder = goodsOrderService.getGoodsOrder(mchOrderNo);
        if(goodsOrder == null) {
            _log.warn("业务订单不存在,payOrderId={},mchOrderNo={}", payOrderId, mchOrderNo);
            return false;
        }
        // 核对金额
        if(goodsOrder.getAmount() != Long.parseLong(amount)) {
            _log.warn("支付金额不一致,dbPayPrice={},payPrice={}", goodsOrder.getAmount(), amount);
            return false;
        }
        return true;
    }

    public boolean verifySign(Map<String, Object> map) {
        String mchId = (String) map.get("mchId");
        if(!this.mchId.equals(mchId)) return false;
        String localSign = PayDigestUtil.getSign(map, resKey, "sign");
        String sign = (String) map.get("sign");
        return localSign.equalsIgnoreCase(sign);
    }

}