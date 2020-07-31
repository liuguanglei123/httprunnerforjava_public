package com.hrun;

import com.beust.jcommander.JCommander;
import com.hrun.HrunArgsParse.ArgsParse;
import com.hrun.report.GenerateReport;
import com.hrun.report.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Hello httprunner!
 *
 */
public class App 
{

    static public String version = "2.2.6";

    static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {
        ArgsParse argsParse = new ArgsParse();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(argsParse)
                .build();

        try {
            jCommander.parse(args);
        }catch(Exception e){
            logger.error("不支持的命令行参数，请输入正确的命令！");
            jCommander.usage();
            return;
        }

        if(args == null || args.length == 0 || (argsParse.getHelp() != null && argsParse.getHelp() == true)){
            jCommander.usage();
            return;
        }

        if(argsParse.getVersion() != null) {
            logger.info(version);
            return;
        }

        if(argsParse.getTestcase_paths() == null || argsParse.getTestcase_paths().size() == 0 ){
            logger.error("输入的参数中，testcase_path为空，无法执行");
            return;
        }
        logger.info("以下是所有的执行目录/文件");
        Optional.ofNullable(argsParse.getTestcase_paths()).get().stream().forEach(str -> logger.info(str));

        //TODO:需要完成格式化校验/美化功能
        /* if args.validate:
            validate_json_file(args.validate)
            exit(0)
        if args.prettify:
            prettify_json_file(args.prettify)
            exit(0)
         */

        //TODO:需要完成创建项目目录结构功能
        /* project_name = args.startproject
        if project_name:
            create_scaffold(project_name)
            exit(0)
         */

        HttpRunner runner = new HttpRunner(argsParse.getFailfast(),argsParse.getSave_test(),
                argsParse.getLog_level(),argsParse.getLog_file());

        //TODO: err_code=0
        try {
            for(String path : argsParse.getTestcase_paths()){
                Summary summary = runner.run_path(path, argsParse.getDot_env_path());
                String report_dir = Optional.ofNullable(argsParse.getReport_dir()).orElseGet(() ->
                        Paths.get(runner.getProject_working_directory()).resolve("reportss").toString()
                );
//                String report_template = "src/main/resources/" + "template.html";
                String report_template = "template.html";
                logger.debug("report_dir: "+report_dir);

                GenerateReport.gen_html_report(
                        summary,
                        report_template,
                        report_dir,
                        argsParse.getReport_file()
                );
            }
        }
        catch(Exception e){
            logger.error(String.format("!!!!!!!!!! exception stage: %s !!!!!!!!!!",runner.getException_stage()));
            logger.error("Exception thrown  :" + e);
        }

        System.out.println( "Hello hrunjava v2 !" );
    }

}
