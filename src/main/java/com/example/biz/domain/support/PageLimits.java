package com.example.biz.domain.support;

/**
 * 平台级上限常量：<b>单一事实源</b>。
 * <p>分页上限、页码上界这类"防恶意请求拖垮数据库"的底线参数，历史上散落在 api 校验注解与仓储兜底钳制两处，
 * 改一处漏一处就会产生防护空洞。收敛到领域侧一个类，api 校验注解与 infrastructure 钳制共同引用同一份值。</p>
 * <p>放在 domain 是因为 api 与 infrastructure 都需要读它，而两者互相不可见（ArchUnit 分层规则）。</p>
 */
public final class PageLimits {

    /** 每页条数上限：api 层强校验 + 仓储层兜底钳制，两处同源 */
    public static final int MAX_PAGE_SIZE = 200;

    /** 页码上界：防止超大 offset 的深分页把数据库打穿 */
    public static final int MAX_PAGE_NO = 10000;

    private PageLimits() {
    }
}
