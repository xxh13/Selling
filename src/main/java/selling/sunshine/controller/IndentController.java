package selling.sunshine.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import selling.sunshine.service.IndentService;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by sunshine on 7/7/16.
 */
@RestController
@RequestMapping("/indent")
public class IndentController {
    private Logger logger = LoggerFactory.getLogger(IndentController.class);

    @Autowired
    private IndentService indentService;

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/template")
    public String indent(HttpServletRequest request) {
        String context = request.getSession().getServletContext().getRealPath("/");
        indentService.produce();

        return "";
    }
}