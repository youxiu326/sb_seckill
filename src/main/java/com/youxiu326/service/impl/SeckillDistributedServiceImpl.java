package com.youxiu326.service.impl;

import com.youxiu326.common.result.JSONResult;
import com.youxiu326.service.SeckillDistributedService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeckillDistributedServiceImpl implements SeckillDistributedService {

    @Transactional
    @Override
    public JSONResult redisLockSeckil(long seckillId, long userId) {
        return null;
    }

    @Transactional
    @Override
    public JSONResult zookeeperLockSeckil(long seckillId, long userId) {
        return null;
    }
}