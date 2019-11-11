package com.youxiu326.service.impl;

import com.youxiu326.common.dynamicquery.DynamicQuery;
import com.youxiu326.common.enums.SeckillStatEnum;
import com.youxiu326.common.result.JSONResult;
import com.youxiu326.entity.SuccessKilled;
import com.youxiu326.service.SeckillDistributedService;
import com.youxiu326.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.Date;

@Service
public class SeckillDistributedServiceImpl implements SeckillDistributedService {

    @Autowired
    private DynamicQuery dynamicQuery;

    @Autowired
    private RedisUtil redisUtil;

    @Transactional
    @Override
    public JSONResult redisLockSeckil(long seckillId, long userId) {
        return seckill(seckillId,userId);
    }

    @Transactional
    @Override
    public JSONResult zookeeperLockSeckil(long seckillId, long userId) {
        return seckill(seckillId,userId);
    }

    @Override
    public Long getSeckillCount(long seckillId) {
        String nativeSql = "SELECT count(*) FROM success_killed WHERE seckill_id=?";
        Object object =  dynamicQuery.nativeQueryObject(nativeSql, new Object[]{seckillId});
        return ((Number) object).longValue();
    }

    @Override
    @Transactional
    public void deleteSeckill(long seckillId) {
        String nativeSql = "DELETE FROM  success_killed WHERE seckill_id=?";
        dynamicQuery.nativeExecuteUpdate(nativeSql, new Object[]{seckillId});
        nativeSql = "UPDATE seckill SET number =100 WHERE seckill_id=?";
        dynamicQuery.nativeExecuteUpdate(nativeSql, new Object[]{seckillId});
    }

    /**
     * SHOW STATUS LIKE 'innodb_row_lock%';
     * 如果发现锁争用比较严重，如InnoDB_row_lock_waits和InnoDB_row_lock_time_avg的值比较高
     */
    @Override
    @Transactional
    public JSONResult seckilDBPCC_TWO(long seckillId, long userId) {
        JSONResult result = new JSONResult();

        // 单用户抢购一件商品没有问题、但是抢购多件商品不建议这种写法
        // UPDATE锁表
        String nativeSql = "UPDATE seckill SET number=number-1 WHERE seckill_id=? AND number>0";
        int count = dynamicQuery.nativeExecuteUpdate(nativeSql, new Object[]{seckillId});
        if(count>0){
            SuccessKilled killed = new SuccessKilled();
            killed.setSeckillId(seckillId);
            killed.setUserId(userId);
            killed.setState((short)0);
            killed.setCreateTime(new Timestamp(new Date().getTime()));
            dynamicQuery.save(killed);
            return result.SUCCEED(SeckillStatEnum.SUCCESS);
        }else{
            return result.FAIL(SeckillStatEnum.END);
        }
    }

    @Override
    @Transactional
    public JSONResult startSeckil(long seckillId, long userId) {
        return seckill(seckillId,userId);
    }

    /**
     * 秒杀业务逻辑抽取
     * @param seckillId
     * @param userId
     * @return
     */
    private JSONResult seckill(long seckillId, long userId){
        JSONResult result = new JSONResult();

        String nativeSql = "SELECT number FROM seckill WHERE seckill_id=?";
        Object object =  dynamicQuery.nativeQueryObject(nativeSql, new Object[]{seckillId});
        if(object == null){
            return result.FAIL(SeckillStatEnum.DATE_BAD);
        }
        Long number =  ((Number) object).longValue();
        if(number>0){
            SuccessKilled killed = new SuccessKilled();
            killed.setSeckillId(seckillId);
            killed.setUserId(userId);
            killed.setState((short)0);
            killed.setCreateTime(new Timestamp(new Date().getTime()));
            dynamicQuery.save(killed);
            nativeSql = "UPDATE seckill  SET number=number-1 WHERE seckill_id=? AND number>0";
            int num = dynamicQuery.nativeExecuteUpdate(nativeSql, new Object[]{seckillId});
            if (num == 0){
                // 秒杀结束标识
                redisUtil.cacheValue(seckillId+"", "ok");
                return result.FAIL(SeckillStatEnum.END);
            }
        }else{
            // 秒杀结束标识
            redisUtil.cacheValue(seckillId+"", "ok");
            return result.FAIL(SeckillStatEnum.END);
        }

        return result.SUCCEED(SeckillStatEnum.SUCCESS);
    }

}