package systems.symbol.rdf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * systems.symbol (c) 2014-2015,2020-2021
 * Module: systems.symbol.vendor.spring
 * @author Symbol Systems
 * Date  : 4/07/2014
 * Time  : 2:24 AM
 */
public class CachedBeanFactory extends DefaultListableBeanFactory {
	final Logger log = LoggerFactory.getLogger(this.getClass());

	Map beans = new HashMap();

	public CachedBeanFactory() {
	}

	public void bind(String name, Object bean) {
		Object old = beans.put(name, bean);
		log.debug("Bound: "+name+" => "+bean+" was: "+old);
	}

	@Override
	public boolean containsBeanDefinition(String name) {
		return (beans.containsKey(name)) || super.containsBeanDefinition(name);
	}

	@Override
	protected <T> T doGetBean( final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly) throws BeansException {
		log.debug("Beans: "+beans);
		log.debug("getBean: "+name+" -> "+beans.containsKey(name));
		Object bean = beans.get(name);
		if (bean!=null && (requiredType==null || requiredType.isInstance(bean)) ) return (T)bean;
		return super.doGetBean(name, requiredType, args, typeCheckOnly);
	}


	public Map getBeanConfig() {
		return beans;
	}
}
