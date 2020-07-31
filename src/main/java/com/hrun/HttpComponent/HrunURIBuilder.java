package com.hrun.HttpComponent;

import com.hrun.component.common.*;
import com.hrun.lazyContent.LazyContent;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Map;

public class HrunURIBuilder extends URIBuilder {

    public HrunURIBuilder(String string) throws URISyntaxException {
        super(string);
    }

    public void addParameter(Params params){
        if(params == null || params.isEmpty())
            return;
        for(Map.Entry<String, LazyContent> entry : params.getParams().entrySet()){
            addParameter(entry.getKey(),String.valueOf(entry.getValue().getEvalValue()));
        }
    }
}
