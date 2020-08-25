package com.hrun;

import com.alibaba.fastjson.JSON;
import com.hrun.component.Meta_data.Meta_data;
import com.hrun.component.api.Api;
import com.hrun.component.common.*;
import com.hrun.component.intf.*;
import com.hrun.component.testcase.TestCase;
import com.hrun.exceptions.HrunBizException;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyContent;
import lombok.Data;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Data
public class Runner {

    static Logger logger = LoggerFactory.getLogger(Runner.class);

    // 如果执行的是run_testcase，那么meta_data是list，list中可能是Meta_data对象，
    // 也可能是嵌套了Meta_data的List
    // 如果执行的是_run_test，那么meta_data是Meta_data类对象。
    private Object meta_datas;
    //TODO:check it
//    private List<Meta_data> meta_datas = new ArrayList<>();

    private Boolean verify;

    private Export export;

    private Variables config_variables;

    private SetupHooks testcase_setup_hooks;

    private TeardownHooks testcase_teardown_hooks;

    //用来保存会话，包括cookie
    private HttpSession http_client_session;

    //用来保存同一个用户中的变量，参数
    private SessionContext session_context;

    public Runner(Config config) {
        this(config, null);
    }

    private String exception_request_type;

    private String exception_name;

    Map validation_results;

    public Runner(Config config, HttpSession http_client_session) {
        this.verify = Optional.ofNullable(config.getVerify()).orElse(true);
        this.export = Optional.ofNullable(config.getExport()).orElse(new Export());

        this.config_variables = config.getVariables();
        this.testcase_setup_hooks = config.getSetup_hooks();
        this.testcase_teardown_hooks = config.getTeardown_hooks();

        try {
            this.http_client_session = Optional.ofNullable(http_client_session)
                    .orElse(new HttpSession());
        } catch (Exception e) {
            logger.error("创建HttpSession对象失败");
        }

        this.session_context = new SessionContext(config_variables);
        Optional.ofNullable(testcase_setup_hooks).ifPresent(hook -> do_hook_actions(hook, "setup"));
    }

    public void do_hook_actions(Hooks actions, String hook_type) {
        // TODO:
        //  httprunner原生支持两种格式的hook，一种是
        //   {"var": "${func()}"}
        //  另一种是
        //   ${func()}
        //  暂时只支持${func()}形式
        logger.debug(String.format("call %s hook actions.", hook_type));

        this.session_context.eval_content(actions);
    }

    public void run_test(RunableComponent test_dict) throws Exception {
        this.meta_datas = null;

        if (test_dict instanceof TestCase) {
            // nested testcase
            if (test_dict.getConfig() == null) {
                ((TestCase) test_dict).setConfig(new Config());
            }
            test_dict.getConfig().getVariables().extend(session_context.getSession_variables_mapping());
            _run_testcase((TestCase) test_dict);
        } else if (test_dict instanceof Api) {
            // self.validation_results = {}
            try {
                _run_test((Api) test_dict);
            } catch (Exception e) {
                // log exception request_type and name for locust stat
                exception_request_type = ((Api) test_dict).getRequest().getMethod().getEvalString();
                exception_name = ((Api) test_dict).getName().getEvalString();
                throw e;
            } finally {
                // get request/response data and validate results
                this.meta_datas = this.http_client_session.getMeta_data();
                ((Meta_data) meta_datas).setValidators(validation_results);
            }
        }
    }

