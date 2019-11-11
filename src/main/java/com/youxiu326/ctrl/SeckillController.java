package com.youxiu326.ctrl;

import com.youxiu326.common.enums.SeckillStatEnum;
import com.youxiu326.common.result.JSONResult;
import com.youxiu326.service.SeckillDistributedService;
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
import java.util.concurrent.CountDownLatch;
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

    @Autowired
    private SeckillDistributedService seckillService;

    private static int corePoolSize = Runtime.getRuntime().availableProcessors();

    //调整队列数 拒绝服务 【AbortPolicy 拒绝任务，并抛出异常，为默认的策略】
    private static ThreadPoolExecutor executor  = new ThreadPoolExecutor(corePoolSize, corePoolSize+1, 10l, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000));

    private final static int skillNum = 1000;

    @ApiOperation(value="Redis分布式锁", notes="秒杀1->Redis分布式锁")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "seckillId", value = "商品id", required = true, dataType = "long",paramType = "query"),
    })
    @PostMapping(value="/redislook/pay",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONResult redislookPay(long seckillId){

        // 先删除记录
        seckillService.deleteSeckill(seckillId);
        redisUtil.cacheValue(seckillId+"", null);//秒杀结束

        final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
        for(int i=0;i<skillNum;i++){
            final long userId = i;
            Runnable task = () -> {
                if(redisUtil.getValue(seckillId+"")==null){
                    JSONResult result = seckillService.redisLockSeckil(seckillId, userId);
                    LOGGER.info("用户:{}{}",userId,result.get("msg"));
                }else{
                    // 秒杀结束
                }
                latch.countDown();
            };
            executor.execute(task);
        }
        try {
            // 等待所有人任务结束
            latch.await();
            Long  seckillCount = seckillService.getSeckillCount(seckillId);
            LOGGER.info("一共秒杀出{}件商品",seckillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new JSONResult().SUCCEED();
    }


    @ApiOperation(value="Zookeeper分布式锁", notes="秒杀2->Zookeeper分布式锁")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "seckillId", value = "商品id", required = true, dataType = "long",paramType = "query"),
    })
    @PostMapping(value="/zookeeperlook/pay",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONResult zookeeperlookPay(long seckillId){

        // 先删除记录
        seckillService.deleteSeckill(seckillId);
        redisUtil.cacheValue(seckillId+"", null);//秒杀结束

        final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
        for(int i=0;i<skillNum;i++){
            final long userId = i;
            Runnable task = () -> {
                if(redisUtil.getValue(seckillId+"")==null){
                    JSONResult result = seckillService.zookeeperLockSeckil(seckillId, userId);
                    LOGGER.info("用户:{}{}",userId,result.get("msg"));
                }else{
                    // 秒杀结束
                }
                latch.countDown();
            };
            executor.execute(task);
        }

        try {
            // 等待所有人任务结束
            latch.await();
            Long  seckillCount = seckillService.getSeckillCount(seckillId);
            LOGGER.info("一共秒杀出{}件商品",seckillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new JSONResult().SUCCEED();
    }

    @ApiOperation(value="ActiveMQ分布式队列秒杀", notes="秒杀5->ActiveMQ分布式队列秒杀")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "seckillId", value = "商品id", required = true, dataType = "long",paramType = "query"),
    })
    @PostMapping(value="/activemq/pay",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONResult activemqPay(long seckillId){

        // 先删除记录
        seckillService.deleteSeckill(seckillId);
        redisUtil.cacheValue(seckillId+"", null);//秒杀结束

        final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
        for(int i=0;i<skillNum;i++){
            final long userId = i;
            Runnable task = () -> {
                if(redisUtil.getValue(seckillId+"")==null){
                    Destination destination = new ActiveMQQueue("seckill.queue");
                    // destination是发送到的队列，message是待发送的消息
                    jmsTemplate.convertAndSend(destination,seckillId+";"+userId);
                }else{
                    // 秒杀结束
                }
                latch.countDown();
            };
            executor.execute(task);
        }

        try {
            // 等待所有人任务结束
            latch.await();
            Long  seckillCount = seckillService.getSeckillCount(seckillId);
            LOGGER.info("一共秒杀出{}件商品",seckillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new JSONResult().SUCCEED();
    }


} 