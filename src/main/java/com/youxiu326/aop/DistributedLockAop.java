package com.youxiu326.aop;

import com.youxiu326.common.result.JSONResult;
import com.youxiu326.lock.redis.RedissLockUtil;
import com.youxiu326.lock.zookeeper.ZookeeperLockUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 锁上移aop
 * order越小越是最先执行，但更重要的是最先执行的最后结束
 *   例如： 先获得锁-> 提交事务 ->释放锁
 *  [[[我是     {{我是 (业务 逻辑) 事务}}     锁]]]
 *
 */
@Order(100)
@Aspect
@Component
public class DistributedLockAop {

    /**
     * redis分布式锁切面
     * @param pjp
     * @return
     */
    @Around("execution(* com.youxiu326.service.SeckillDistributedService.redisLockSeckil(..))")
    public Object redisLockArround(ProceedingJoinPoint pjp) {
        Object o = null;
        boolean res=false;
        Object[] args = pjp.getArgs();
        Object seckillId = args[0];
        try {
            // 尝试获取锁，最多等待3秒，上锁以后20秒自动解锁
            res = RedissLockUtil.tryLock(seckillId+"", TimeUnit.SECONDS, 3, 20);
            if(res) {
                o = pjp.proceed();
            }else{
                return new JSONResult().FAIL;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException();
        } finally {
            if(res){//释放锁
                RedissLockUtil.unlock(seckillId+"");
            }
        }
        return o;
    }

    /**
     * zookeeper分布式锁切面
     * @param pjp
     * @return
     */
    @Around("execution(* com.youxiu326.service.SeckillDistributedService.zookeeperLockSeckil(..))")
    public Object zookeeperLockArround(ProceedingJoinPoint pjp) {
        Object o = null;
        boolean res=false;
        Object[] args = pjp.getArgs();
        Object seckillId = args[0];
        try {
            // 尝试获取锁，最多等待3秒，使用临时节点，会话关闭，节点删除
            res = ZookeeperLockUtil.acquire(3,TimeUnit.SECONDS);
            if(res) {
                o = pjp.proceed();
            }else{
                return new JSONResult().FAIL;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException();
        } finally {
            if(res){//释放锁
                ZookeeperLockUtil.release();
            }
        }
        return o;
    }



} 