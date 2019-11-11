package com.youxiu326.queue;

import com.youxiu326.common.enums.SeckillStatEnum;
import com.youxiu326.common.result.JSONResult;
import com.youxiu326.common.websocket.WebSocketServer;
import com.youxiu326.service.SeckillDistributedService;
import com.youxiu326.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class ActiveMQConsumer {

    @Autowired
    private SeckillDistributedService seckillService;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private RedisUtil redisUtil;

    // 使用JmsListener配置消费者监听的队列，其中text是接收到的消息
    @JmsListener(destination = "seckill.queue")
    public void receiveQueue(String message) throws IOException {
        //收到通道的消息之后执行秒杀操作(超卖)
        String[] array = message.split(";");
        // JSONResult result = seckillService.startSeckil(Long.parseLong(array[0]), Long.parseLong(array[1]));
        JSONResult result = seckillService.seckilDBPCC_TWO(Long.parseLong(array[0]), Long.parseLong(array[1]));

        if(result.get("msg").equals(SeckillStatEnum.SUCCESS.toString())){
            webSocketServer.sendInfo(array[0].toString(), "秒杀成功");//推送给前台
        }else{
            webSocketServer.sendInfo(array[0].toString(), "秒杀失败");//推送给前台
            redisUtil.cacheValue(array[0], "ok");//秒杀结束
        }
    }
} 