package com.youxiu326.ctrl;

import com.youxiu326.common.result.JSONResult;
import com.youxiu326.util.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.Destination;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀controller
 * @link https://gitee.com/52itstyle/spring-boot-seckill
 */
@Api(value="分布式秒杀",tags={"分布式秒杀接口"})
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final static Logger LOGGER = LoggerFactory.getLogger(SeckillController.class);

    @Autowired
    private JmsMessagingTemplate jmsTemplate;

    @Autowired
    private RedisUtil redisUtil;

    private static int corePoolSize = Runtime.getRuntime().availableProcessors();

    //调整队列数 拒绝服务 【AbortPolicy 拒绝任务，并抛出异常，为默认的策略】
    private static ThreadPoolExecutor executor  = new ThreadPoolExecutor(corePoolSize, corePoolSize+1, 10l, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000));


    @ApiOperation(value="Rediss分布式锁", notes="秒杀1->Rediss分布式锁")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "killId", value = "商品id", required = true, dataType = "long",paramType = "query"),
    })
    @PostMapping(value="/redislook/pay",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONResult redislookPay(long killId){
        JSONResult result = new JSONResult();

        for(int i=0;i<1000;i++){
            final long userId = i;
            Runnable task = () -> {
                if(redisUtil.getValue(killId+"")==null){
                    Destination destination = new ActiveMQQueue("seckill.queue");
                    // destination是发送到的队列，message是待发送的消息
                    jmsTemplate.convertAndSend(destination,killId+";"+userId);
                }else{
                    //秒杀结束
                }
            };
            executor.execute(task);
        }

        return result;
    }


    @ApiOperation(value="ActiveMQ分布式队列秒杀", notes="秒杀5->ActiveMQ分布式队列秒杀")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "killId", value = "商品id", required = true, dataType = "long",paramType = "query"),
    })
    @PostMapping(value="/activemq/pay",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONResult activemqPay(long killId){
        JSONResult result = new JSONResult();

        for(int i=0;i<1000;i++){
            final long userId = i;
            Runnable task = () -> {
                if(redisUtil.getValue(killId+"")==null){
                    Destination destination = new ActiveMQQueue("seckill.queue");
                    // destination是发送到的队列，message是待发送的消息
                    jmsTemplate.convertAndSend(destination,killId+";"+userId);
                }else{
                    //秒杀结束
                }
            };
            executor.execute(task);
        }

        return result;
    }


} 