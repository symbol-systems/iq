package systems.symbol.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by JO on 20/12/14.
 */
public class ClassHelper {
    static final Logger log = LoggerFactory.getLogger(ClassHelper.class);

    public static Object newInstance(String type)  {
        return newInstance(Thread.currentThread().getContextClassLoader(), type);
    }

    public static Object newInstance(ClassLoader cl, String type)  {
        if (type.startsWith("bean:")) type = type.substring(5);
//        if (!type.endsWith(".class")) type = type+".class";
        Class<?> clzz = null;
        try {
            clzz = Class.forName(type, true, cl);
            return clzz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            ClassHelper.log.error(e.getMessage(),e);
            return null;
        } catch (InstantiationException e) {
            ClassHelper.log.error(e.getMessage(),e);
            return null;
        } catch (IllegalAccessException e) {
            ClassHelper.log.error(e.getMessage(),e);
            return null;
        } catch (InvocationTargetException e) {
            ClassHelper.log.error(e.getMessage(),e);
            return null;
        } catch (NoSuchMethodException e) {
            ClassHelper.log.error(e.getMessage(),e);
            return null;
        }
    }
}
