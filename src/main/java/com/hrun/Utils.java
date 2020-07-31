package com.hrun;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.hrun.component.ProjectMapping;
import com.hrun.exceptions.HrunExceptionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    static Pattern absolute_http_url_regexp_compile = Pattern.compile("^https\\://.*|^http\\://.*");

    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Method tmpMethod = null;

    public static File getFile(String path){
        File file = new File(path);
        if(!file.exists()){
            logger.error("path not exist:" + path);
            HrunExceptionFactory.create("E0004");
        }

        return file;
    }

    public static void dump_logs(Object object, ProjectMapping project_mapping,String tag_name){
        String json_file_abs_path = prepare_dump_json_file_abs_path(project_mapping, tag_name);
        String jsonOutput= JSON.toJSONString(object);
        dump_json_file(jsonOutput, json_file_abs_path);
    }

    public static String prepare_dump_json_file_abs_path(ProjectMapping project_mapping, String tag_name){
        String pwd_dir_path = project_mapping.getPWD();
        String test_path = project_mapping.getTest_path();

        if(StringUtils.isEmpty(test_path)){
            String dump_file_name = String.format("tests_mapping.%s.json",tag_name);
            String dumped_json_file_abs_path = (Paths.get(pwd_dir_path)).resolve("logs").resolve(dump_file_name).toString();
            return dumped_json_file_abs_path;
        }

        // both test_path and pwd_dir_path are absolute path
        String logs_dir_path = (Paths.get(pwd_dir_path)).resolve("storage").toAbsolutePath().toString();
        String test_path_relative_path = test_path.substring(pwd_dir_path.length()+1);
        String file_foder_path;
        String dump_file_name;
        if(new File(test_path).isDirectory()){
            file_foder_path = Paths.get(logs_dir_path).resolve(test_path_relative_path).toAbsolutePath().toString();
            dump_file_name = "all.%s.json".format(tag_name);
        }else{
            String file_relative_folder_path = Paths.get(test_path_relative_path).getParent().toString();
            String test_file = Paths.get(test_path_relative_path).getFileName().toString();
            file_foder_path = Paths.get(logs_dir_path).resolve(file_relative_folder_path).toAbsolutePath().toString();
            String test_file_name = test_file.split("\\.")[0];
            dump_file_name = test_file_name + "." + tag_name + ".json";
        }

        String dumped_json_file_abs_path = Paths.get(file_foder_path).resolve(dump_file_name).toAbsolutePath().toString();
        return dumped_json_file_abs_path;
    }

    public static void dump_json_file(String json_data, String json_file_abs_path){
        Path file_foder_path = Paths.get(json_file_abs_path).getParent();
        if(!(new File(file_foder_path.toString())).exists())
            (new File(file_foder_path.toString())).mkdir();

        prettyJson(json_data);
        try {
            // 防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw
            File writename = new File(json_file_abs_path);
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            out.write(prettyJson(json_data)); // \r\n即为换行
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String prettyJson(String json){
        if(StringUtils.isBlank(json)){
            return json;
        }

        try {
            JSONObject jsonObject = new JSONObject(true);
            jsonObject = JSONObject.parseObject(json, Feature.OrderedField);
            return JSONObject.toJSONString(jsonObject,true);
        }catch (ClassCastException e){
            try {
                Object jsonArray = new JSONArray();
                jsonArray = JSONArray.parse(json, Feature.OrderedField);
                return JSONArray.toJSONString(jsonArray,true);
            }catch (Exception f){
                logger.info("json内容美化失败！");
                return json;
            }
        }
    }

    public static <T extends Serializable> T deepcopy_dict(T data){
        //深复制，源码用的是python中自带的deepcopy通过递归实现的，这里通过字节流的复制实现的，百度到的方法
        /*deepcopy dict data, ignore file object (_io.BufferedReader)*/
        if(data == null)
            return data;

        return clone(data);
    }

    /**
     * 深复制一个实例，实例需要继承Serializable接口
     * @param data
     * @param <T>
     * @return
     */
    public static <T extends Serializable> T deepcopy_obj(T data){
        //深复制，源码用的是python中自带的deepcopy通过递归实现的，这里通过字节流的复制实现的，百度到的方法
        /*deepcopy dict data, ignore file object (_io.BufferedReader)*/
        if(data == null)
            return data;

        return clone(data);
    }

    public static <T extends Serializable> T clone(T obj) {
        T clonedObj = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            clonedObj = (T) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clonedObj;
    }

    public static String build_url(String base_url,String path){
        if(absolute_http_url_regexp_compile.matcher(path).matches()){
            return path;
        }
        else if(base_url != null && !base_url.equals("")){
            return String.format("%s/%s",base_url,path);
        }
        else{
            HrunExceptionFactory.create("E0018");
            return null;
        }
    }

    public static Object query_json(JSONObject json_content, String query){
        return query_json(json_content,query,"\\.");
    }

    public static Object query_json(Object json_content, String query, String delimiter){
        Boolean raise_flag = false;
        String response_body = String.format("response body: {}\n",json_content);

        String[] keys = query.split(delimiter);
        try{
            for(String key : keys){
                if(json_content instanceof JSONArray){
                    json_content = ((JSONArray) json_content).get(Integer.valueOf(key));
                }else if(json_content instanceof JSONObject){
                    json_content = ((JSONObject) json_content).get(key);
                }//TODO: else if(json_content instanceof String){}
                else{
                    logger.error(String.format("invalid type value: %s %s ",json_content,json_content.getClass()));
                }
            }
        }catch(Exception e){
            raise_flag = true;
        }
        if(raise_flag){
            String err_msg = String.format("Failed to extract! => %s",query);
            err_msg += response_body;
            logger.error(err_msg);
            HrunExceptionFactory.create("E0026");
        }

        return json_content;
    }

    public static String getErrorInfoFromException(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String result =  "\r\n" + sw.toString() + "\r\n";
            sw.close();
            pw.close();
            return result;
        } catch (Exception e2) {
            return "ErrorInfoFromException";
        }
    }

    public static Boolean checkIsList(Object obj){
        if(obj instanceof List)
            return true;
        else
            return false;
    }

    public static String unicodeToCn(String unicode) {
        /** 以 \ u 分割，因为java注释也能识别unicode，因此中间加了一个空格*/
        String[] strs = unicode.split("\\\\u");
        String returnStr = "";
        // 由于unicode字符串以 \ u 开头，因此分割出的第一个字符是""。
        for (int i = 1; i < strs.length; i++) {
            returnStr += (char) Integer.valueOf(strs[i], 16).intValue();
        }
        return returnStr;
    }

    public static String convertStreamToString(InputStream is) {
        StringBuilder sb1 = new StringBuilder();
        byte[] bytes = new byte[4096];
        int size = 0;

        try {
            while ((size = is.read(bytes)) > 0) {
                String str = new String(bytes, 0, size, "UTF-8");
                sb1.append(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb1.toString();
    }

    public static String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
    }

    //Unicode转中文
    public static String decodeUnicode(String unicode) {
        StringBuffer string = new StringBuffer();

//        String[] hex =unicode.replace("\\\\\\\\u","\\\\u").replace("\\\\\"","\"").split("\\\\u");
        String[] hex =unicode.split("\\\\u");

        for (int i = 0; i < hex.length; i++) {

            try {
                // 汉字范围 \u4e00-\u9fa5 (中文)
                if(hex[i].length()>=4){//取前四个，判断是否是汉字
                    String chinese = hex[i].substring(0, 4);
                    try {
                        int chr = Integer.parseInt(chinese, 16);
                        boolean isChinese = isChinese((char) chr);
                        //转化成功，判断是否在  汉字范围内
                        if (isChinese){//在汉字范围内
                            // 追加成string
                            string.append((char) chr);
                            //并且追加  后面的字符
                            String behindString = hex[i].substring(4);
                            string.append(behindString);
                        }else {
                            string.append(hex[i]);
                        }
                    } catch (NumberFormatException e1) {
                        string.append(hex[i]);
                    }

                }else{
                    string.append(hex[i]);
                }
            } catch (NumberFormatException e) {
                string.append(hex[i]);
            }
        }

        return string.toString();
    }

    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }
}
