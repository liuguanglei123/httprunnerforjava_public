package com.hrun.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.hrun.App;
import com.hubspot.jinjava.Jinjava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GenerateReport {

    static Logger logger = LoggerFactory.getLogger(GenerateReport.class);

    public static String gen_html_report(Summary summary,String report_template, String report_dir,String report_file){
        Long start_at_timestamp = (Long)summary.getTime().get("start_at");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String utc_time_iso_8601_str = df.format(new Date(start_at_timestamp));
        summary.getTime().put("start_datetime",utc_time_iso_8601_str);

        //TODO: if not summary["time"] or summary["stat"]["testcases"]["total"] == 0:
        Jinjava jinjava = new Jinjava();
        String renderedTemplate = "";
        try {
            String template = Resources.toString(Resources.getResource(report_template), Charsets.UTF_8);
            renderedTemplate = jinjava.render(template,JSONObject.parseObject(JSON.toJSONString(summary, SerializerFeature.WriteNullStringAsEmpty)));
        }catch(Exception e){
            e.printStackTrace();
        }

        String report_path;
        if(!Strings.isNullOrEmpty(report_file)){
            report_path = Paths.get(report_dir).resolve(report_file).toAbsolutePath().toString();
        }else{
            utc_time_iso_8601_str = utc_time_iso_8601_str.replace(":","").replace(" ","T");
            report_path = Paths.get(report_dir).resolve(utc_time_iso_8601_str).toAbsolutePath().toString() + ".html";
        }

        if(!(new File(report_dir).exists()))
            (new File(report_dir)).mkdir();

        try{
            //使用这个构造函数时，如果存在report_path文件，
            //则先把这个文件给删除掉，然后创建新的report_path
            FileWriter writer=new FileWriter(report_path);
            writer.write(renderedTemplate);
            writer.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        logger.info("Generated Html report: %s".format(report_path));

        String osName = System.getProperty("os.name", "");// 获取操作系统的名字
        try{
            if (osName.startsWith("Windows")) {// windows
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + report_path);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return report_path;
    }


}



