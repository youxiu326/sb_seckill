package com.youxiu326.service.impl;

import com.youxiu326.common.dynamicquery.DynamicQuery;
import com.youxiu326.common.enums.SeckillStatEnum;
import com.youxiu326.common.result.JSONResult;
import com.youxiu326.entity.SuccessKilled;
import com.youxiu326.service.SeckillDistributedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.Date;

@Service
public class SeckillDistributedServiceImpl implements SeckillDistributedService {

    @Autowired
    private DynamicQuery dynamicQuery;

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
        Long number =  ((Number) object).longValue();
        if(number>0){
            SuccessKilled killed = new SuccessKilled();
            killed.setSeckillId(seckillId);
            killed.setUserId(userId);
            killed.setState((short)0);
            killed.setCreateTime(new Timestamp(new Date().getTime()));
            dynamicQuery.save(killed);
            nativeSql = "UPDATE seckill  SET number=number-1 WHERE seckill_id=? AND number>0";
            dynamicQuery.nativeExecuteUpdate(nativeSql, new Object[]{seckillId});
        }else{
            return result.FAIL(SeckillStatEnum.END);
        }

        return result.SUCCEED(SeckillStatEnum.SUCCESS);
    }

}