package com.github.peacetrue.modelgenerator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * 模型生成器maven插件
 *
 * @author xiayx
 */
@SpringBootApplication
@Mojo(name = "generate")
public class ModelGeneratorMojo extends AbstractMojo {

    /** 配置文件路径 */
    @Parameter(defaultValue = "file:${project.build.resources[0].directory}/application-datasource.properties")
    private String propertiesPath;
    /** 项目路径。默认为插件执行项目的父级项目 */
    @Parameter(defaultValue = "${project.basedir}/..")
    private String projectPath;

    public void execute() throws MojoExecutionException {
        getLog().info("从配置[" + propertiesPath + "]中读取模型信息并在目录[" + projectPath + "]下生成源码文件");
        try {
            this.executeThrowable();
        } catch (Exception e) {
            throw new MojoExecutionException("生成源码异常", e);
        }
    }

    private void executeThrowable() {
        SpringApplication springApplication = new SpringApplication(new DefaultResourceLoader(), ModelGeneratorMojo.class);
        springApplication.setEnvironment(new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                super.customizePropertySources(propertySources);

                HashMap<String, Object> projectPathSource = new HashMap<>(1);
                projectPathSource.put("peacetrue.model-generator.project-path", projectPath);
                propertySources.addLast(new MapPropertySource("projectPath", projectPathSource));

                try {
                    Resource resource = springApplication.getResourceLoader().getResource(propertiesPath);
                    propertySources.addLast(new ResourcePropertySource(resource));
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("读取配置[%s]异常", propertiesPath), e);
                }
            }
        });

        ConfigurableApplicationContext context = springApplication.run();
        ModelGenerator modelGenerator = context.getBean(ModelGenerator.class);
        ModelSupplier modelSupplier = context.getBean(ModelSupplier.class);
        List<Model> models = modelSupplier.getModels();
        getLog().info("共取得" + models.size() + "个模型信息");
        models.forEach(modelGenerator::generate);
        context.close();
    }
}