    public void _run_test(Api test_dict) throws Exception {
        this.__clear_test_data();

        this._handle_skip_feature(test_dict);

        //TODO:很多内容这里没有做，后面还需要完善
        Variables test_variables = test_dict.getVariables();
        this.session_context.init_test_variables(test_variables);

        String test_name = this.session_context.eval_content(test_dict.getName()).getEvalString();
//        String test_name = test_dict.getName().toString();//TODO:要像上一行一样，加载变量或者方法


        Request raw_request = test_dict.getRequest();
        Request parsed_test_request = (Request) this.session_context.eval_content(raw_request);
        this.session_context.update_test_variables("request", parsed_test_request);

        String url = parsed_test_request.getUrl().getEvalString();
        String base_url = "";
        if (Optional.ofNullable(test_dict.getBase_url()).isPresent()) {
            this.session_context.eval_content(test_dict.getBase_url()).getEvalString();
        }
        String parsed_url = Utils.build_url(base_url, url);

        SetupHooks setup_hooks = test_dict.getSetup_hooks();
        if(Optional.ofNullable(setup_hooks).isPresent())
            this.do_hook_actions(setup_hooks,"setup");

        String method = "";
        try {
            method = String.valueOf(Optional.of(parsed_test_request.getMethod()).get().getEvalValue());
            Optional.ofNullable(parsed_test_request.getVerify()).orElseGet(() -> {
                parsed_test_request.setVerify(this.verify);
                return null;
            });
            //TODO: group什么作用？group_name = parsed_test_request.getGroup();
        } catch (Exception e) {
            HrunExceptionFactory.create("E0019");
        }

        //# TODO: move method validation to json schema
        List<String> valid_methods = Arrays.asList("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        if (!valid_methods.contains(method.toUpperCase())) {
            logger.error("Invalid HTTP method! => " + method);
            logger.error("Available HTTP methods: " + valid_methods);
            HrunExceptionFactory.create("E0020");
        }
        logger.info(method + " " + url);
        //TODO:检查所有request包含的组件是否有tostring
        logger.debug(
                String.format("request kwargs(raw): %s ", JSON.toJSONString(parsed_test_request)));
        CloseableHttpResponse resp = null;
        try {
            resp = this.http_client_session.request(
                    method,
                    parsed_url,
                    test_name,
                    parsed_test_request
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (resp == null) {
            HrunExceptionFactory.create("E0036");
        }
        ResponseObject resp_obj = new ResponseObject(resp);

        // teardown hooks
        TeardownHooks teardown_hooks = test_dict.getTeardown_hooks();
        if(Optional.ofNullable(teardown_hooks).isPresent()) {
            //TODO:要把resp_obj对象更新在session_context中，方便后续引用该response对象
//            this.session_context.update_test_variables("response", resp_obj);
            this.do_hook_actions(teardown_hooks, "teardown");
        }

        // extract
        Extract extractors = test_dict.getExtract();
        try {
            Map<String, Object> extracted_variables_mapping = new HashMap<>();
            extracted_variables_mapping = resp_obj.extract_response(extractors,(String)this.http_client_session.getMeta_data().getData().get(0).getResponse().get("body"));
            this.session_context.update_session_variables(new Variables(extracted_variables_mapping));
        } catch (HrunBizException e) {
            //TODO: log_req_resp_details()
            HrunExceptionFactory.create("E0038");
        }


        // validate
        List<com.hrun.lazyContent.LazyFunction> validators = test_dict.getValidate().getPrepared_validators();
        Validator validator = new Validator(this.session_context, resp_obj);
        try {
            validator.validate(validators,(String)this.http_client_session.getMeta_data().getData().get(0).getResponse().get("body"));
        } catch (HrunBizException e) {
            HrunExceptionFactory.create("E0039");
        } finally {
            validation_results = validator.getValidation_results();
        }
    }

    public void __clear_test_data() {
        this.http_client_session.init_meta_data();
    }

    public void _handle_skip_feature(Api test_dict) {
        String skip_reason = null;
        if (Optional.ofNullable(test_dict.getSkip()).isPresent()) {
            skip_reason = String.valueOf(test_dict.getSkip().to_value());
        }
        //TODO: containsSkipIf  containsSkipUnless
//        else if(test_dict.containsSkipIf())
//        else if(test_dict.containsSkipUnless())

        String finalSkip_reason = skip_reason;
        Optional.ofNullable(skip_reason).ifPresent(reason -> {
            logger.info(" skip_reasonis " + finalSkip_reason);
            HrunExceptionFactory.create("E0001");
        });
    }

    public void _run_testcase(TestCase testcase_dict) throws Exception {
        Config config = Optional.ofNullable(testcase_dict.getConfig()).orElse(new Config());

        // each teststeps in one tstcase (YAML/JSON) share the same sessieon.
        Runner test_runner = new Runner(config, this.http_client_session);

        List<RunableComponent> tests = testcase_dict.getTestSteps();
        for (int i = 0; i < tests.size(); i++) {
            // override current teststep variables with former testcase output variables
            Variables former_output_variables = session_context.getTest_variables_mapping();
            if (former_output_variables != null) {
                if (tests.get(i).getVariables() != null) {
                    tests.get(i).getVariables().extend(former_output_variables);
                } else {
                    tests.get(i).setVariables(former_output_variables);
                }
            }
            try {
                test_runner.run_test(tests.get(i));
            } catch (Exception e) {
                // log exception request_type and name for locust stat
                this.exception_request_type = test_runner.getException_request_type();
                this.exception_name = test_runner.getException_name();
                throw e;
            } finally {
                Object _meta_datas = test_runner.getMeta_datas();
                ((List) this.meta_datas).add(_meta_datas);
            }
        }

        session_context.update_session_variables(
                test_runner.export_variables(test_runner.export)
        );

    }

    public Variables export_variables(Export output_variables_list) {
        //export current testcase variables
        Variables variables_mapping = session_context.getSession_variables_mapping();
        Map<String, LazyContent> output = new HashMap<>();

        for (String variable : output_variables_list.getExport()) {
            if (!variables_mapping.getVariables().keySet().contains(variable)) {
                logger.warn(
                        "variable '{}' can not be found in variables mapping, " +
                                "failed to export!".format(variable)
                );
                continue;
            }
            output.put(variable, variables_mapping.getVariable(variable));
        }

        logger.info(JSON.toJSONString(output));
        return new Variables(output);
    }

}
