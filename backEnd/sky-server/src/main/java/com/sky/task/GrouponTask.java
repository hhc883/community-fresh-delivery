package com.sky.task;

import com.sky.context.BaseContext;
import com.sky.service.GrouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拼团超时处理定时任务
 */
@Component
@Slf4j
public class GrouponTask {

    @Autowired
    private GrouponService grouponService;

    /**
     * 关闭超过截止时间仍未成团的拼团，并取消/退款相关订单、归还库存。
     * 每 5 分钟执行一次。
     * <p>
     * 定时任务线程没有经过 JWT 拦截器，BaseContext 中无用户 id，
     * 而 DishMapper.update 等带 @AutoFill 的方法会读取它，
     * 因此这里显式填入「系统操作人」= 0，并在 finally 中清理
     * （@Scheduled 默认单线程池会复用线程，不清理会污染后续任务）。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processExpiredGroupon() {
        try {
            BaseContext.setCurrentId(0L);
            int count = grouponService.closeExpired();
            if (count > 0) {
                log.info("拼团超时处理完成，共关闭 {} 个未成团的团", count);
            }
        } catch (Exception e) {
            log.error("拼团超时处理失败", e);
        } finally {
            BaseContext.removeCurrentId();
        }
    }
}
