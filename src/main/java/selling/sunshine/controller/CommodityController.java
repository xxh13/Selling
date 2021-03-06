package selling.sunshine.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import common.sunshine.model.selling.admin.Admin;
import common.sunshine.model.selling.agent.Agent;
import common.sunshine.model.selling.goods.Goods4Agent;
import common.sunshine.model.selling.goods.Goods4Customer;
import common.sunshine.model.selling.goods.Thumbnail;
import common.sunshine.model.selling.order.CustomerOrder;
import common.sunshine.model.selling.user.User;
import common.sunshine.pagination.DataTablePage;
import common.sunshine.pagination.DataTableParam;
import common.sunshine.utils.ResponseCode;
import common.sunshine.utils.ResultData;
import common.sunshine.utils.SortRule;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import selling.sunshine.form.GoodsForm;
import selling.sunshine.model.BackOperationLog;
import selling.sunshine.model.sum.Vendition;
import selling.sunshine.service.*;
import selling.sunshine.utils.PlatformConfig;
import selling.sunshine.utils.WechatConfig;
import selling.sunshine.utils.WechatUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sunshine on 4/8/16.
 */
@RequestMapping("/commodity")
@RestController
public class CommodityController {

    private Logger logger = LoggerFactory.getLogger(CommodityController.class);

    @Autowired
    private CommodityService commodityService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private LogService logService;

    @Autowired
    private StatisticService statisticService;

