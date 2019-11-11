package com.youxiu326.queue.redis;

import com.youxiu326.common.enums.SeckillStatEnum;
import com.youxiu326.common.result.JSONResult;
import com.youxiu326.common.websocket.WebSocketServer;
import com.youxiu326.service.SeckillDistributedService;
import com.youxiu326.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;

/**
 * redis队列消费者
 */
@Service
public class RedisConsumer {
	
	@Autowired
	private SeckillDistributedService seckillService;

	@Autowired
	private RedisUtil redisUtil;

    public void receiveMessage(String message) throws IOException {
		//Thread th=Thread.currentThread();
		//System.out.println("Tread name:"+th.getName());
        //收到通道的消息之后执行秒杀操作(超卖)
    	String[] array = message.split(";"); 
    	if(redisUtil.getValue(array[0])==null){//control层已经判断了，其实这里不需要再判断了

    		JSONResult result = seckillService.seckilDBPCC_TWO(Long.parseLong(array[0]), Long.parseLong(array[1]));
    		if(result.get("msg").equals(SeckillStatEnum.SUCCESS.toString())){
    			WebSocketServer.sendInfo(array[0], "秒杀成功");//推送给前台
    		}else{
    			WebSocketServer.sendInfo(array[0], "秒杀失败");//推送给前台
    			redisUtil.cacheValue(array[0], "ok");//秒杀结束
    		}

    	}else{
			redisUtil.cacheValue(array[0], "ok");//秒杀结束
    		WebSocketServer.sendInfo(array[0], "秒杀失败");//推送给前台
    	}
    }
}