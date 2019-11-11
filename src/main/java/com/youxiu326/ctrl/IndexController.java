package com.youxiu326.ctrl;

import com.youxiu326.common.websocket.WebSocketServer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class IndexController {

    /**
     * 测试websocket接收页面
     * @param model
     * @return
     */
    @GetMapping(value = {"/","index"})
    public String index(Model model){
        model.addAttribute("cid", 1000);
        return "index";
    }

    /**
     * 测试websocket推送数据接口
     * @param cid
     * @param message
     * @return
     */
    @ResponseBody
    @RequestMapping("/socket/push/{cid}")
    public Map pushToWeb(@PathVariable String cid, String message) {
        Map result = new HashMap();
        try {
            if (message==null)
                message = new Random().nextInt(1000)+"?random";
            WebSocketServer.sendInfo(message,cid);
            result.put("code", 200);
            result.put("msg", "success");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


} 