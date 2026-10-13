package com.example.biz.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架构守护测试：规范里的"依赖规则（硬性约束）"在 CI 强制执行，违反即构建失败。
 */
@AnalyzeClasses(packages = "com.example.biz")
class ArchitectureTest {

    /**
     * 分层依赖方向：api -> application -> domain，infrastructure 依赖 domain（依赖倒置）。
     * <p>domain 允许所有外层只读访问（api 组装视图 / 翻译领域异常），单向性不破坏；
     * 禁止的是反方向——任何层不得反向触碰 api 与 infrastructure，application 只能被 api 触碰。</p>
     */
    @ArchTest
    static final ArchRule layerDependencyRule = layeredArchitecture().consideringAllDependencies()
            .layer("Api").definedBy("..biz.api..")
            .layer("Application").definedBy("..biz.application..")
            .layer("Domain").definedBy("..biz.domain..")
            .layer("Infrastructure").definedBy("..biz.infrastructure..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Api", "Application", "Infrastructure")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();

    /** domain 层零框架依赖：不出现 Spring / Jakarta / MyBatis / Swagger / Lombok / PageHelper */
    @ArchTest
    static final ArchRule domainShouldBeFrameworkFree = noClasses().that().resideInAPackage("..biz.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "org.apache.ibatis..",
                    "io.swagger..",
                    "com.github.pagehelper..",
                    "lombok..");

    /** domain 层不得依赖工程内其他任何层 */
    @ArchTest
    static final ArchRule domainShouldNotDependOnOtherLayers = noClasses().that().resideInAPackage("..biz.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..biz.api..", "..biz.application..", "..biz.infrastructure..");

    /** 聚合根与领域服务各自内聚，不与其他业务包横向纠缠（按聚合切分） */
    @ArchTest
    static final ArchRule domainSlicesShouldBeFreeOfCycles =
            SlicesRuleDefinition.slices().matching("com.example.biz.(*)..").should().beFreeOfCycles();
}
