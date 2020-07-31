package com.hrun.component.intf;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.Parse;
import com.hrun.component.common.Config;
import com.hrun.component.common.Extract;
import com.hrun.component.common.Validate;
import com.hrun.component.common.Variables;
import com.hrun.lazyContent.LazyString;

public interface RunableComponent extends Parseable {

    Variables getVariables();

    void setVariables(Variables variables);

    LazyString getBase_url();

    void setBase_url(LazyString base_url);

    Validate getValidate();

    void setValidate(Validate validate);

    Config getConfig();

    Extract getExtract();

    LazyString getName();
}
