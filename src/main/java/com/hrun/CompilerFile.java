package com.hrun;

import com.hrun.builtin.Comparator;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.util.List;

public class CompilerFile {

    private static JavaCompiler compiler;

    static{
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    /**
     * 编译java文件
     * @param ops 编译参数
     * @param files 需要编译的文件
     */
    private static void javac(List<String> ops, String... files){
        StandardJavaFileManager manager = null;
        try{
            manager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> it = manager.getJavaFileObjects(files);
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, ops, null, it);
            task.call();
            /* if(logger.isDebugEnabled()){
                for(String file:files)
                    logger.debug("Compile Java File:" + file);
            } */
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            if(manager!=null){
                try {
                    manager.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class FileClassLoader extends ClassLoader {
        private String rootDir;

        public FileClassLoader(String rootDir) {
            this.rootDir = rootDir;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // 获取类的class文件字节数组
            byte[] classData = getClassData(name);
            if (classData == null) {
                throw new ClassNotFoundException();
            } else {
                //直接生成class对象
                return defineClass(name, classData, 0, classData.length);
            }
        }

        private byte[] getClassData(String className) {
            // 读取类文件的字节
            String path = classNameToPath(className);
            try {
                InputStream ins = new FileInputStream(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesNumRead = 0;
                // 读取类文件的字节码
                while ((bytesNumRead = ins.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesNumRead);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String classNameToPath(String className) {
            return rootDir + File.separatorChar
                    + className.replace('.', File.separatorChar) + ".class";
        }

    }
    /**
     * 加载类
     * @param name 类名
     * @return
     */
    public static Class<?> load(String file_path, String name) throws ClassNotFoundException {
        FileClassLoader loader = new FileClassLoader(file_path);
        Class<?> cls=loader.loadClass(name);

        return cls;
        /* Class<?> cls = null;
        ClassLoader classLoader = null;
        try{
            classLoader = CompilerFile.class.getClassLoader();
            cls = classLoader.loadClass(name);
            if(logger.isDebugEnabled()){
                logger.debug("Load Class["+name+"] by "+classLoader);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return cls;*/
    }

    /**
     * 编译代码并加载类
     * @param filePath java代码路径
    //* @param source java代码
     * @param clsName 类名
     * @param ops 编译参数
     * @return
     */
    public static Class<?> loadClass(String filePath,String clsName,List<String> ops,String project_working_directory){
        try {
            javac(ops,filePath);
            return load(project_working_directory,clsName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class<?> loadClass(String module){
        //TODO： hrun这里获取了builtin模块下的两个文件内容，这里只获取了Comparator类的方法
        try{
            return Comparator.class;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 调用类方法
     * @param cls 类
     * @param methodName 方法名
     * @param paramsCls 方法参数类型
     * @param params 方法参数
     * @return
     */
    public static Object invoke(Class<?> cls,String methodName,Class<?>[] paramsCls,Object[] params){
        Object result = null;
        try {
            Method method = cls.getDeclaredMethod(methodName, paramsCls);
            Object obj = cls.newInstance();
            result = method.invoke(obj, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
