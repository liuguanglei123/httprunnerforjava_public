package com.hrun.HrunArgsParse;

import com.beust.jcommander.Parameter;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArgsParse {

    @Parameter(names={ "-V", "--version"},description = "show version",help=true)
    private Boolean version;

    //TODO:1
    @Parameter(names="testcase_paths",description = "testcase file path", converter=ArgsParseListConverter.class)
    private ArrayList<String> testcase_paths;

    @Parameter(names="--log-level",description = "Specify logging level, default is INFO.")
    private String log_level = "INFO";

    @Parameter(names="--log-file",description = "Write logs to specified file path.")
    private String log_file;

    @Parameter(names="--dot-env-path",description = "Specify .env file path, which is useful for keeping sensitive data.")
    private String dot_env_path;

    @Parameter(names="--report-template",description = "specify report template path.")
    private String report_template;

    @Parameter(names="--report-dir",description = "specify report save directory.")
    private String report_dir;

    @Parameter(names="--report-file",description = "specify report file name.")
    private String report_file;

    //TODO: 这里的help=true是不是加错了？
    @Parameter(names="--failfast",description = "Stop the test run on the first error or failure.", help=true)
    private Boolean failfast = true;

    @Parameter(names="--save-tests",description = "Save loaded tests and parsed tests to JSON file.", help=true)
    private Boolean save_test = true;

    @Parameter(names="--startproject",description = "Specify new project name.")
    private String startproject;

    @Parameter(names="--validate",description = "Validate JSON testcase format.")
    private String validate;

    @Parameter(names="--prettify",description = "Prettify JSON testcase format.")
    private String prettify;

    @Parameter(names="--help",description = "Show help info.", help=true)
    private Boolean help;

    public List<String> getTestcase_paths(){
        return this.testcase_paths;
    }

}
