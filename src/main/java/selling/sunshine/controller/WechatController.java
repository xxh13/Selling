package selling.sunshine.controller;

import com.alibaba.fastjson.JSON;
import com.thoughtworks.xstream.XStream;
import common.sunshine.utils.Encryption;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import selling.sunshine.service.AgentService;
import selling.sunshine.service.FollowerService;
import selling.sunshine.utils.PlatformConfig;
import common.sunshine.utils.ResponseCode;
import common.sunshine.utils.ResultData;
import selling.sunshine.utils.WechatUtil;
import selling.wechat.model.*;
import common.sunshine.utils.XStreamFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * 微信相关接口
 * Created by sunshine on 5/24/16.
 */
@RestController
public class WechatController {
    private Logger logger = LoggerFactory.getLogger(WechatController.class);

    @Autowired
    private FollowerService followerService;

    @Autowired
    private AgentService agentService;

    /**
     * 和微信公众号后台配置对接的接口
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/wechat")
    public String check(HttpServletRequest request) {
        String signature = request.getParameter("signature");// 微信加密签名
        String timestamp = request.getParameter("timestamp");// 时间戳
        String nonce = request.getParameter("nonce");// 随机数
        String echostr = request.getParameter("echostr");//
        ArrayList params = new ArrayList();
        params.add(PlatformConfig.getValue("wechat_token"));
        params.add(timestamp);
        params.add(nonce);
        Collections.sort(params, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        String temp = params.get(0) + (String) params.get(1) + params.get(2);
        if (Encryption.SHA1(temp).equals(signature)) {
            return echostr;
        }
        return "";
    }


    /**
     * 微信公众号后台的消息和事件（包括文本、图片、地址、菜单的点击）
     * @param request
     * @param response
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/wechat", produces = "text/xml;charset=utf-8")
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        try {
            ServletInputStream stream = request.getInputStream();
            String input = WechatUtil.inputStream2String(stream);
            XStream content = XStreamFactory.init(false);
            content.alias("xml", InMessage.class);
            final InMessage message = (InMessage) content.fromXML(input);
            HttpSession session = request.getSession();
            session.setAttribute("openId", message.getFromUserName());
            switch (message.getMsgType()) {
                case "event":
                    if (message.getEvent().equals("subscribe")) {
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                Follower follower = WechatUtil.queryUserInfo(message.getFromUserName(), PlatformConfig.getAccessToken());
                                follower.setChannel("fuwu");
                                followerService.subscribe(follower);
                            }
                        };
                        thread.start();
                        content.alias("xml", Articles.class);
                        content.alias("item", Article.class);
                        Articles result = new Articles();
                        result.setFromUserName(message.getToUserName());
                        result.setToUserName(message.getFromUserName());
                        result.setCreateTime(new Date().getTime());
                        List<Article> list = subscribe();
                        result.setArticles(list);
                        result.setArticleCount(list.size());
                        content.processAnnotations(Article.class);
                        String xml = content.toXML(result);
                        logger.debug(JSON.toJSONString(xml));
                        return xml;
                    } else if (message.getEvent().equals("unsubscribe")) {
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                followerService.unsubscribe(message.getFromUserName());
                            }
                        };
                        thread.start();
                        return "";
                    } else if (message.getEvent().equalsIgnoreCase("click")) {
                        if (message.getEventKey().equalsIgnoreCase("unbind")) {
                            content.alias("xml", TextOutMessage.class);
                            TextOutMessage result = new TextOutMessage();
                            result.setFromUserName(message.getToUserName());
                            result.setToUserName(message.getFromUserName());
                            result.setCreateTime(new Date().getTime());
                            result.setContent("回复'解绑'即可完成操作");
                            String xml = content.toXML(result);
                            return xml;
                        }
                        if (message.getEventKey().equalsIgnoreCase("purchase")) {
                            content.alias("xml", TextOutMessage.class);
                            TextOutMessage result = new TextOutMessage();
                            result.setFromUserName(message.getToUserName());
                            result.setToUserName(message.getFromUserName());
                            result.setCreateTime(new Date().getTime());
                            result.setContent("欢迎您对云草纲目的关注,客户自助购买即将上线,敬请期待!");
                            String xml = content.toXML(result);
                            return xml;
                        }
                    }
                    break;
                case "text":
                    if (message.getContent().equals("解绑")) {
                        String openId = message.getFromUserName();
                        ResultData unbindResponse = agentService.unbindAgent(openId);
                        content.alias("xml", TextOutMessage.class);
                        TextOutMessage result = new TextOutMessage();
                        result.setFromUserName(message.getToUserName());
                        result.setToUserName(message.getFromUserName());
                        result.setCreateTime(new Date().getTime());
                        if (unbindResponse.getResponseCode() == ResponseCode.RESPONSE_OK) {
                            Subject subject = SecurityUtils.getSubject();
                            if (subject != null) {
                                subject.logout();
                            }
                            result.setContent("解绑成功");
                        } else {
                            result.setContent("您当前尚未绑定任何账户");
                        }
                        String xml = content.toXML(result);
                        return xml;
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return "";
    }

    /**
     * 生成用户关注自动回复的图文消息的私有方法
     * @return
     */
    private List<Article> subscribe() {
        List<Article> list = new ArrayList<>();
        Article welcome = new Article();
        welcome.setTitle("我们的故事｜关于家人关于爱");
        welcome.setDescription("云草纲目，每一瓶，都是家人与朋友满满的爱。");
        welcome.setPicUrl("https://mmbiz.qlogo.cn/mmbiz/zhe7KjM5iaS8Z1VmBFxR793iaJhia9fKCkz0BibJy4bWnLrhLlWHVAqibXGZQz1KiaqWBg6Ikzw7Mbs97EHq1bO6uZibw/0?wx_fmt=jpeg");
        welcome.setUrl("http://mp.weixin.qq.com/s?__biz=MzI1OTMyNTI1NQ==&mid=2247483666&idx=1&sn=84bdafae0d39c1c9d4c1b85bb8bf184a");
        list.add(welcome);
        Article guidance = new Article();
        guidance.setTitle("使用指南｜云草纲目代理商管理手册");
        guidance.setDescription("欢迎加入“云草纲目”大家庭，我们共同努力把最信任的佳品分享给身边最在乎的人。");
        guidance.setPicUrl("https://mmbiz.qlogo.cn/mmbiz/zhe7KjM5iaSibkQicWl47EG0pdFrtLBx96445SZ2qdVOLcl0Lbjn0SCibGO0MFHiccRaoniawAaM8JBmPdIiavbFKmRNA/0?wx_fmt=jpeg");
        guidance.setUrl("http://mp.weixin.qq.com/s?__biz=MzIwNjI1OTY2Mg==&mid=503177455&idx=1&sn=ad4c39e7083a36e89009e4f72ef4f139");
        list.add(guidance);
        Article product = new Article();
        product.setTitle("何为三七｜参中之王，千金不换");
        product.setDescription("现代研究发现，三七的化学成分和药理作用与人参相似，但其治疗外伤和心血管病的功能则是人参无法比拟的。因为人们对三七的药效的了解比人参晚了1000多年；所以，三七的名气没有人参那么大，但是他却是名副其实的参中之王。");
        product.setPicUrl("https://mmbiz.qlogo.cn/mmbiz/zhe7KjM5iaS8HCZAY1KWib9AHYx7fscNkiaic5ZJDKB1jY1xEmFxzCAlKvEDaSdwwCCLuv2EaE2SjDm8LDhU23a74Q/0?wx_fmt=jpeg");
        product.setUrl("http://mp.weixin.qq.com/s?__biz=MzIwNjI1OTY2Mg==&mid=2650661098&idx=1&sn=74b3f4690b182c295ba8b8ffc1e65b38");
        list.add(product);
        Article effect = new Article();
        effect.setTitle("参中之王｜三七功效，看这一篇就够啦！");
        effect.setDescription("看似矛盾的两种功能——止血与活血，竟然在三七上完美呈现，这是中药的神奇之处。三七这种看似矛盾的和谐，正是中国文化的核心，也是中药的核心。《中庸》说道，“致中和，天地位焉，万物育焉。血气不和，百病生。”三七是和血的良药。血气和，方能健康长寿。");
        effect.setPicUrl("https://mmbiz.qlogo.cn/mmbiz/zhe7KjM5iaS8h1soIe4bdE2y2aYpuTXQMjDycicIFo8AfCWVCdXrMk2MQlct4MmPOicKneBVfIz3rPhaItK3iapMzQ/0?wx_fmt=jpeg");
        effect.setUrl("https://mp.weixin.qq.com/s?__biz=MzI1OTMyNTI1NQ==&mid=2247483670&idx=1&sn=ff34c12e664c54bcb6a9135d8cb6a54c");
        list.add(effect);
        Article manufact = new Article();
        manufact.setTitle("制作工艺｜一颗三七的蜕变");
        manufact.setDescription("历经波澜，粉身碎骨，只为以最棒的姿态来到您的面前，每一粒三七粉都是满满的深情切意。");
        manufact.setPicUrl("https://mmbiz.qlogo.cn/mmbiz/zhe7KjM5iaS8Z1VmBFxR793iaJhia9fKCkzOXSogNWy72UyffanIVxDrWYNibWibVGODmpacwhCWRHBZTALzMfDCHpg/0?wx_fmt=png");
        manufact.setUrl("http://mp.weixin.qq.com/s?__biz=MzI1OTMyNTI1NQ==&mid=2247483673&idx=1&sn=2f5f641373d96503219dae12eda7f4d4");
        list.add(manufact);


        return list;
    }
}
