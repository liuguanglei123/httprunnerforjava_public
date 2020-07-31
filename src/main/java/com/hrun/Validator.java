package com.hrun;

import com.hrun.exceptions.HrunBizException;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Data
public class Validator {
    static Logger logger = LoggerFactory.getLogger(Validator.class);

    private SessionContext session_context;

    private ResponseObject resp_obj;

    private List<Validate_extractor> validate_extractor_list = new ArrayList<Validate_extractor>();

    private Map validation_results = new HashMap<String,Object>(){{
        put("validate_script",null); //未实现
        put("validate_extractor",validate_extractor_list);
    }};

    @Data
    class Validate_extractor{
        private String comparator;
        private String check;
        private Object check_value;
        private Object expect;
        private Object expect_value;
        private String check_result;
    }

    public Validator(SessionContext session_context, ResponseObject resp_obj){
        this.session_context = session_context;
        this.resp_obj = resp_obj;
    }

    public Object __eval_validator_check(LazyString check_item,String respBody){
        //TODO: 看看hrun的源码注释吧，支持的类型实在是太多了，暂时只能支持hrun注释中的第四种类型
        Object check_value;
        if(check_item.getIsLazyString())
            check_value = this.session_context.eval_content(check_item).getEvalValue();
        else
            check_value = this.resp_obj.extract_field(check_item.getRaw_value(),respBody);
        return check_value;
    }

    public Object __eval_validator_expect(Object expect_item){
        if(expect_item instanceof LazyString)
            return this.session_context.eval_content((LazyString)expect_item).getEvalValue();
        else
            return expect_item;
    }

    public void validate(List<LazyFunction> validators,String respBody) throws Exception {
        if(validators == null || validators.isEmpty())
            return;

        logger.debug("start to validate.");

        Boolean validate_pass = true;
        List<String> failures = new ArrayList<String>();

        for(LazyFunction validator : validators){
            //TODO:python_script
            List<Object> validate_extractor = new ArrayList<>();

            if(!(validator instanceof LazyFunction)) {
                HrunExceptionFactory.create("E0025");
                logger.error(String.format("validator should be parsed first: %s"),validators);
            }

            // evaluate validator args with context variable mapping.
            List<LazyContent> validator_args = validator.get_args();
            if(!(validator_args.get(0) instanceof LazyString))
                HrunExceptionFactory.create("E0035");
            LazyString check_item = (LazyString)validator_args.get(0);
            LazyContent expect_item = validator_args.get(1);
            Object check_value = this.__eval_validator_check(check_item,respBody);
            Object expect_value = null;
            if(expect_item instanceof LazyString)
                expect_value = this.__eval_validator_expect(expect_item);
            else
                expect_value = expect_item.getEvalValue();

            List<Object> calcArgs = new ArrayList<Object>();
            calcArgs.add(check_value);
            calcArgs.add(expect_value);
            validator.setArgs(calcArgs);

            String comparator = validator.getFunc_name();
            Map validator_dict = new HashMap<String,Object>();
            validator_dict.put("comparator",comparator);
            validator_dict.put("check",check_item.getRaw_value());
            validator_dict.put("check_value",check_value);
            validator_dict.put("expect",expect_item.getRaw_value());
            validator_dict.put("expect_value",expect_value);

            String validate_msg = String.format("\nvalidate: %s %s %s(%s)",
                    check_item.getRaw_value(),
                    comparator,
                    expect_value,
                    expect_value.getClass().getSimpleName()
            );

            try {
                validator.to_value(this.session_context.getTest_variables_mapping());
                validator_dict.put("check_result", "pass");
                validate_msg += "\t==> pass";
                logger.debug(validate_msg);
            }
//            }catch(AssertionError e){
//                validate_pass = false;
//                validator_dict.put("check_result","fail");
//                validate_msg += "\t==> fail";
//                validate_msg += String.format("\n%s (%s) %s %s(%s)",
//                        check_value,
//                        check_value.getClass().getSimpleName(),
//                        comparator,
//                        expect_value,
//                        expect_value.getClass().getSimpleName()
//                );
//                logger.error(validate_msg);
//                failures.add(validate_msg);
            catch(InvocationTargetException | HrunBizException e){
                Throwable cause = e.getCause();
                if(cause instanceof AssertionError || e instanceof HrunBizException){
                    validate_pass = false;
                    validator_dict.put("check_result","fail");
                    validate_msg += "\t==> fail";
                    validate_msg += String.format("\n%s (%s) %s %s(%s)",
                            check_value,
                            Optional.ofNullable(check_value).map( cv -> cv.getClass().getSimpleName() ).orElse("null"),
                            comparator,
                            expect_value,
                            expect_value.getClass().getSimpleName()
                    );
                    logger.error(validate_msg);
                    failures.add(validate_msg);
                }else{
                    e.printStackTrace();
                    throw e;
                }
            }finally {
                ((List) this.validation_results.get("validate_extractor")).add(validator_dict);
                // 这里不需要对validator.get_args() 再执行操作了，因为validator_args就是validator.get_args()的引用
                // validator.get_args().addAll(validator_args);
            }
        }

        if(!validate_pass){
            String failures_string = "";
            for(String failure : failures){
                failures_string += String.format("\n%s",failure);
            }
            logger.error(failures_string);
            HrunExceptionFactory.create("E0028");
        }
    }
}
