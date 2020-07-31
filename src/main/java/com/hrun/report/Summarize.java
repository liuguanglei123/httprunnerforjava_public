package com.hrun.report;

import java.util.HashMap;
import java.util.Map;

public class Summarize {

    public static Map<String,Object> get_summary(HtmlTestResult result){
        Map<String,Object> summary = new HashMap<>();
        summary.put("success",result.wasSuccessful());
        Map<String,Long> stat = new HashMap<>();
        stat.put("total",Long.valueOf(result.getTestsRun()));
        stat.put("failures",Long.valueOf(result.getRunNum().get("failedStepNum")));
        stat.put("errors",Long.valueOf(result.getRunNum().get("erroredStepNum")));
        stat.put("successes",Long.valueOf(result.getRunNum().get("successedStepNum")));
        stat.put("skipped",0L);
        summary.put("stat",stat);

        Map<String,Number> time = new HashMap<>();
        time.put("start_at",result.getStart_at());
        time.put("duration",result.getDuration());
        summary.put("time",time);

        summary.put("records",result.getRecords());

        return summary;
    }

    public static void aggregate_stat(Map<String,Object> origin_stat,Map<String,Long> new_stat){
        for(String key : new_stat.keySet()){
            if(!origin_stat.containsKey(key))
                origin_stat.put(key,new_stat.get(key));
            else if(key.equals("start_at"))
                origin_stat.put("start_at",Math.min((Long)origin_stat.get("start_at"),(Long)new_stat.get("start_at")));
            else if(key.equals("duration"))
                origin_stat.put("duration",(Long)origin_stat.get("duration")+(Long)new_stat.get("duration"));
            else
                origin_stat.put(key,(Long)origin_stat.get(key) + (Long)new_stat.get(key));
        }
    }
}
