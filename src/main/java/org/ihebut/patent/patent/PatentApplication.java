package org.ihebut.patent.patent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口。
 *
 * <p>提供专利库、专家库、需求匹配、成果展示与价值评估等后端API。</p>
 */
@SpringBootApplication
public class PatentApplication {

    /**
     * Spring Boot 启动方法。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PatentApplication.class, args);
    }

}