    /**
     * 创建商品
     * @param form
     * @param result
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public ModelAndView create(@Valid GoodsForm form, BindingResult result, HttpServletRequest request) {
        ModelAndView view = new ModelAndView();
        if (result.hasErrors()) {
            view.setViewName("redirect:/commodity/create");
            return view;
        }

        Goods4Customer goods = new Goods4Customer(form.getName(), Double.parseDouble(form.getAgentPrice()), Double.parseDouble(form.getPrice()), form.getDescription(), form.getStandard(), form.getMeasure(), form.getProduceNo(), form.getProduceDate());
        if (!StringUtils.isEmpty(form.getNickname())) {
            goods.setNickname(form.getNickname());
        }
        goods.setBlockFlag(form.isBlock());
        ResultData response = commodityService.createGoods4Customer(goods);
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            List<Thumbnail> thumbnails = (List<Thumbnail>) commodityService.fetchThumbnail().getData();
            for (Thumbnail thumbnail : thumbnails) {
                thumbnail.setGoods((Goods4Customer) response.getData());
            }
            commodityService.updateThumbnails(thumbnails);

            Subject subject = SecurityUtils.getSubject();
            User user = (User) subject.getPrincipal();
            if (user == null) {
                view.setViewName("redirect:/commodity/create");
                return view;
            }
            Admin admin = user.getAdmin();
            BackOperationLog backOperationLog = new BackOperationLog(admin.getUsername(), toolService.getIP(request),
                    "管理员" + admin.getUsername() + "添加了一个新商品，商品名称:" + form.getName());
            logService.createbackOperationLog(backOperationLog);
            view.setViewName("redirect:/commodity/overview");
            return view;
        } else {
            view.setViewName("redirect:/commodity/create");
            return view;
        }
    }

    /**
     * 修改商品页面
     * @param goodsId
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/edit/{goodsId}")
    public ModelAndView edit(@PathVariable("goodsId") String goodsId) {
        ModelAndView view = new ModelAndView();
        Map<String, Object> condition = new HashMap<>();
        condition.put("goodsId", goodsId);
        ResultData resultData = commodityService.fetchGoods4Customer(condition);
        if (resultData.getResponseCode() != ResponseCode.RESPONSE_OK) {
            view.setViewName("redirect:/commodity/overview");
            return view;
        }
        Goods4Customer target = ((ArrayList<Goods4Customer>) resultData.getData()).get(0);
        view.addObject("goods", target);

        view.setViewName("/backend/goods/update");
        return view;
    }

    /**
     * 上传商品图片-type:slide 表示轮播
     * @param goodsId
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/thumbnails/{goodsId}")
    @ResponseBody
    public String thumbnails(@PathVariable("goodsId") String goodsId) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("goodsId", goodsId);
        condition.put("type", "slide");
        ResultData resultData = commodityService.fetchThumbnail(condition);
        List<Thumbnail> thumbnails = (List<Thumbnail>) resultData.getData();

        JSONArray resultArray = new JSONArray();
        JSONArray initialPreviewArray = new JSONArray();
        JSONArray initialPreviewConfigArray = new JSONArray();
        if (thumbnails.size() == 0) {

            resultArray.add(initialPreviewArray);
            resultArray.add(initialPreviewConfigArray);
            return resultArray.toJSONString();
        }
        for (Thumbnail thumbnail : thumbnails) {
            JSONObject initialPreviewConfigObject = new JSONObject();
            initialPreviewArray.add(thumbnail.getPath());
            initialPreviewConfigObject.put("url", "/commodity/delete/Thumbnail/" + thumbnail.getThumbnailId());
            initialPreviewConfigObject.put("key", thumbnail.getThumbnailId());
            initialPreviewConfigArray.add(initialPreviewConfigObject);
        }
        resultArray.add(initialPreviewArray);
        resultArray.add(initialPreviewConfigArray);
        return resultArray.toJSONString();

    }

    /**
     * 上传商品图片-type:cover 表示封面
     * @param goodsId
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/picture/{goodsId}")
    @ResponseBody
    public String picture(@PathVariable("goodsId") String goodsId) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("goodsId", goodsId);
        condition.put("type", "cover");
        ResultData resultData = commodityService.fetchThumbnail(condition);
        List<Thumbnail> thumbnails = (List<Thumbnail>) resultData.getData();

        JSONArray resultArray = new JSONArray();
        JSONArray initialPreviewArray = new JSONArray();
        JSONArray initialPreviewConfigArray = new JSONArray();
        if (thumbnails.size() == 0) {

            resultArray.add(initialPreviewArray);
            resultArray.add(initialPreviewConfigArray);
            return resultArray.toJSONString();
        }
        for (Thumbnail thumbnail : thumbnails) {
            JSONObject initialPreviewConfigObject = new JSONObject();
            initialPreviewArray.add(thumbnail.getPath());
            initialPreviewConfigObject.put("url", "/commodity/delete/Thumbnail/" + thumbnail.getThumbnailId());
            initialPreviewConfigObject.put("key", thumbnail.getThumbnailId());
            initialPreviewConfigArray.add(initialPreviewConfigObject);
        }
        resultArray.add(initialPreviewArray);
        resultArray.add(initialPreviewConfigArray);
        return resultArray.toJSONString();

    }

    /**
     * 修改商品表单
     * @param goodsId
     * @param form
     * @param result
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/edit/{goodsId}")
    public ModelAndView edit(@PathVariable("goodsId") String goodsId, @Valid GoodsForm form, BindingResult result,
                             HttpServletRequest request) {
        ModelAndView view = new ModelAndView();
        if (result.hasErrors()) {
            view.setViewName("redirect:/commodity/overview");
            return view;
        }
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        if (user == null) {
            view.setViewName("redirect:/commodity/overview");
            return view;
        }
        Map<String, Object> condition = new HashMap<>();
        condition.put("goodsId", goodsId);
        ResultData queryData = commodityService.fetchGoods4Customer(condition);
        Goods4Customer oldGoods = ((List<Goods4Customer>) queryData.getData()).get(0);
        Goods4Customer goods = new Goods4Customer(form.getName(), Double.parseDouble(form.getAgentPrice()), Double.parseDouble(form.getPrice()), form.getDescription(), form.getStandard(), form.getMeasure(), form.getProduceNo(), form.getProduceDate());
        if (!StringUtils.isEmpty(form.getNickname())) {
            goods.setNickname(form.getNickname());
        }
        goods.setBlockFlag(form.isBlock());
        goods.setGoodsId(goodsId);
        ResultData response = commodityService.updateGoods4Customer(goods);

        List<Thumbnail> thumbnails = (List<Thumbnail>) commodityService.fetchThumbnail().getData();
        for (Thumbnail thumbnail : thumbnails) {
            thumbnail.setGoods((Goods4Customer) response.getData());
        }
        commodityService.updateThumbnails(thumbnails);
        Admin admin = user.getAdmin();
        BackOperationLog backOperationLog = new BackOperationLog(admin.getUsername(), toolService.getIP(request), "管理员" + admin.getUsername() + "将商品名称为" + oldGoods.getName() + "的商品信息修改了");
        logService.createbackOperationLog(backOperationLog);
        view.setViewName("redirect:/commodity/overview");
        return view;

    }

    /**
     * 后台商品列表页
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/overview")
    public ModelAndView overview() {
        ModelAndView view = new ModelAndView();
        view.setViewName("/backend/goods/list");
        return view;
    }

    /**
     * 后台获取客户商品的表单
     * @param param
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/overview")
    public DataTablePage<Goods4Customer> overview(DataTableParam param) {
        DataTablePage<Goods4Customer> result = new DataTablePage<>(param);
        if (StringUtils.isEmpty(param)) {
            return result;
        }
        Map<String, Object> condition = new HashMap<>();
        ResultData response = commodityService.fetchGoods4Customer(condition, param);
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result = (DataTablePage<Goods4Customer>) response.getData();
        }
        return result;
    }

    /*
     * 上架的所有商品
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/shelve")
    public DataTablePage<Goods4Customer> shelve(DataTableParam param) {
        DataTablePage<Goods4Customer> result = new DataTablePage<>(param);
        if (StringUtils.isEmpty(param)) {
            return result;
        }
        Map<String, Object> condition = new HashMap<>();
        condition.put("blockFlag", false);
        ResultData response = commodityService.fetchGoods4Customer(condition, param);
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result = (DataTablePage<Goods4Customer>) response.getData();
        }
        return result;
    }

    /**
     * 客户商城首页——商品列表
     * @param request
     * @param agentId
     * @param code
     * @param state
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/viewlist")
    public ModelAndView viewList(HttpServletRequest request, HttpServletResponse response, String agentId, String code, String state) {
        ModelAndView view = new ModelAndView();
        String linkUrl = "https://" + PlatformConfig.getValue("server_url") + "/commodity/viewlist";
        String link = "";
		try {
			link = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
			      + PlatformConfig.getValue("wechat_appid") + "&redirect_uri=" + URLEncoder.encode(linkUrl, "utf-8")
			      + "&response_type=code&scope=snsapi_base&state=view#wechat_redirect";
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        //先判断微信，若是则需要openID
        if (request.getHeader("user-agent").toLowerCase().contains("micromessenger")) {
        	//一系列的openID获取代码，最后放入session中
            String openId = null;
            if (StringUtils.isEmpty(code) || StringUtils.isEmpty(state)) {
                HttpSession session = request.getSession();
                if (session.getAttribute("openId") == null || session.getAttribute("openId").equals("")) {
                    try {
						response.sendRedirect(link);
					} catch (IOException e) {
						e.printStackTrace();
					}
                	WechatConfig.oauthWechat(view, "/customer/component/goods_error_msg");
                    view.setViewName("/customer/component/goods_error_msg");
                    return view;
                }
            }
            if (code != null && !code.equals("")) {
                openId = WechatUtil.queryOauthOpenId(code);
            }
            if (openId == null || openId.equals("")) {
                HttpSession session = request.getSession();
                if (session.getAttribute("openId") != null && !session.getAttribute("openId").equals("")) {
                    openId = (String) session.getAttribute("openId");
                }
            }
            if (openId == null || openId.equals("")) {
            	try {
					response.sendRedirect(link);
				} catch (IOException e) {
					e.printStackTrace();
				}
                WechatConfig.oauthWechat(view, "/customer/component/goods_error_msg");
                view.setViewName("/customer/component/goods_error_msg");
                return view;
            }
            if (!StringUtils.isEmpty(openId)) {
                HttpSession session = request.getSession();
                session.setAttribute("openId", openId);
            }
        }
        Map<String, Object> condition = new HashMap<String, Object>();
        condition.put("blockFlag", false);
        List<SortRule> rules = new ArrayList<>();
        rules.add(new SortRule("goods_position", "desc"));
        condition.put("sort", rules);
        ResultData fetchGoodsData = commodityService.fetchGoods4Customer(condition);
        if (fetchGoodsData.getResponseCode() != ResponseCode.RESPONSE_OK) {
            WechatConfig.oauthWechat(view, "/customer/component/goods_error_msg");
            view.setViewName("/customer/component/goods_error_msg");
            return view;
        }
        List<Goods4Customer> goods4Customers = (List<Goods4Customer>) fetchGoodsData.getData();
        for (Goods4Customer goods : goods4Customers) {
        	//商品缩略图的封面获取
            List<Thumbnail> thumbnails = goods.getThumbnails();
            List<Thumbnail> newThumbnails = new ArrayList<Thumbnail>();
            for (Thumbnail thumbnail : thumbnails) {
                if (thumbnail.getType().equals("cover")) {
                    newThumbnails.add(thumbnail);
                }
            }
            goods.setThumbnails(newThumbnails);
        }
        view.addObject("goodsList", goods4Customers);
        view.setViewName("/customer/goods/goods_list");
        return view;
    }

    
    /**
     * 客户商城-单个商品
     * @param request
     * @param goodsId
     * @param agentId
     * @param code
     * @param state
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{goodsId}")
    public ModelAndView view(HttpServletRequest request, HttpServletResponse response, @PathVariable("goodsId") String goodsId, String agentId, String code, String state) {
        ModelAndView view = new ModelAndView();
        String openId = null;
        String linkUrl = "https://" + PlatformConfig.getValue("server_url") + "/commodity/" + goodsId
        + "?agentId=" + agentId;
        String link = "";
		try {
			link = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
			      + PlatformConfig.getValue("wechat_appid") + "&redirect_uri=" + URLEncoder.encode(linkUrl, "utf-8")
			      + "&response_type=code&scope=snsapi_base&state=view#wechat_redirect";
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        //先判断微信
        if (request.getHeader("user-agent").toLowerCase().contains("micromessenger")) {
        	//一系列的openID获取代码，最后放入session中
            if (StringUtils.isEmpty(code) || StringUtils.isEmpty(state)) {
                HttpSession session = request.getSession();
                if (session.getAttribute("openId") == null || session.getAttribute("openId").equals("")) {
                	try {
 						response.sendRedirect(link);
 					} catch (IOException e) {
 						e.printStackTrace();
 					}
                	WechatConfig.oauthWechat(view, "/customer/component/goods_error_msg");
                    view.setViewName("/customer/component/goods_error_msg");
                    return view;
                }
            }
            if (code != null && !code.equals("")) {
                openId = WechatUtil.queryOauthOpenId(code);
            }
            if (openId == null || openId.equals("")) {
                HttpSession session = request.getSession();
                if (session.getAttribute("openId") != null && !session.getAttribute("openId").equals("")) {
                    openId = (String) session.getAttribute("openId");
                }
            }
            view.addObject("wechat", openId);
            if (openId == null || openId.equals("")) {
            	try {
						response.sendRedirect(link);
					} catch (IOException e) {
						e.printStackTrace();
					}
                WechatConfig.oauthWechat(view, "/customer/component/goods_error_msg");
                view.setViewName("/customer/component/goods_error_msg");
                return view;
            }
            if (!StringUtils.isEmpty(openId)) {
                HttpSession session = request.getSession();
                session.setAttribute("openId", openId);
            }
        }
        Map<String, Object> condition = new HashMap<>();
        if (openId != null) {
            condition.put("wechat", openId);
            List<SortRule> rules = new ArrayList<>();
            rules.add(new SortRule("create_time", "desc"));
            condition.put("sort", rules);
            ResultData customerOrderResponse = orderService.fetchCustomerOrder(condition);
            if (customerOrderResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
                List<CustomerOrder> list = (List<CustomerOrder>) customerOrderResponse.getData();
                view.addObject("history", list.get(0));
            }
        }
        condition.clear();
        condition.put("goodsId", goodsId);
        condition.put("blockFlag", false);
        ResultData fetchCommodityData = commodityService.fetchGoods4Customer(condition);
        if (fetchCommodityData.getResponseCode() != ResponseCode.RESPONSE_OK) {
            WechatConfig.oauthWechat(view, "/customer/component/goods_error_msg");
            view.setViewName("/customer/component/goods_error_msg");
            return view;
        }
        Goods4Customer goods = ((List<Goods4Customer>) fetchCommodityData.getData()).get(0);
        if (!StringUtils.isEmpty(agentId)) {
            condition.clear();
            condition.put("agentId", agentId);
            condition.put("granted", true);
            condition.put("blockFlag", false);
            ResultData fetchAgentData = agentService.fetchAgent(condition);
            if (fetchAgentData.getResponseCode() != ResponseCode.RESPONSE_OK) {
                WechatConfig.oauthWechat(view, "/customer/component/agent_error_msg");
                view.setViewName("/customer/component/agent_error_msg");
                return view;
            }
            Agent agent = ((List<Agent>) fetchAgentData.getData()).get(0);
            view.addObject("agent", agent);
        }
        for (int i = 0; i < goods.getThumbnails().size(); i++) {
            if (goods.getThumbnails().get(i).getType().equals("cover")) {
                goods.getThumbnails().remove(i);
                break;
            }
        }
        view.addObject("goods", goods);
        WechatConfig.oauthWechat(view, "/customer/goods/detail");
        view.setViewName("/customer/goods/detail");
        return view;
    }

    /**
     * 客户商城-查询客户订单
     * @param orderId
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/customerorder")
    public ModelAndView customerOrder(String orderId) {
        ModelAndView view = new ModelAndView();
        Map<String, Object> condition = new HashMap<>();
        if (!StringUtils.isEmpty(orderId)) {
            condition.put("orderId", orderId);
        }
        if (condition.isEmpty()) {
            // 订单不存在错误页面
            WechatConfig.oauthWechat(view, "/customer/component/order_error_msg");
            view.setViewName("/customer/component/order_error_msg");
            return view;
        }
        ResultData fetchCustomerOrderData = orderService.fetchCustomerOrder(condition);
        if (fetchCustomerOrderData.getResponseCode() != ResponseCode.RESPONSE_OK) {
            // 订单不存在错误页面
            WechatConfig.oauthWechat(view, "/customer/component/order_error_msg");
            view.setViewName("/customer/component/order_error_msg");
            return view;
        }
        CustomerOrder customerOrder = ((List<CustomerOrder>) fetchCustomerOrderData.getData()).get(0);
        view.addObject("customerOrder", customerOrder);
        WechatConfig.oauthWechat(view, "/customer/order/detail");
        view.setViewName("/customer/order/detail");
        return view;
    }

    /**
     * 后台商品详情页
     * @param goodsId
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/detail/{goodsId}")
    public ModelAndView detail(@PathVariable("goodsId") String goodsId) {
        ModelAndView view = new ModelAndView();
        Map<String, Object> condition = new HashMap<>();
        condition.put("goodsId", goodsId);
        ResultData queryData = commodityService.fetchGoods4Customer(condition);
        if (queryData.getData() != null) {
            Goods4Customer goods = ((List<Goods4Customer>) queryData.getData()).get(0);
            view.addObject("goods", goods);
            List<Thumbnail> thumbnails = (List<Thumbnail>) commodityService.fetchThumbnail(condition).getData();
            if (thumbnails.size() != 0) {
                view.addObject("thumbnails", thumbnails);
            } else {
                view.addObject("thumbnails", 0);
            }
        }
        view.setViewName("/backend/goods/detail");
        return view;
    }

    /**
     * 后台管理员下架商品
     * @param goodsId
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/forbid/{goodsId}")
    public ModelAndView forbid(@PathVariable("goodsId") String goodsId, HttpServletRequest request) {
        ModelAndView view = new ModelAndView();
        Map<String, Object> condition = new HashMap<String, Object>();
        condition.put("goodsId", goodsId);
        ResultData resultData = commodityService.fetchGoods4Customer(condition);
        if (resultData.getResponseCode() != ResponseCode.RESPONSE_OK) {
            view.setViewName("redirect:/commodity/detail/" + goodsId);
            return view;
        }
        Goods4Customer target = ((List<Goods4Customer>) resultData.getData()).get(0);
        target.setBlockFlag(true);
        ResultData response = commodityService.updateGoods4Customer(target);
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            Subject subject = SecurityUtils.getSubject();
            User user = (User) subject.getPrincipal();
            if (user == null) {
                view.setViewName("redirect:/commodity/detail/" + goodsId);
                return view;
            }
            Admin admin = user.getAdmin();
            BackOperationLog backOperationLog = new BackOperationLog(admin.getUsername(), toolService.getIP(request),
                    "管理员" + admin.getUsername() + "将商品" + target.getName() + "下架");
            logService.createbackOperationLog(backOperationLog);
            view.setViewName("redirect:/commodity/detail/" + goodsId);
        } else {
            view.setViewName("redirect:/commodity/detail/" + goodsId);
        }
        return view;
    }

    /**
     * 后台管理员上架商品
     * @param goodsId
     * @param request
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/enable/{goodsId}")
    public ModelAndView enable(@PathVariable("goodsId") String goodsId, HttpServletRequest request) {
        ModelAndView view = new ModelAndView();
        Map<String, Object> condition = new HashMap<>();
        condition.put("goodsId", goodsId);
        ResultData resultData = commodityService.fetchGoods4Customer(condition);
        if (resultData.getResponseCode() != ResponseCode.RESPONSE_OK) {
            view.setViewName("redirect:/commodity/detail/" + goodsId);
            return view;
        }
        Goods4Customer target = ((List<Goods4Customer>) resultData.getData()).get(0);
        target.setBlockFlag(false);
        ResultData response = commodityService.updateGoods4Customer(target);
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            Subject subject = SecurityUtils.getSubject();
            User user = (User) subject.getPrincipal();
            if (user == null) {
                view.setViewName("redirect:/commodity/detail/" + goodsId);
                return view;
            }
            Admin admin = user.getAdmin();
            BackOperationLog backOperationLog = new BackOperationLog(admin.getUsername(), toolService.getIP(request),
                    "管理员" + admin.getUsername() + "将商品" + target.getName() + "上架");
            logService.createbackOperationLog(backOperationLog);
            view.setViewName("redirect:/commodity/detail/" + goodsId);
        } else {
            view.setViewName("redirect:/commodity/detail/" + goodsId);
        }
        return view;
    }

    /**
     * 上传商品图片-slide轮播ajax
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/thumbnail/upload")
    public ResultData uploadThumbnail(MultipartHttpServletRequest request) {
        ResultData result = new ResultData();
        String context = request.getSession().getServletContext().getRealPath("/");
        JSONObject resultObject = new JSONObject();
        try {
            MultipartFile file = request.getFile("thumbnail[]");
            if (file != null) {
                ResultData response = uploadService.upload(file, context);
                if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
                    Thumbnail thumbnail = new Thumbnail((String) response.getData(), "slide");
                    response = commodityService.createThumbnail(thumbnail);
                    if (response.getResponseCode() != ResponseCode.RESPONSE_OK) {
                        result.setResponseCode(ResponseCode.RESPONSE_ERROR);
                        result.setDescription("图片记录存储失败");
                        return result;
                    }
                    String thumbnailId = ((Thumbnail) response.getData()).getThumbnailId();
                    result.setData(thumbnailId);
                    return result;
                } else {
                    result.setResponseCode(ResponseCode.RESPONSE_ERROR);
                    result.setDescription("图片上传失败");
                    return result;
                }
            }
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription("未获取到图片");
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(e.getMessage());
        }
        return result;
    }

    /**
     * 上传商品封面cover的ajax
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/cover/upload")
    public ResultData uploadCover(MultipartHttpServletRequest request) {
        ResultData result = new ResultData();
        String context = request.getSession().getServletContext().getRealPath("/");
        try {
            MultipartFile file = request.getFile("picture");
            if (file != null) {
                ResultData response = uploadService.upload(file, context);
                if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
                    Thumbnail thumbnail = new Thumbnail((String) response.getData(), "cover");
                    response = commodityService.createThumbnail(thumbnail);
                    if (response.getResponseCode() != ResponseCode.RESPONSE_OK) {
                        result.setResponseCode(ResponseCode.RESPONSE_ERROR);
                        result.setDescription("图片记录存储失败");
                        return result;
                    }
                    String thumbnailId = ((Thumbnail) response.getData()).getThumbnailId();
                    result.setData(thumbnailId);
                    return result;
                } else {
                    result.setResponseCode(ResponseCode.RESPONSE_ERROR);
                    result.setDescription("图片上传失败");
                    return result;
                }
            }
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription("未获取到图片");
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(e.getMessage());
        }
        return result;
    }

    /**
     * 删除商品封面
     * @param thumbnailId
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/delete/thumbnail/{thumbnailId}")
    public ResultData deleteThumbnail(@PathVariable("thumbnailId") String thumbnailId) {
        ResultData result = new ResultData();
        ResultData response = commodityService.deleteGoodsThumbnail(thumbnailId);
        if (response.getResponseCode() == ResponseCode.RESPONSE_OK) {
            result.setData(response.getData());
        } else if (response.getResponseCode() == ResponseCode.RESPONSE_NULL) {
            result.setResponseCode(ResponseCode.RESPONSE_NULL);
            result.setDescription("未找到该thumbnail图片");
        } else {
            result.setResponseCode(ResponseCode.RESPONSE_ERROR);
            result.setDescription(response.getDescription());
        }
        return result;
    }

    /**
     * 获取商品销售详情
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/volume")
    public ResultData volume() {
        ResultData resultData = new ResultData();
        JSONArray array = new JSONArray();
        Map<String, Object> condition = new HashMap<>();
        condition.put("blockFlag", false);
        ResultData queryData = commodityService.fetchGoods4Agent(condition);
        if (queryData.getResponseCode() == ResponseCode.RESPONSE_OK) {
            List<Goods4Agent> goodsList = (List<Goods4Agent>) queryData.getData();// 所有没有下架的商品
            Map<String, List<Object>> map = new HashMap<>();
            for (Goods4Agent goods : goodsList) {
                List<Object> list = new ArrayList<>();
                list.add(goods.getName());
                list.add(0);
                list.add(0);
                map.put(goods.getGoodsId(), list);
            }
            condition.clear();
            condition.put("type", 0);
            queryData = statisticService.purchaseRecord(condition);
            if (queryData.getResponseCode() == ResponseCode.RESPONSE_OK) {
                List<Vendition> venditions = (List<Vendition>) queryData.getData();
                for (Vendition vendition : venditions) {
                    if (map.containsKey(vendition.getGoodsId())) {
                        List<Object> list = map.get(vendition.getGoodsId());
                        list.set(2, vendition.getGoodsQuantity());
                        map.put(vendition.getGoodsId(), list);
                    }
                }
            }
            condition.put("monthly", true);
            queryData = statisticService.purchaseRecord(condition);
            if (queryData.getResponseCode() == ResponseCode.RESPONSE_OK) {
                List<Vendition> monthlyVenditions = (List<Vendition>) queryData.getData();
                for (Vendition vendition : monthlyVenditions) {
                    if (map.containsKey(vendition.getGoodsId())) {
                        List<Object> list = map.get(vendition.getGoodsId());
                        list.set(1, vendition.getGoodsQuantity());
                        map.put(vendition.getGoodsId(), list);
                    }
                }
            }
            for (String key : map.keySet()) {
                JSONObject object = new JSONObject();
                List<Object> list = map.get(key);
                object.put("goodsId", key);
                object.put("goodsName", list.get(0).toString());
                object.put("monthQuantity", list.get(1).toString());
                object.put("overallQuantity", list.get(2).toString());
                array.add(object);
            }
            resultData.setData(array);
            return resultData;
        }

        resultData.setResponseCode(ResponseCode.RESPONSE_NULL);
        return resultData;
    }

    /**
     * 后台商品销售统计
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/summary")
    public ModelAndView summary() {
        ModelAndView view = new ModelAndView();
        view.setViewName("/backend/goods/sales");
        return view;
    }

}
